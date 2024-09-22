@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package top.iseason.bukkittemplate.utils.bukkit

import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir

/**
 * bukkit 的实体相关工具
 */
object EntityUtils {

    /**
     * 给予有物品栏的对象物品,如果是实体且放不下将会放置到实体脚下
     * @param itemStacks 待输入的物品
     *
     */
    fun InventoryHolder.giveItems(itemStacks: Array<ItemStack>) {
        giveItems(*itemStacks)
    }

    /**
     * 给予有物品栏的对象物品,如果是实体且放不下将会放置到实体脚下
     * @param itemStacks 待输入的物品
     *
     */
    fun InventoryHolder.giveItems(itemStacks: Collection<ItemStack>) {
        giveItems(itemStacks.toTypedArray())
    }

    /**
     * 给予有物品栏的对象物品,如果是实体且放不下将会放置到实体脚下
     * @param itemStacks 待输入的物品
     *
     */
    @JvmName("giveItemsVararg")
    fun InventoryHolder.giveItems(vararg itemStacks: ItemStack) {
        val addItems = inventory.addItem(*itemStacks).values
        if (this !is Entity) return
        for (addItem in addItems) {
            if (addItem == null) continue
            val item = world.spawnEntity(location, EntityType.DROPPED_ITEM) as Item
            item.itemStack = addItem
        }
    }

    //    @JvmName("giveItemsOrDrop")
    fun InventoryHolder.giveItem(itemStack: ItemStack) {
        val addItems = inventory.addItem(itemStack).values
        if (this !is Entity) return
        for (addItem in addItems) {
            if (addItem == null) continue
            val item = world.spawnEntity(location, EntityType.DROPPED_ITEM) as Item
            item.itemStack = addItem
        }
    }

    /**
     * 获取玩家手上拿着的物品,兼容低版本
     * @return 没有或者是空气都返回null
     */
    fun PlayerInventory.getHeldItem(): ItemStack? {
        val item = getItem(heldItemSlot)
        if (item.checkAir()) return null
        return item
    }

    /**
     * 获取玩家手上拿着的物品,兼容低版本
     * @return 没有或者是空气都返回null
     */
    fun Player.getHeldItem(): ItemStack? = inventory.getHeldItem()

}