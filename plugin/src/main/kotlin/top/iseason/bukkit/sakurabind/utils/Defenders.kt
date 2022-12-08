package top.iseason.bukkit.sakurabind.utils

import org.bukkit.entity.Entity
import top.iseason.bukkit.sakurabind.config.BaseSetting

class Defenders(val target: Entity, val setting: BaseSetting) {
    private val sets = LinkedHashSet<Entity>()
    private var iterator: MutableIterator<Entity> = sets.iterator()

    /**
     * 添加肉盾
     */
    fun addDefender(defender: Entity) {
        if (sets.contains(defender)) return
        sets.add(defender)
        iterator = sets.iterator()
    }

    /**
     * 使用轮询的方式来获取
     */
    fun getDefender(): Entity? {
        var defender: Entity? = null
        while (iterator.hasNext()) {
            val next = iterator.next()
            val l1 = next.location
            val l2 = target.location
            if (next.isDead || l1.world != l2.world || l1.distance(l2) >= setting.getDouble("entity-deny.defend-distance")) iterator.remove()
            defender = next
            break
        }
        if (!iterator.hasNext()) {
            iterator = sets.iterator()
        }
        return defender
    }
}