package top.iseason.bukkit.sakurabind.task

import org.bukkit.Location
import org.bukkit.entity.Entity
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object FallingList {
    private val falling = ConcurrentHashMap<UUID, Entity>()
    private val fallingLocation = ConcurrentHashMap<String, UUID>()
    private val entityLocation = ConcurrentHashMap<UUID, String>()

    fun check() {
        if (falling.isEmpty()) return
        val iterator = falling.iterator()
        while (iterator.hasNext()) {
            val (uuid, next) = iterator.next()
            if (next.isDead || !next.isValid) {
                iterator.remove()
                removeLocation(uuid)
                continue
            }
            indexLocation(uuid, locationToString(next.location))
        }
    }

    fun addFalling(falling: Entity) {
        val uuid = falling.uniqueId
        this.falling[uuid] = falling
        indexLocation(uuid, locationToString(falling.location))
    }

    fun removeFalling(falling: Entity) {
        val uuid = falling.uniqueId
        this.falling.remove(uuid)
        removeLocation(uuid)
    }

    fun findFalling(str: String): Entity? {
        fallingLocation[str]?.let { uuid ->
            val entity = falling[uuid]
            if (entity != null && !entity.isDead && entity.isValid && locationToString(entity.location) == str) {
                return entity
            }
            removeLocation(uuid)
        }
        val entity = falling.values.firstOrNull { !it.isDead && it.isValid && locationToString(it.location) == str }
            ?: return null
        indexLocation(entity.uniqueId, str)
        return entity
    }

    fun findFalling(location: Location) = findFalling(locationToString(location))

    fun locationToString(location: Location) =
        "${location.world?.name},${location.blockX},${location.blockY},${location.blockZ}"

    private fun indexLocation(uuid: UUID, location: String) {
        val oldLocation = entityLocation.put(uuid, location)
        if (oldLocation != null && oldLocation != location) {
            fallingLocation.remove(oldLocation, uuid)
        }
        fallingLocation[location] = uuid
    }

    private fun removeLocation(uuid: UUID) {
        val oldLocation = entityLocation.remove(uuid) ?: return
        fallingLocation.remove(oldLocation, uuid)
    }

}
