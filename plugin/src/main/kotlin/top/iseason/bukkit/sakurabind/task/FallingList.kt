package top.iseason.bukkit.sakurabind.task

import org.bukkit.Location
import org.bukkit.entity.Entity
import java.util.concurrent.ConcurrentHashMap

object FallingList {
    private val falling = ConcurrentHashMap<String, Entity>()

    fun check() {
        if (falling.isEmpty()) return
        val iterator = falling.iterator()
        while (iterator.hasNext()) {
            val (_, next) = iterator.next()
            if (next.isDead) {
                iterator.remove()
                continue
            }
        }
    }

    fun addFalling(falling: Entity) {
        this.falling[locationToString(falling.location)] = falling
    }

    fun findFalling(str: String) = falling[str]
    fun findFalling(location: Location) = falling[locationToString(location)]

    fun locationToString(location: Location) =
        "${location.world?.name},${location.blockX},${location.blockY},${location.blockZ}"

}