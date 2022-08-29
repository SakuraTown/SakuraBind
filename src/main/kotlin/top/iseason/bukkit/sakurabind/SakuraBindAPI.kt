package top.iseason.bukkit.sakurabind

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import top.iseason.bukkit.bukkittemplate.utils.bukkit.applyMeta
import top.iseason.bukkit.bukkittemplate.utils.toColor
import java.util.*

/**
 * 绑定API
 */
@Suppress("UNUSED")
object SakuraBindAPI {
    private val SAKURA_BIND = NamespacedKey(SakuraBind.javaPlugin, "sakura_bind")
    private val SAKURA_BIND_LORE = NamespacedKey(SakuraBind.javaPlugin, "sakura_bind_lore")

    /**
     * 将物品绑定玩家
     * @param item 需要绑定的物品
     * @param player 绑定的玩家
     * @param showLore 是否显示lore
     */
    @JvmStatic
    @JvmOverloads
    fun bind(item: ItemStack, player: Player, showLore: Boolean = true) = bind(item, player.uniqueId, showLore)

    /**
     * 将物品绑定UUID
     * @param item 绑定的物品
     * @param uuid 绑定的uuid
     * @param showLore 是否显示lore
     */
    @JvmStatic
    @JvmOverloads
    fun bind(item: ItemStack, uuid: UUID, showLore: Boolean = true) {
        item.applyMeta {
            persistentDataContainer.set(SAKURA_BIND, PersistentDataType.STRING, uuid.toString())
        }
        if (showLore) updateLore(item)
    }

    /**
     * 解绑物品
     * @param item 解绑的物品
     */
    @JvmStatic
    fun unBind(item: ItemStack) {
        item.applyMeta {
            persistentDataContainer.remove(SAKURA_BIND)
        }
        updateLore(item)
    }

    /**
     * 更新绑定物品的lore
     */
    @JvmStatic
    fun updateLore(item: ItemStack) {
        item.applyMeta {
            //删除旧的lore
            val oldLore = persistentDataContainer.get(SAKURA_BIND_LORE, PersistentDataType.STRING)
            if (oldLore != null && hasLore()) {
                val apply = lore!!.apply { remove(oldLore) }
                lore = apply
                persistentDataContainer.remove(SAKURA_BIND_LORE)
            }
            // 没有绑定则返回
            val owner = getOwner(item) ?: return@applyMeta
            val player = Bukkit.getPlayer(owner) ?: Bukkit.getOfflinePlayer(owner)
            val loreStr = Config.lore.replace("%player%", player.name!!).toColor()
            //记录历史
            if (player.hasPlayedBefore()) {
                persistentDataContainer.set(
                    SAKURA_BIND_LORE,
                    PersistentDataType.STRING,
                    loreStr
                )
            }
            //更新lore
            lore = if (hasLore()) {
                lore!!.apply {
                    if (Config.loreIndex >= size - 1)
                        add(loreStr)
                    else add(Config.loreIndex, loreStr)
                }
            } else listOf(loreStr)
        }
    }

    /**
     * 物品是否被绑定
     * @param item 需要判断的物品
     */
    @JvmStatic
    fun hasBind(item: ItemStack): Boolean {
        if (!item.hasItemMeta()) return false
        return item.itemMeta!!.persistentDataContainer.has(SAKURA_BIND, PersistentDataType.STRING)
    }

    /**
     * 获取物品绑定的物主
     * @param item 目标物品
     */
    @JvmStatic
    fun getOwner(item: ItemStack): UUID? {
        if (!item.hasItemMeta()) return null
        val uuidString =
            item.itemMeta!!.persistentDataContainer.get(SAKURA_BIND, PersistentDataType.STRING)
                ?: return null
        return kotlin.runCatching { UUID.fromString(uuidString) }.getOrNull()
    }

    /**
     * 判断物品是否属于某个uuid的
     * @param item 目标物品
     * @param uuid uuid
     *
     */
    @JvmStatic
    fun isOwner(item: ItemStack, uuid: UUID) = uuid == getOwner(item)

    /**
     * 判断物品是否属于某个玩家的
     * @param item 目标物品
     * @param player 目标玩家
     */
    @JvmStatic
    fun isOwner(item: ItemStack, player: Player) = player.uniqueId == getOwner(item)

    /**
     * 获取绑定物品的显示lore，如果是绑定物品返回null
     */
    @JvmStatic
    fun getBindLore(item: ItemStack): String? {
        val owner = getOwner(item) ?: return null
        val player = Bukkit.getPlayer(owner) ?: Bukkit.getOfflinePlayer(owner)
        if (!player.hasPlayedBefore()) return null
        return Config.lore.replace("%player%", player.name!!).toColor()
    }
}