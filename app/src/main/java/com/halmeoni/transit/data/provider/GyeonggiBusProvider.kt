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

class GyeonggiBusProvider(
    private val okHttpClient: OkHttpClient = OkHttpClient()
) : RealtimeBusProvider {

    override suspend fun getBusArrival(step: RouteStep, apiKey: String): RealtimeStatus = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext RealtimeStatus.AuthenticationRequired("공공데이터 버스 API 키가 필요합니다.")
        }

        val stationId = step.startLocalStationId?.trim() ?: ""
        val targetBusNo = step.routeName?.trim() ?: ""
        val targetRouteId = step.busLocalRouteId?.trim() ?: ""

        if (stationId.isBlank()) {
            return@withContext RealtimeStatus.Unsupported("경기도 정류소 고유번호가 없습니다.")
        }

        try {
            val encodedKey = if (apiKey.contains("%")) apiKey else URLEncoder.encode(apiKey, "UTF-8")
            val url = "http://apis.data.go.kr/6410000/busarrivalservice/getBusArrivalList?serviceKey=$encodedKey&stationId=$stationId"

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/xml")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext RealtimeStatus.NetworkError("통신 오류 (코드: ${response.code()})")
            }

            val xmlBody = response.body()?.string() ?: ""
            parseGyeonggiBusXml(xmlBody, targetBusNo, targetRouteId)
        } catch (e: Exception) {
            RealtimeStatus.NetworkError(e.message ?: "네트워크 연결 오류")
        }
    }

    fun parseGyeonggiBusXml(
        xml: String,
        targetBusNo: String,
        targetRouteId: String
    ): RealtimeStatus {
        if (xml.isBlank()) return RealtimeStatus.NoData("응답 데이터가 없습니다.")

        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(xml)))
            doc.documentElement.normalize()

            val resultCode = getTagValue(doc.documentElement, "resultCode")
            val resultMsg = getTagValue(doc.documentElement, "resultMessage").ifBlank { getTagValue(doc.documentElement, "resultMsg") }

            if (resultCode.isNotEmpty() && resultCode != "0") {
                return if (resultCode == "30" || resultCode == "04" || resultMsg.contains("KEY") || resultMsg.contains("SERVICE_KEY")) {
                    RealtimeStatus.AuthenticationRequired("인증키가 유효하지 않습니다 ($resultMsg)")
                } else {
                    RealtimeStatus.NoData("도착 정보 없음 ($resultMsg)")
                }
            }

            val items = doc.getElementsByTagName("busArrivalList")
            if (items.length == 0) {
                return RealtimeStatus.NoData("현재 운행 중인 버스 정보가 없습니다.")
            }

            var matchedElement: Element? = null

            for (i in 0 until items.length) {
                val elem = items.item(i) as? Element ?: continue
                val routeId = getTagValue(elem, "routeId")
                val routeName = getTagValue(elem, "routeName")

                val routeIdMatch = targetRouteId.isNotBlank() && routeId == targetRouteId
                val routeNameMatch = targetBusNo.isNotBlank() && (routeName.equals(targetBusNo, ignoreCase = true) || routeName.contains(targetBusNo))

                if (routeIdMatch || routeNameMatch) {
                    matchedElement = elem
                    break
                }
            }

            if (matchedElement == null) {
                return RealtimeStatus.NoData("현재 운행 중인 버스 정보가 없습니다.")
            }

            val predictTime1 = getTagValue(matchedElement, "predictTime1")
            val locationNo1 = getTagValue(matchedElement, "locationNo1")
            val predictTime2 = getTagValue(matchedElement, "predictTime2")
            val locationNo2 = getTagValue(matchedElement, "locationNo2")

            if (predictTime1.isBlank()) {
                return RealtimeStatus.NoData("현재 운행 중인 버스 정보가 없습니다.")
            }

            val min1 = predictTime1.toIntOrNull()
            val stops1 = locationNo1.toIntOrNull()
            val min2 = predictTime2.toIntOrNull()
            val stops2 = locationNo2.toIntOrNull()

            val msg1 = formatGyeonggiBusMessage(min1, stops1)
            val msg2 = if (min2 != null && min2 > 0) {
                "다음 버스: " + formatGyeonggiBusMessage(min2, stops2)
            } else {
                null
            }

            return RealtimeStatus.Available(
                RealtimeArrival.Bus(
                    firstArrivalMinutes = min1,
                    firstRemainingStops = stops1,
                    firstMessage = msg1,
                    secondArrivalMinutes = min2,
                    secondRemainingStops = stops2,
                    secondMessage = msg2
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

    private fun formatGyeonggiBusMessage(minutes: Int?, stops: Int?): String {
        if (minutes == null || minutes <= 0) {
            return if (stops != null && stops > 0) "곧 도착 (${stops}정거장 전)" else "곧 도착"
        }
        return if (stops != null && stops > 0) {
            "약 ${minutes}분 후 도착 (${stops}정거장 전)"
        } else {
            "약 ${minutes}분 후 도착"
        }
    }
}
