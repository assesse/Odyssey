package com.halmeoni.transit.data.provider

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.halmeoni.transit.domain.model.RealtimeArrival
import com.halmeoni.transit.domain.model.RealtimeStatus
import com.halmeoni.transit.domain.model.RouteStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

class SeoulSubwayProvider(
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson()
) : RealtimeSubwayProvider {

    override suspend fun getSubwayArrival(step: RouteStep, apiKey: String): RealtimeStatus = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext RealtimeStatus.AuthenticationRequired("서울 지하철 API 키를 등록해 주세요.")
        }

        val rawStationName = step.startName.trim()
        if (rawStationName.isBlank()) {
            return@withContext RealtimeStatus.Unsupported("지하철 역명 정보가 없습니다.")
        }

        val cleanStationName = normalizeStationName(rawStationName)
        val targetSubwayId = mapOdsaySubwayCodeToSeoulId(step.subwayCode, step.routeName)

        try {
            val encodedKey = if (apiKey.contains("%")) apiKey else URLEncoder.encode(apiKey, "UTF-8")
            val encodedStation = URLEncoder.encode(cleanStationName, "UTF-8")
            // Use http or https seamlessly
            val url = "http://swopenapi.seoul.go.kr/api/subway/$encodedKey/json/realtimeStationArrival/0/16/$encodedStation"

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext RealtimeStatus.NetworkError("실시간 지하철 정보를 가져오지 못했어요.")
            }

            val jsonBody = response.body()?.string() ?: ""
            parseSubwayArrivalJson(jsonBody, targetSubwayId, step)
        } catch (e: Exception) {
            RealtimeStatus.NetworkError("실시간 지하철 정보를 가져오지 못했어요.")
        }
    }

    fun parseSubwayArrivalJson(
        json: String,
        targetSubwayId: String?,
        step: RouteStep
    ): RealtimeStatus {
        if (json.isBlank()) return RealtimeStatus.NoData("지하철 도착 정보가 없습니다.")

        try {
            val subwayResponse = gson.fromJson(json, SeoulSubwayResponse::class.java)
            if (subwayResponse?.errorMessage != null) {
                val code = subwayResponse.errorMessage.code ?: ""
                val msg = subwayResponse.errorMessage.message ?: ""
                if (code == "INFO-200" || code.contains("200")) {
                    return RealtimeStatus.NoData("해당 역의 실시간 도착 정보가 없습니다.")
                }
                if (code.startsWith("ERROR") || code == "INFO-100") {
                    return RealtimeStatus.AuthenticationRequired("지하철 인증키를 확인해 주세요.")
                }
            }

            val arrivals = subwayResponse?.realtimeArrivalList ?: return RealtimeStatus.NoData("도착 예정 열차가 없습니다.")
            if (arrivals.isEmpty()) return RealtimeStatus.NoData("도착 예정 열차가 없습니다.")

            // 1. Filter by Line ID (SubwayId)
            val lineFiltered = if (!targetSubwayId.isNullOrBlank()) {
                arrivals.filter { it.subwayId == targetSubwayId }
            } else {
                arrivals
            }

            if (lineFiltered.isEmpty()) {
                return RealtimeStatus.NoData("해당 노선의 도착 예정 열차가 없습니다.")
            }

            // 2. Filter by Direction
            val nextStation = step.passStops.getOrNull(1)?.replace("역", "")?.trim() ?: ""
            val endStation = step.endName.replace("역", "")?.trim() ?: ""
            val wayCode = step.subwayWayCode // 1: 상행, 2: 하행

            val matchedArrival = lineFiltered.firstOrNull { item ->
                val trainLine = item.trainLineNm ?: ""
                val bstatn = item.bstatnNm ?: ""
                val updn = item.updnLine ?: ""

                val nextMatch = nextStation.isNotBlank() && (trainLine.contains(nextStation) || bstatn.contains(nextStation))
                val endMatch = endStation.isNotBlank() && (trainLine.contains(endStation) || bstatn.contains(endStation))
                val wayMatch = when (wayCode) {
                    1 -> updn.contains("상행") || updn.contains("내선")
                    2 -> updn.contains("하행") || updn.contains("외선")
                    else -> false
                }

                nextMatch || endMatch || wayMatch
            } ?: lineFiltered.firstOrNull()

            if (matchedArrival == null) {
                return RealtimeStatus.NoData("해당 방향 열차 정보가 없습니다.")
            }

            // 3. Parse ETA and format messages
            val arrivalSec = matchedArrival.barvlDt?.toIntOrNull()
            val arrivalMinutes = if (arrivalSec != null && arrivalSec > 0) {
                (arrivalSec + 59) / 60
            } else {
                parseSubwayMinutesFromMsg(matchedArrival.arvlMsg2)
            }

            val rawMsg = matchedArrival.arvlMsg2 ?: ""
            val cleanArrivalMsg = formatSubwayArrivalMessage(rawMsg, arrivalMinutes)
            val destName = if (!matchedArrival.bstatnNm.isNullOrBlank()) "${matchedArrival.bstatnNm}행" else ""
            val directionDesc = if (!matchedArrival.trainLineNm.isNullOrBlank()) matchedArrival.trainLineNm else ""
            val currentPos = if (!matchedArrival.arvlMsg3.isNullOrBlank()) "${matchedArrival.arvlMsg3} 위치" else ""

            // 4. Freshness check
            val isStale = isTimestampStale(matchedArrival.recptnDt)

            val arrivalObj = RealtimeArrival.Subway(
                arrivalMinutes = arrivalMinutes,
                arrivalMessage = cleanArrivalMsg,
                destinationName = destName,
                nextStationDirection = directionDesc,
                currentPositionMsg = currentPos,
                isStale = isStale
            )

            return if (isStale) RealtimeStatus.Stale(arrivalObj) else RealtimeStatus.Available(arrivalObj)
        } catch (e: Exception) {
            return RealtimeStatus.NetworkError("실시간 지하철 정보를 가져오지 못했어요.")
        }
    }

    fun normalizeStationName(name: String): String {
        var clean = name.trim()
        if (clean.endsWith("역") && clean != "서울역") {
            clean = clean.substring(0, clean.length - 1)
        }
        if (clean.contains("(")) {
            val withoutBracket = clean.substringBefore("(").trim()
            if (withoutBracket.isNotBlank()) clean = withoutBracket
        }
        return clean
    }

    fun mapOdsaySubwayCodeToSeoulId(subwayCode: Int?, routeName: String?): String? {
        return when (subwayCode) {
            1 -> "1001"
            2 -> "1002"
            3 -> "1003"
            4 -> "1004"
            5 -> "1005"
            6 -> "1006"
            7 -> "1007"
            8 -> "1008"
            9 -> "1009"
            101 -> "1065" // 공항철도
            102 -> "1077" // 신분당선
            104 -> "1063" // 경의중앙선
            108 -> "1067" // 경춘선
            109 -> "1075" // 수인분당선
            116 -> "1075" // 수인분당선
            117 -> "1079" // 신림선
            else -> {
                val name = routeName ?: ""
                when {
                    name.contains("1호선") -> "1001"
                    name.contains("2호선") -> "1002"
                    name.contains("3호선") -> "1003"
                    name.contains("4호선") -> "1004"
                    name.contains("5호선") -> "1005"
                    name.contains("6호선") -> "1006"
                    name.contains("7호선") -> "1007"
                    name.contains("8호선") -> "1008"
                    name.contains("9호선") -> "1009"
                    name.contains("공항철도") -> "1065"
                    name.contains("신분당") -> "1077"
                    name.contains("경의중앙") -> "1063"
                    name.contains("수인분당") -> "1075"
                    else -> null
                }
            }
        }
    }

    private fun parseSubwayMinutesFromMsg(msg: String?): Int? {
        if (msg == null) return null
        val match = Regex("""(\d+)\s*분""").find(msg)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun formatSubwayArrivalMessage(rawMsg: String, minutes: Int?): String {
        if (rawMsg.contains("전역") || rawMsg.contains("진입") || rawMsg.contains("도착")) {
            return rawMsg.replace("[", " (").replace("]", ")")
        }
        if (minutes != null && minutes > 0) {
            return "약 ${minutes}분 후 도착"
        }
        return rawMsg
    }

    private fun isTimestampStale(recptnDt: String?): Boolean {
        if (recptnDt.isNullOrBlank()) return false
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
            val date = sdf.parse(recptnDt)
            if (date != null) {
                val diffMs = System.currentTimeMillis() - date.time
                diffMs > 5 * 60 * 1000L
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}

data class SeoulSubwayResponse(
    @SerializedName("errorMessage") val errorMessage: SeoulSubwayError?,
    @SerializedName("realtimeArrivalList") val realtimeArrivalList: List<SeoulSubwayArrivalItem>?
)

data class SeoulSubwayError(
    @SerializedName("status") val status: Int?,
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("total") val total: Int?
)

data class SeoulSubwayArrivalItem(
    @SerializedName("subwayId") val subwayId: String?,
    @SerializedName("updnLine") val updnLine: String?,
    @SerializedName("trainLineNm") val trainLineNm: String?,
    @SerializedName("statnNm") val statnNm: String?,
    @SerializedName("btrainSttus") val btrainSttus: String?,
    @SerializedName("barvlDt") val barvlDt: String?,
    @SerializedName("bstatnNm") val bstatnNm: String?,
    @SerializedName("arvlMsg2") val arvlMsg2: String?,
    @SerializedName("arvlMsg3") val arvlMsg3: String?,
    @SerializedName("arvlCd") val arvlCd: String?,
    @SerializedName("recptnDt") val recptnDt: String?
)
