package top.iseason.bukkit.sakurabind.task

import org.bukkit.entity.Entity
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.ConcurrentHashMap

object EntityRemoveQueue : BukkitRunnable() {
    private val isRemoving = ConcurrentHashMap.newKeySet<Entity>()

    /**
     * 同步删除实体，存在一定延迟
     */
    fun syncRemove(item: Entity) {
        if (item.isDead) return
        // 为了兼容尽可能多的mod服务端和游戏版本
        // 只能让它传送到玩家碰不到的地方，再在事件结束后删除
        val location = item.location
        location.y = Short.MIN_VALUE.toDouble()
        item.teleport(location)
        isRemoving.add(item)
    }

    /**
     * 实体是否被标记删除
     */
    fun isRemoved(entity: Entity) = entity.isDead || isRemoving.contains(entity)

    override fun run() {
        if (isRemoving.isEmpty()) {
            return
        }
        for (entity in isRemoving) {
            entity.remove()
        }
        isRemoving.clear()
    }
}