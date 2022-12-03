package top.iseason.bukkit.sakurabind

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.task.DelaySender
import top.iseason.bukkittemplate.hook.PlaceHolderHook
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.applyMeta
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.toColor
import top.iseason.bukkittemplate.utils.other.EasyCoolDown
import java.util.*

/**
 * 绑定API
 */
@Suppress("UNUSED")
object SakuraBindAPI {
//    private val SAKURA_BIND = NamespacedKey(SakuraBind.javaPlugin, "sakura_bind")
//    private val SAKURA_BIND_LORE = NamespacedKey(SakuraBind.javaPlugin, "sakura_bind_lore")

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
        val set = NBTEditor.set(item, uuid.toString(), *Config.nbtPathUuid) ?: return
        item.itemMeta = set.itemMeta
        if (showLore) updateLore(item)
    }

    /**
     * 解绑物品
     * @param item 解绑的物品
     */
    @JvmStatic
    fun unBind(item: ItemStack) {
        val set = NBTEditor.set(item, null, *Config.nbtPathUuid)
        item.itemMeta = set.itemMeta
        updateLore(item)
    }

    /**
     * 更新绑定物品的lore
     */
    @JvmStatic
    fun updateLore(item: ItemStack) {

        val itemMeta = item.itemMeta ?: return
        var oldLore = NBTEditor.getKeys(item, *Config.nbtPathLore)
//        println(NBTEditor.getNBTCompound(item).toString())
//        println(oldLore)
        val owner = getOwner(item)
        var temp = item
        //有旧的lore,先删除
        if (oldLore != null && itemMeta.hasLore()) {
            oldLore = oldLore.map { it.substring(0, it.length - 1) }
            val newLore = itemMeta.lore!!.apply { removeAll(oldLore) }
            temp.applyMeta { this.lore = newLore }
            temp = NBTEditor.set(temp, null, *Config.nbtPathLore)
        }
        val setting = ItemSettings.getSetting(item, true)
        // 有主人
        if (owner != null) {
            val player = Bukkit.getPlayer(owner) ?: Bukkit.getOfflinePlayer(owner)
            val loreStr = setting.getStringList("lore").map { str ->
                var t = str
                t = t.replace("%player%", player.name!!)
                t = PlaceHolderHook.setPlaceHolder(t, player)
                t
            }
//            println(loreStr)
            //添加lore
            temp.applyMeta {
                lore = if (hasLore()) {
                    lore!!.apply {
                        val index = setting.getInt("lore-index")
                        if (index >= size - 1)
                            addAll(loreStr)
                        else addAll(index, loreStr)
                    }
                } else loreStr
//                println(lore)
            }
            //记录历史
            if (player.hasPlayedBefore()) {
                val compound = NBTEditor.getEmptyNBTCompound()
                for ((i, s) in loreStr.withIndex()) {
                    compound.set("", "$s$i")
                }
                temp = NBTEditor.set(temp, compound, *Config.nbtPathLore)
            }
        }
        item.itemMeta = temp.itemMeta
    }

    /**
     * 物品是否被绑定
     * @param item 需要判断的物品
     */
    @JvmStatic
    fun hasBind(item: ItemStack): Boolean {
        if (!item.hasItemMeta()) return false
        return getOwner(item) != null
    }

    /**
     * 获取物品绑定的物主
     * @param item 目标物品
     */
    @JvmStatic
    fun getOwner(item: ItemStack): UUID? {
        if (!item.hasItemMeta()) return null
        val uuidString = NBTEditor.getString(item, *Config.nbtPathUuid) ?: return null
        return kotlin.runCatching { UUID.fromString(uuidString) }.getOrNull()
    }

//    fun isTileEntity(block: Block): Boolean {
//        return block.chunk.tileEntities.any { it.block == block }
//    }
//
//    fun getTileOwner(block: Block): String? {
//        return NBTEditor.getString(block, *Config.nbtPathUuid)
//    }
//
//    fun setTileOwner(block: Block, uuid: UUID) {
//        NBTEditor.set(block, uuid.toString(),"Tags", *Config.nbtPathUuid)
//    }

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
    fun isOwner(item: ItemStack, player: Player) = isOwner(item, player.uniqueId)

    /**
     * 获取绑定物品的显示lore，如果是绑定物品返回null
     */
    @JvmStatic
    fun getBindLore(item: ItemStack): List<String>? {
        val owner = getOwner(item) ?: return null
        val player = Bukkit.getPlayer(owner) ?: Bukkit.getOfflinePlayer(owner)
        if (!player.hasPlayedBefore()) return null
        return ItemSettings.getSetting(item).getStringList("lore")
            .map { it.replace("%player%", player.name!!).toColor() }
    }

    /**
     * 将物品送回物主，玩家在线优先进背包
     */
    fun sendBackItem(uuid: UUID, items: List<ItemStack>): List<ItemStack> {
        val player = Bukkit.getPlayer(uuid)
        //物主在线
        var release = mutableListOf<ItemStack>()
        if (player != null && player.isOnline) {
            for (itemStack in items) {
                val addItem = player.inventory.addItem(itemStack)
                if (addItem.isNotEmpty()) {
                    release.addAll(addItem.values)
                }
            }
            //全部返还
            if (release.isEmpty()) {
                if (!EasyCoolDown.check(uuid, 1000))
                    player.sendColorMessage(Lang.send_back_all)
                return release
            }
            if (!EasyCoolDown.check(uuid, 1000))
                player.sendColorMessage(Lang.send_back)
        } else release = items.toMutableList()

        // 使用邮件发送

        if (release.isNotEmpty()) {
            DelaySender.sendItem(uuid, release)
        }
        return emptyList()
    }
}