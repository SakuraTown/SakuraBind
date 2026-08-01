package top.iseason.bukkit.sakurabind.hook

import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import pers.neige.neigeitems.event.ItemGiveEvent
import pers.neige.neigeitems.event.ItemPackGiveEvent
import pers.neige.neigeitems.event.MythicDropEvent
import pers.neige.neigeitems.manager.ItemManager
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.config.matcher.BaseMatcher
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkittemplate.hook.BaseHook
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

object NeigeItemsHook : BaseHook("NeigeItems"), Listener {

    fun isNeigeItem(item: ItemStack): Boolean = hasHooked && ItemManager.isNiItem(item) != null

    fun getNeigeItemId(item: ItemStack): String? {
        if (!hasHooked) return null
        return ItemManager.getItemId(item)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onItemPackGive(event: ItemPackGiveEvent) {
        val player = event.player
        event.itemStacks.forEach { item ->
            SakuraBindAPI.tryAutoBind(
                item,
                player,
                "auto-bind.onNeigeItemsGive",
                BindType.NEIGE_ITEMS_GIVE_BIND_ITEM,
                message = Lang.auto_bind__onNeigeItemsGive
            )
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onItemGive(event: ItemGiveEvent) {
        SakuraBindAPI.tryAutoBind(
            event.itemStack,
            event.player,
            "auto-bind.onNeigeItemsGive",
            BindType.NEIGE_ITEMS_GIVE_BIND_ITEM,
            message = Lang.auto_bind__onNeigeItemsGive
        )
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMythicDrop(event: MythicDropEvent.Drop) {
        val player = event.player ?: return
        val items = event.dropItems.asSequence() + event.fishDropItems.orEmpty().asSequence()
        items.forEach { item ->
            SakuraBindAPI.tryAutoBind(
                item,
                player,
                "auto-bind.onMythicMobDeath",
                BindType.NEIGE_ITEMS_MYTHIC_DROP_BIND_ITEM,
                message = Lang.auto_bind__onMythicMobDeath
            )
        }
    }
}

class NeigeItemsMatcher : BaseMatcher() {
    private lateinit var ids: Set<String>
    private var matchAll = false

    override fun getKeys(): Array<String> = arrayOf("neigeitems")

    override fun fromSetting(key: String, any: Any): BaseMatcher? {
        val matcher = NeigeItemsMatcher()
        if (any is String && any == "all") return matcher.apply { matchAll = true }
        if (any !is Collection<*>) return null
        matcher.ids = any.map { it.toString().trim() }.toHashSet()
        return matcher
    }

    override fun tryMatch(item: ItemStack): Boolean {
        if (matchAll) return NeigeItemsHook.isNeigeItem(item)
        return ids.contains(NeigeItemsHook.getNeigeItemId(item))
    }

    override fun onDebug(item: ItemStack, debugHolder: CommandSender) {
        val itemId = NeigeItemsHook.getNeigeItemId(item)
        val configured = if (matchAll) "all" else if (ids.size > 3) "..." else ids.joinToString()
        debugHolder.sendColorMessage(
            Lang.command__test__try_match_neigeitems.formatBy(configured, itemId ?: "", tryMatch(item))
        )
    }
}
