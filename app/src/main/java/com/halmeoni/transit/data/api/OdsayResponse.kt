package com.halmeoni.transit.data.api

import com.google.gson.annotations.SerializedName

data class OdsayResponse(
    @SerializedName("result") val result: OdsayResult? = null
)

data class OdsayResult(
    @SerializedName("searchType") val searchType: Int? = null,
    @SerializedName("outTrafficDay") val outTrafficDay: Int? = null,
    @SerializedName("path") val path: List<OdsayPath>? = null
)

data class OdsayPath(
    @SerializedName("pathType") val pathType: Int? = null, // 1: 지하철, 2: 버스, 3: 버스+지하철
    @SerializedName("info") val info: OdsayPathInfo? = null,
    @SerializedName("subPath") val subPath: List<OdsaySubPath>? = null
)

data class OdsayPathInfo(
    @SerializedName("trafficDistance") val trafficDistance: Int? = null,
    @SerializedName("totalWalk") val totalWalk: Int? = null,
    @SerializedName("totalTime") val totalTime: Int? = null,
    @SerializedName("payment") val payment: Int? = null,
    @SerializedName("busTransitCount") val busTransitCount: Int? = null,
    @SerializedName("subwayTransitCount") val subwayTransitCount: Int? = null,
    @SerializedName("mapObj") val mapObj: String? = null,
    @SerializedName("firstStartStation") val firstStartStation: String? = null,
    @SerializedName("lastEndStation") val lastEndStation: String? = null,
    @SerializedName("totalStationCount") val totalStationCount: Int? = null
)

data class OdsaySubPath(
    @SerializedName("trafficType") val trafficType: Int? = null, // 1: 지하철, 2: 버스, 3: 도보
    @SerializedName("distance") val distance: Int? = null,
    @SerializedName("sectionTime") val sectionTime: Int? = null,
    @SerializedName("stationCount") val stationCount: Int? = null,
    @SerializedName("startName") val startName: String? = null,
    @SerializedName("startX") val startX: Double? = null,
    @SerializedName("startY") val startY: Double? = null,
    @SerializedName("endName") val endName: String? = null,
    @SerializedName("endX") val endX: Double? = null,
    @SerializedName("endY") val endY: Double? = null,
    @SerializedName("lane") val lane: List<OdsayLane>? = null,
    @SerializedName("passStopList") val passStopList: OdsayPassStopList? = null
)

data class OdsayLane(
    @SerializedName("name") val name: String? = null,
    @SerializedName("busNo") val busNo: String? = null,
    @SerializedName("subwayCode") val subwayCode: Int? = null,
    @SerializedName("type") val type: Int? = null
)

data class OdsayPassStopList(
    @SerializedName("stations") val stations: List<OdsayStation>? = null
)

data class OdsayStation(
    @SerializedName("index") val index: Int? = null,
    @SerializedName("stationID") val stationID: Int? = null,
    @SerializedName("stationName") val stationName: String? = null,
    @SerializedName("x") val x: String? = null,
    @SerializedName("y") val y: String? = null
)
