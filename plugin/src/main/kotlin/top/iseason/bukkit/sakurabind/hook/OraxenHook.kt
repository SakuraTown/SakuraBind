package top.iseason.bukkit.sakurabind.hook

import io.th0rgal.oraxen.api.OraxenItems
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.config.matcher.BaseMatcher
import top.iseason.bukkittemplate.hook.BaseHook
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

object OraxenHook : BaseHook("Oraxen") {
    fun isOraxenItem(item: ItemStack) = OraxenItems.getIdByItem(item) != null
    fun getOraxenItemId(item: ItemStack): String? = OraxenItems.getIdByItem(item)
}

class OraxenMatcher : BaseMatcher() {
    private lateinit var ids: Set<String>
    private var matchAll = false

    override fun getKeys(): Array<String> = arrayOf("oraxen")

    override fun fromSetting(key: String, any: Any): BaseMatcher? {
        if (any is String && any == "all") return OraxenMatcher().apply { this.matchAll = true }
        if (any !is Collection<*>) return null
        val oraxenMatcher = OraxenMatcher()
        oraxenMatcher.ids = any.map { it.toString().trim() }.toHashSet()
        return oraxenMatcher
    }

    override fun tryMatch(item: ItemStack): Boolean {
        if (matchAll) return OraxenHook.isOraxenItem(item)
        return ids.contains(OraxenHook.getOraxenItemId(item))
    }

    override fun onDebug(item: ItemStack, debugHolder: CommandSender) {
        val itemsId = OraxenHook.getOraxenItemId(item)
        if (matchAll) debugHolder.sendColorMessage(
            Lang.command__test__try_match_oraxen.formatBy("all", itemsId ?: "", tryMatch(item))
        )
        else debugHolder.sendColorMessage(
            Lang.command__test__try_match_oraxen
                .formatBy(if (ids.size > 3) "..." else ids.joinToString(), itemsId ?: "", tryMatch(item))
        )
    }

}