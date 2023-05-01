package top.iseason.bukkit.sakurabind.hook

import dev.lone.itemsadder.api.CustomStack
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.config.matcher.BaseMatcher
import top.iseason.bukkittemplate.hook.BaseHook
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

object ItemsAdderHook : BaseHook("ItemsAdder") {

    fun isItemsAdderItem(item: ItemStack) = CustomStack.byItemStack(item) != null

    fun getItemsAdderId(item: ItemStack): String? = CustomStack.byItemStack(item)?.namespacedID
    fun getItemsAdderNamespace(item: ItemStack) = CustomStack.byItemStack(item)?.namespace

}

class ItemsAdderMatcher : BaseMatcher() {
    private lateinit var ids: Set<String>
    private var matchAll = false
    private var onlyNamespace = false
    override fun getKeys(): Array<String> = arrayOf("itemsadder", "itemsadder-namespace")

    override fun fromSetting(key: String, any: Any): BaseMatcher? {
        val itemsAdderMatcher = ItemsAdderMatcher()
        if (key == "itemsadder-namespace") itemsAdderMatcher.onlyNamespace = true
        if (any is String && any == "all") return itemsAdderMatcher.apply { this.matchAll = true }
        if (any !is Collection<*>) return null
        itemsAdderMatcher.ids = any.map { it.toString().trim() }.toHashSet()
        return itemsAdderMatcher
    }

    override fun tryMatch(item: ItemStack): Boolean {
        if (matchAll) return ItemsAdderHook.isItemsAdderItem(item)
        if (onlyNamespace) return ids.contains(ItemsAdderHook.getItemsAdderNamespace(item))
        return ids.contains(ItemsAdderHook.getItemsAdderId(item))
    }

    override fun onDebug(item: ItemStack, debugHolder: CommandSender) {
        val itemsId =
            if (onlyNamespace) ItemsAdderHook.getItemsAdderNamespace(item) else ItemsAdderHook.getItemsAdderId(item)
        if (matchAll) debugHolder.sendColorMessage(
            Lang.command__test__try_match_itemsadder.formatBy("all", itemsId ?: "", tryMatch(item))
        )
        else debugHolder.sendColorMessage(
            Lang.command__test__try_match_itemsadder
                .formatBy(if (ids.size > 3) "..." else ids.joinToString(), itemsId ?: "", tryMatch(item))
        )
    }

}