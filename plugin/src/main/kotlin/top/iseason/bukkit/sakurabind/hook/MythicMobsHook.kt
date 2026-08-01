package top.iseason.bukkit.sakurabind.hook

import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.config.matcher.BaseMatcher
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkittemplate.hook.BaseHook
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

object MythicMobsHook : BaseHook("MythicMobs"), Listener {

    fun isMythicItem(item: ItemStack): Boolean {
        if (!hasHooked) return false
        return MythicBukkit.inst().itemManager.isMythicItem(item)
    }

    fun getMythicItemId(item: ItemStack): String? {
        if (!hasHooked) return null
        return MythicBukkit.inst().itemManager.getMythicTypeFromItem(item)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onMythicMobDeath(event: MythicMobDeathEvent) {
        val player = event.killer as? Player ?: return

        event.drops.forEach { item ->
            SakuraBindAPI.tryAutoBind(
                item,
                player,
                "auto-bind.onMythicMobDeath",
                BindType.MYTHIC_MOB_DEATH_BIND_ITEM,
                message = Lang.auto_bind__onMythicMobDeath
            )
        }
    }
}

class MythicMobsMatcher : BaseMatcher() {
    private lateinit var ids: Set<String>
    private var matchAll = false

    override fun getKeys(): Array<String> = arrayOf("mythicmobs")

    override fun fromSetting(key: String, any: Any): BaseMatcher? {
        val matcher = MythicMobsMatcher()
        if (any is String && any == "all") return matcher.apply { matchAll = true }
        if (any !is Collection<*>) return null
        matcher.ids = any.map { it.toString().trim() }.toHashSet()
        return matcher
    }

    override fun tryMatch(item: ItemStack): Boolean {
        if (matchAll) return MythicMobsHook.isMythicItem(item)
        return ids.contains(MythicMobsHook.getMythicItemId(item))
    }

    override fun onDebug(item: ItemStack, debugHolder: CommandSender) {
        val itemId = MythicMobsHook.getMythicItemId(item)
        val configured = if (matchAll) "all" else if (ids.size > 3) "..." else ids.joinToString()
        debugHolder.sendColorMessage(
            Lang.command__test__try_match_mythicmobs.formatBy(configured, itemId ?: "", tryMatch(item))
        )
    }
}
