package com.halmeoni.transit.data.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

data class OdsayResponse(
    @SerializedName("result") val result: OdsayResult? = null,
    @SerializedName("error") val error: List<OdsayErrorItem>? = null
)

data class OdsayErrorItem(
    @SerializedName("code") val code: String? = null,
    @SerializedName("msg") val msg: String? = null,
    @SerializedName("message") val message: String? = null
) {
    val displayMessage: String?
        get() = msg ?: message
}

class OdsayResponseDeserializer : JsonDeserializer<OdsayResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): OdsayResponse {
        if (!json.isJsonObject) {
            return OdsayResponse()
        }
        val jsonObject = json.asJsonObject

        val result = if (jsonObject.has("result") && !jsonObject.get("result").isJsonNull) {
            context.deserialize<OdsayResult>(jsonObject.get("result"), OdsayResult::class.java)
        } else {
            null
        }

        val errors = mutableListOf<OdsayErrorItem>()
        if (jsonObject.has("error") && !jsonObject.get("error").isJsonNull) {
            val errorElement = jsonObject.get("error")
            if (errorElement.isJsonArray) {
                for (item in errorElement.asJsonArray) {
                    if (item.isJsonObject) {
                        errors.add(context.deserialize(item, OdsayErrorItem::class.java))
                    }
                }
            } else if (errorElement.isJsonObject) {
                errors.add(context.deserialize(errorElement, OdsayErrorItem::class.java))
            }
        }

        return OdsayResponse(
            result = result,
            error = if (errors.isEmpty()) null else errors
        )
    }
}

data class OdsayResult(
    @SerializedName("searchType") val searchType: Int? = null,
    @SerializedName("outTrafficCheck") val outTrafficCheck: Int? = null,
    @SerializedName("busCount") val busCount: Int? = null,
    @SerializedName("subwayCount") val subwayCount: Int? = null,
    @SerializedName("subwayBusCount") val subwayBusCount: Int? = null,
    @SerializedName("pointDistance") val pointDistance: Double? = null,
    @SerializedName("startRadius") val startRadius: Int? = null,
    @SerializedName("endRadius") val endRadius: Int? = null,
    @SerializedName("path") val path: List<OdsayPath>? = null
)

data class OdsayPath(
    @SerializedName("pathType") val pathType: Int? = null, // 1: 지하철, 2: 버스, 3: 버스+지하철
    @SerializedName("info") val info: OdsayPathInfo? = null,
    @SerializedName("subPath") val subPath: List<OdsaySubPath>? = null
)

data class OdsayPathInfo(
    @SerializedName("trafficDistance") val trafficDistance: Double? = null,
    @SerializedName("totalWalk") val totalWalk: Int? = null,
    @SerializedName("totalTime") val totalTime: Int? = null,
    @SerializedName("payment") val payment: Int? = null,
    @SerializedName("busTransitCount") val busTransitCount: Int? = null,
    @SerializedName("subwayTransitCount") val subwayTransitCount: Int? = null,
    @SerializedName("mapObj") val mapObj: String? = null,
    @SerializedName("firstStartStation") val firstStartStation: String? = null,
    @SerializedName("lastEndStation") val lastEndStation: String? = null,
    @SerializedName("totalStationCount") val totalStationCount: Int? = null,
    @SerializedName("totalDistance") val totalDistance: Double? = null
)

data class OdsaySubPath(
    @SerializedName("trafficType") val trafficType: Int? = null, // 1: 지하철, 2: 버스, 3: 도보
    @SerializedName("distance") val distance: Double? = null,
    @SerializedName("sectionTime") val sectionTime: Int? = null,
    @SerializedName("stationCount") val stationCount: Int? = null,
    @SerializedName("startName") val startName: String? = null,
    @SerializedName("startX") val startX: Double? = null,
    @SerializedName("startY") val startY: Double? = null,
    @SerializedName("startID") val startID: Int? = null,
    @SerializedName("startStationCityCode") val startStationCityCode: Int? = null,
    @SerializedName("startStationProviderCode") val startStationProviderCode: Int? = null,
    @SerializedName("startLocalStationID") val startLocalStationID: String? = null,
    @SerializedName("startArsID") val startArsID: String? = null,
    @SerializedName("endName") val endName: String? = null,
    @SerializedName("endX") val endX: Double? = null,
    @SerializedName("endY") val endY: Double? = null,
    @SerializedName("endID") val endID: Int? = null,
    @SerializedName("endStationCityCode") val endStationCityCode: Int? = null,
    @SerializedName("endStationProviderCode") val endStationProviderCode: Int? = null,
    @SerializedName("endLocalStationID") val endLocalStationID: String? = null,
    @SerializedName("endArsID") val endArsID: String? = null,
    @SerializedName("way") val way: String? = null,
    @SerializedName("wayCode") val wayCode: Int? = null,
    @SerializedName("lane") val lane: List<OdsayLane>? = null,
    @SerializedName("passStopList") val passStopList: OdsayPassStopList? = null
)

data class OdsayLane(
    @SerializedName("name") val name: String? = null,
    @SerializedName("busNo") val busNo: String? = null,
    @SerializedName("busID") val busID: Int? = null,
    @SerializedName("busLocalBlID") val busLocalBlID: String? = null,
    @SerializedName("busCityCode") val busCityCode: Int? = null,
    @SerializedName("busProviderCode") val busProviderCode: Int? = null,
    @SerializedName("subwayCode") val subwayCode: Int? = null,
    @SerializedName("subwayCityCode") val subwayCityCode: Int? = null,
    @SerializedName("type") val type: Int? = null
)

data class OdsayPassStopList(
    @SerializedName("stations") val stations: List<OdsayStation>? = null
)

data class OdsayStation(
    @SerializedName("index") val index: Int? = null,
    @SerializedName("stationID") val stationID: Int? = null,
    @SerializedName("stationName") val stationName: String? = null,
    @SerializedName("stationCityCode") val stationCityCode: Int? = null,
    @SerializedName("stationProviderCode") val stationProviderCode: Int? = null,
    @SerializedName("localStationID") val localStationID: String? = null,
    @SerializedName("arsID") val arsID: String? = null,
    @SerializedName("x") val x: String? = null,
    @SerializedName("y") val y: String? = null
)
