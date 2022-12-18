package top.iseason.bukkit.sakurabind.task

import org.bukkit.Location
import org.bukkit.entity.Entity
import java.util.concurrent.ConcurrentLinkedQueue

object FallingList {
    private val falling = ConcurrentLinkedQueue<Entity>()

    fun check() {
        if (falling.isEmpty()) return
        val iterator = falling.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.isDead) {
                iterator.remove()
                continue
            }
        }
    }

    fun addFalling(falling: Entity) {
        this.falling.add(falling)
    }

    fun findFalling(str: String) = falling.find {
        str == locationToString(it.location)
    }

    fun locationToString(location: Location) =
        "${location.world?.name},${location.blockX},${location.blockY},${location.blockZ}"

}