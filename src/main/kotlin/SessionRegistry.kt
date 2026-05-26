package com.example

import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

class SessionRegistry {
    private val channels = ConcurrentHashMap<String, CopyOnWriteArraySet<Channel<String>>>()

    fun add(roomId: String): Channel<String> {
        val ch = Channel<String>(Channel.UNLIMITED)
        channels.getOrPut(roomId) { CopyOnWriteArraySet() }.add(ch)
        return ch
    }

    fun remove(roomId: String, ch: Channel<String>) {
        channels[roomId]?.remove(ch)
        ch.close()
    }

    suspend fun broadcast(roomId: String, message: String) {
        val dead = mutableListOf<Channel<String>>()
        channels[roomId]?.forEach { ch ->
            if (!ch.trySend(message).isSuccess) dead.add(ch)
        }
        if (dead.isNotEmpty()) channels[roomId]?.removeAll(dead.toSet())
    }
}
