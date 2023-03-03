package com.jacklabs.blanco.player

import android.graphics.Color

data class Player(
    var uid: String? = null,
    var name: String? = null,
    var host: Boolean = false,
    var color: Int = Color.RED,
    var blanco: Boolean = false,
    var votedOut: Boolean = false,
    var order: Int = 0
)