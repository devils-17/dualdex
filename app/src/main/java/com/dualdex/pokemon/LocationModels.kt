package com.dualdex.pokemon

enum class RegionId(val displayName: String) {
    JOHTO("Johto"),
    KANTO("Kanto"),
    HOENN("Hoenn")
}

enum class MapNodeType {
    TOWN,
    CITY,
    ROUTE,
    DUNGEON,
    LANDMARK,
    FACILITY
}

/**
 * Raw player location read from SaveBlock1 in GBA EWRAM at runtime.
 */
data class PlayerLocation(
    val mapGroup: Int,
    val mapNum: Int,
    val warpId: Int,
    val x: Int,
    val y: Int,
    val localX: Int,
    val localY: Int,
    val escapeMapGroup: Int,
    val escapeMapNum: Int,
    val isIndoors: Boolean,
    val isValid: Boolean
)

/**
 * High-level resolved map location section for display on the companion Town Map.
 */
data class RegionMapSection(
    val id: String,
    val name: String,
    val region: RegionId,
    val gridX: Int,           // 0..27 on standard GBA Town Map
    val gridY: Int,           // 0..14 on standard GBA Town Map
    val width: Int = 1,
    val height: Int = 1,
    val nodeType: MapNodeType = MapNodeType.ROUTE,
    val description: String = "",
    val landmarks: List<String> = emptyList(),
    val gymLeader: String? = null,
    val badge: String? = null,
    val connections: List<String> = emptyList()
)
