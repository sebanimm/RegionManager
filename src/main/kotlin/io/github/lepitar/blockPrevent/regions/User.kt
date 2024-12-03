package io.github.lepitar.blockPrevent.regions

import org.bukkit.entity.Player
import java.util.UUID

interface User {
    val uniqueId: UUID

    val name: String

    val bukkitPlayer: Player?

    val isOnline: Boolean
        get() {
            return bukkitPlayer != null
        }

    fun sendMessage(message: String) {
        bukkitPlayer?.sendMessage(message)
    }
}