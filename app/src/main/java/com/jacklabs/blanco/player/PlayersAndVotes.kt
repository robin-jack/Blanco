package com.jacklabs.blanco.player

data class PlayersAndVotes(
    var players: HashMap<String, Player> = hashMapOf(),
    var votes: HashMap<String, Int> = hashMapOf()
)