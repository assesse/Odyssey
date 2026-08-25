package com.halmeoni.transit.data.provider

import com.halmeoni.transit.domain.model.RealtimeArrival
import com.halmeoni.transit.domain.model.RealtimeStatus
import com.halmeoni.transit.domain.model.RouteStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilderFactory

class SeoulBusProvider(
    private val okHttpClient: OkHttpClient = OkHttpClient()
) : RealtimeBusProvider {

    override suspend fun getBusArrival(step: RouteStep, apiKey: String): RealtimeStatus = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext RealtimeStatus.AuthenticationRequired("공공데이터 버스 API 키가 필요합니다.")
        }

        val busRouteId = step.busLocalRouteId ?: step.busId?.toString() ?: ""
        val targetBusNo = step.routeName?.trim() ?: ""
        val targetArsId = step.startArsId?.trim() ?: ""

        if (busRouteId.isBlank() && targetArsId.isBlank()) {
            return@withContext RealtimeStatus.Unsupported("정류소 또는 노선 식별 정보가 부족합니다.")
        }

        try {
            val encodedKey = if (apiKey.contains("%")) apiKey else URLEncoder.encode(apiKey, "UTF-8")
            
            val url = if (busRouteId.isNotBlank()) {
                "http://ws.bus.go.kr/api/rest/arrive/getArrInfoByRouteAll?serviceKey=$encodedKey&busRouteId=$busRouteId"
            } else {
                "http://ws.bus.go.kr/api/rest/arrive/getLowArrInfoByStId?serviceKey=$encodedKey&stId=${step.startLocalStationId ?: ""}"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/xml")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext RealtimeStatus.NetworkError("통신 오류 (코드: ${response.code()})")
            }

            val xmlBody = response.body()?.string() ?: ""
            parseSeoulBusXml(xmlBody, targetBusNo, targetArsId, step.startName)
        } catch (e: Exception) {
            RealtimeStatus.NetworkError(e.message ?: "네트워크 연결 오류")
        }
    }

    fun parseSeoulBusXml(
        xml: String,
        targetBusNo: String,
        targetArsId: String,
        targetStationName: String
    ): RealtimeStatus {
        if (xml.isBlank()) return RealtimeStatus.NoData("응답 데이터가 없습니다.")

        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(xml)))
            doc.documentElement.normalize()

            val headerCd = getTagValue(doc.documentElement, "headerCd")
            val headerMsg = getTagValue(doc.documentElement, "headerMsg")

            if (headerCd.isNotEmpty() && headerCd != "0") {
                return if (headerCd == "4" || headerCd == "30" || headerMsg.contains("KEY") || headerMsg.contains("SERVICE_KEY")) {
                    RealtimeStatus.AuthenticationRequired("인증키가 유효하지 않습니다 ($headerMsg)")
                } else {
                    RealtimeStatus.NoData("도착 정보 없음 ($headerMsg)")
                }
            }

            val items = doc.getElementsByTagName("itemList")
            if (items.length == 0) {
                return RealtimeStatus.NoData("현재 운행 중인 버스 정보가 없습니다.")
            }

            var matchedElement: Element? = null

            for (i in 0 until items.length) {
                val elem = items.item(i) as? Element ?: continue
                val arsId = getTagValue(elem, "arsId")
                val stNm = getTagValue(elem, "stNm")
                val rtNm = getTagValue(elem, "rtNm")

                val arsMatch = targetArsId.isNotBlank() && (arsId == targetArsId || arsId.endsWith(targetArsId))
                val stationMatch = targetStationName.isNotBlank() && (stNm.contains(targetStationName) || targetStationName.contains(stNm))
                val routeMatch = targetBusNo.isBlank() || rtNm.equals(targetBusNo, ignoreCase = true) || rtNm.contains(targetBusNo)

                if ((arsMatch || stationMatch) && routeMatch) {
                    matchedElement = elem
                    break
                }
            }

            if (matchedElement == null) {
                return RealtimeStatus.NoData("현재 운행 중인 버스 정보가 없습니다.")
            }

            val arrmsg1 = getTagValue(matchedElement, "arrmsg1")
            val arrmsg2 = getTagValue(matchedElement, "arrmsg2")
            val traTime1 = getTagValue(matchedElement, "traTime1")
            val traTime2 = getTagValue(matchedElement, "traTime2")

            if (arrmsg1.isBlank() || arrmsg1.contains("운행종료") || arrmsg1.contains("출발대기") || arrmsg1.contains("미운행")) {
                return RealtimeStatus.NoData(if (arrmsg1.isNotBlank()) arrmsg1 else "현재 운행 중인 버스 정보가 없습니다.")
            }

            val firstMinutes = parseMinutes(arrmsg1, traTime1)
            val firstStops = parseRemainingStops(arrmsg1)
            val cleanFirstMsg = formatBusArrivalMessage(arrmsg1, firstMinutes, firstStops)

            val secondMinutes = parseMinutes(arrmsg2, traTime2)
            val secondStops = parseRemainingStops(arrmsg2)
            val cleanSecondMsg = if (arrmsg2.isNotBlank() && !arrmsg2.contains("운행종료") && !arrmsg2.contains("대기")) {
                formatBusArrivalMessage(arrmsg2, secondMinutes, secondStops)
            } else {
                null
            }

            return RealtimeStatus.Available(
                RealtimeArrival.Bus(
                    firstArrivalMinutes = firstMinutes,
                    firstRemainingStops = firstStops,
                    firstMessage = cleanFirstMsg,
                    secondArrivalMinutes = secondMinutes,
                    secondRemainingStops = secondStops,
                    secondMessage = cleanSecondMsg?.let { "다음 버스: $it" }
                )
            )
        } catch (e: Exception) {
            return RealtimeStatus.NetworkError("데이터 해석 오류: ${e.message}")
        }
    }

    private fun getTagValue(elem: Element, tag: String): String {
        val list = elem.getElementsByTagName(tag)
        if (list.length > 0) {
            val node = list.item(0)
            return node?.textContent?.trim() ?: ""
        }
        return ""
    }

    private fun parseMinutes(arrmsg: String, traTimeSec: String): Int? {
        val sec = traTimeSec.toIntOrNull()
        if (sec != null && sec > 0) {
            return (sec + 59) / 60
        }
        val match = Regex("""(\d+)\s*분""").find(arrmsg)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun parseRemainingStops(arrmsg: String): Int? {
        val match = Regex("""\[(\d+)번째\s*전\]""").find(arrmsg)
            ?: Regex("""\[(\d+)번째\]""").find(arrmsg)
            ?: Regex("""(\d+)개\s*전""").find(arrmsg)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun formatBusArrivalMessage(arrmsg: String, minutes: Int?, stops: Int?): String {
        if (arrmsg.contains("곧") || arrmsg.contains("진입")) {
            return if (stops != null && stops > 0) "곧 도착 (${stops}정거장 전)" else "곧 도착"
        }
        if (minutes != null && minutes > 0) {
            return if (stops != null && stops > 0) "약 ${minutes}분 후 도착 (${stops}정거장 전)" else "약 ${minutes}분 후 도착"
        }
        return arrmsg.replace("[", " (").replace("]", ")")
    }
}
