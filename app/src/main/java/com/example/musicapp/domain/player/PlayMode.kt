package com.example.musicapp.domain.player

enum class PlayMode {
    STOP,
    NEXT,
    SHUFFLE;

    fun next(): PlayMode {
        return values()[(ordinal + 1) % values().size]
    }
}
