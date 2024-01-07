package top.iseason.bukkit.sakurabind.hook

import net.Indyuce.mmoitems.MMOItems
import net.Indyuce.mmoitems.api.event.ItemBuildEvent
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.config.matcher.BaseMatcher
import top.iseason.bukkittemplate.hook.BaseHook
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

object MMOItemsHook : BaseHook("MMOItems"), org.bukkit.event.Listener {

    @EventHandler
    fun onItemBuild(event: ItemBuildEvent) {
        val itemStack = event.itemStack
        if (!SakuraBindAPI.hasBind(itemStack)) {
            return
        }
        SakuraBindAPI.updateLore(itemStack)
    }

    fun isMMOItemsItem(item: ItemStack): Boolean {
        if (!hasHooked) return false
        return MMOItems.getType(item) != null
    }

    /**
     * type:id
     */
    fun getMMOItemsId(item: ItemStack): String? {
        if (!hasHooked) return null
        val type = MMOItems.getType(item)?.id ?: return null
        val id = MMOItems.getID(item) ?: return null
        return "$type:$id"
    }

    /**
     * type
     */
    fun getMMOItemsType(item: ItemStack): String? {
        if (!hasHooked) return null
        return MMOItems.getType(item)?.id
    }

}

class MMOItemsMatcher : BaseMatcher() {
    private lateinit var ids: Set<String>
    private var matchAll = false
    private var onlyType = false
    override fun getKeys(): Array<String> = arrayOf("mmoitems", "mmoitems-type")

    override fun fromSetting(key: String, any: Any): BaseMatcher? {
        val mmoItemsMatcher = MMOItemsMatcher()
        if (key == "mmoitems-type") mmoItemsMatcher.onlyType = true
        if (any is String && any == "all") return mmoItemsMatcher.apply { this.matchAll = true }
        if (any !is Collection<*>) return null
        mmoItemsMatcher.ids = any.map { it.toString().trim() }.toHashSet()
        return mmoItemsMatcher
    }

    override fun tryMatch(item: ItemStack): Boolean {
        if (matchAll) return MMOItemsHook.isMMOItemsItem(item)
        if (onlyType) return ids.contains(MMOItemsHook.getMMOItemsType(item))
        return ids.contains(MMOItemsHook.getMMOItemsId(item))
    }

    override fun onDebug(item: ItemStack, debugHolder: CommandSender) {
        val mmoItemsId = if (onlyType) MMOItemsHook.getMMOItemsType(item) else MMOItemsHook.getMMOItemsId(item)
        if (matchAll) debugHolder.sendColorMessage(
            Lang.command__test__try_match_mmoitems.formatBy("all", mmoItemsId ?: "", tryMatch(item))
        )
        else debugHolder.sendColorMessage(
            Lang.command__test__try_match_mmoitems
                .formatBy(if (ids.size > 3) "..." else ids.joinToString(), mmoItemsId ?: "", tryMatch(item))
        )
    }
}
//
//object SakuraBindStat : BooleanStat(
//    "SAKURA_BIND",
//    Material.REDSTONE_LAMP,
//    "樱花绑定",
//    arrayOf("SakuraBind 自动绑定开关", "效果就是插入 自动绑定的 NBT", "在config.yml 中配置"),
//    arrayOf("all"),
//    *emptyArray<Material>()
//) {
//    override fun whenApplied(item: ItemStackBuilder, data: BooleanData) {
//        super.whenApplied(item, data)
//        item.addItemTag(ItemTag(Config.auto_bind_nbt, ""))
//    }
//}