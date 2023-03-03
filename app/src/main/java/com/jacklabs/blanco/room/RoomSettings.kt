package com.jacklabs.blanco.room

data class RoomSettings(
    val mode: String = "classic",
    val automatic: Boolean = true,
    val numBlancos: Int = 1,
    val time: Int = 10
)
