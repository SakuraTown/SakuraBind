package top.iseason.bukkit.sakurabind

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.cache.BlockCache
import top.iseason.bukkit.sakurabind.cache.EntityCache
import top.iseason.bukkit.sakurabind.config.*
import top.iseason.bukkit.sakurabind.config.DefaultItemSetting.stripLoreColor
import top.iseason.bukkit.sakurabind.event.*
import top.iseason.bukkit.sakurabind.task.DelaySender
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkittemplate.hook.PlaceHolderHook
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.applyMeta
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.noColor
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.toColor
import top.iseason.bukkittemplate.utils.other.EasyCoolDown
import java.util.*

/**
 * 绑定API
 */
@Suppress("UNUSED")
object SakuraBindAPI {
    /**
     * 将物品绑定玩家
     * @param item 需要绑定的物品
     * @param player 绑定的玩家
     * @param showLore 是否显示lore
     */
    @JvmStatic
    @JvmOverloads
    fun bind(
        item: ItemStack,
        player: Player,
        showLore: Boolean = true,
        type: BindType = BindType.API_BIND_ITEM
    ) = bind(item, player.uniqueId, showLore, type)

    /**
     * 将物品绑定UUID
     * @param item 绑定的物品
     * @param uuid 绑定的uuid
     * @param showLore 是否显示lore
     */
    @JvmStatic
    @JvmOverloads
    fun bind(item: ItemStack, uuid: UUID, showLore: Boolean = true, type: BindType) {
        val itemBindEvent = ItemBindEvent(item, ItemSettings.getSetting(item), uuid, type)
        Bukkit.getPluginManager().callEvent(itemBindEvent)
        if (itemBindEvent.isCancelled) return
        val set = NBTEditor.set(item, itemBindEvent.owner.toString(), *Config.nbtPathUuid) ?: return
        item.itemMeta = set.itemMeta
        if (showLore) updateLore(item)
        BindLogger.log(
            itemBindEvent.owner,
            itemBindEvent.bindType,
            itemBindEvent.setting,
            item
        )
    }

    /**
     * 解绑物品
     * @param item 解绑的物品
     */
    @JvmStatic
    fun unBind(item: ItemStack, type: BindType = BindType.API_UNBIND_ITEM) {
        val owner = getOwner(item) ?: return
        val itemUnBIndEvent = ItemUnBIndEvent(item, owner, ItemSettings.getSetting(item), type)
        Bukkit.getPluginManager().callEvent(itemUnBIndEvent)
        if (itemUnBIndEvent.isCancelled) return
        var set = NBTEditor.set(item, null, *Config.nbtPathUuid)
        set = NBTEditor.set(set, null, *ItemSettings.nbtPath)
        item.itemMeta = set.itemMeta
        updateLore(item)
        BindLogger.log(
            itemUnBIndEvent.owner,
            itemUnBIndEvent.bindType,
            itemUnBIndEvent.setting,
            item
        )
    }


    /**
     * 绑定方块
     */
    @JvmStatic
    fun bindBlock(block: Block, uuid: UUID, setting: BaseSetting, type: BindType = BindType.API_BIND_BLOCK) {
        if (!isBlockEnable()) return
        val blockBindEvent = BlockBindEvent(block, setting, uuid, type)
        Bukkit.getPluginManager().callEvent(blockBindEvent)
        if (blockBindEvent.isCancelled) return
        BlockCache.addBlock(block, blockBindEvent.owner, blockBindEvent.setting.keyPath)
        BindLogger.log(
            blockBindEvent.owner,
            blockBindEvent.bindType,
            blockBindEvent.setting,
            block
        )

    }

    /**
     * 解绑方块
     */
    @JvmStatic
    fun unbindBlock(block: Block, type: BindType = BindType.API_UNBIND_BLOCK) {
        if (!isBlockEnable()) return
        val blockOwner = BlockCache.getBlockInfo(block) ?: return
        val blockUnBindEvent = BlockUnBindEvent(block, blockOwner.second, UUID.fromString(blockOwner.first), type)
        Bukkit.getPluginManager().callEvent(blockUnBindEvent)
        if (blockUnBindEvent.isCancelled) return
        BlockCache.removeBlock(block)
        BindLogger.log(
            blockUnBindEvent.owner,
            blockUnBindEvent.bindType,
            blockUnBindEvent.setting,
            block
        )

    }

    /**
     * 绑定实体
     */
    @JvmStatic
    fun bindEntity(entity: Entity, player: Player, setting: BaseSetting, type: BindType = BindType.API_BIND_ENTITY) {
        if (!isEntityEnable()) return
        val uniqueId = player.uniqueId
        val entityBindEvent = EntityBindEvent(entity, setting, uniqueId, type)
        Bukkit.getPluginManager().callEvent(entityBindEvent)
        if (entityBindEvent.isCancelled) return
        val toString = entityBindEvent.owner.toString()
        EntityCache.addEntity(entity, toString, entityBindEvent.setting.keyPath)
        // 1.9 才有这个API
        if (NBTEditor.getMinecraftVersion().greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_9) &&
            entity is LivingEntity &&
            setting.getBoolean("entity-deny.ai", toString, player)
        ) {
            entity.setAI(false)
        }
        if (NBTEditor.getMinecraftVersion().greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_10) &&
            setting.getBoolean("entity-deny.gravity", toString, player)
        ) {
            entity.setGravity(false)
        }
        val name = setting.getString("entity.bind-name")
        if (name.isNotEmpty()) {
            entity.customName = PlaceHolderHook.setPlaceHolder(name.formatBy(player.name, entity.type.name), player)
        }
        BindLogger.log(
            entityBindEvent.owner,
            entityBindEvent.bindType,
            entityBindEvent.setting,
            entity
        )
    }

    /**
     * 解绑实体
     */
    @JvmStatic
    fun unbindEntity(entity: Entity, type: BindType = BindType.API_UNBIND_ENTITY) {
        if (!isEntityEnable()) return
        val entityOwner = EntityCache.getEntityInfo(entity) ?: return
        val entityUnBindEvent = EntityUnBindEvent(entity, entityOwner.second, UUID.fromString(entityOwner.first), type)
        Bukkit.getPluginManager().callEvent(entityUnBindEvent)
        if (entityUnBindEvent.isCancelled) return
        EntityCache.removeEntity(entity)
        BindLogger.log(
            entityUnBindEvent.owner,
            entityUnBindEvent.bindType,
            entityUnBindEvent.setting,
            entity
        )
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
            oldLore = oldLore.map { it.substring(0, it.length - 2) }
            val newLore = itemMeta.lore!!.apply { removeAll(oldLore) }
            temp.applyMeta { this.lore = newLore }
            temp = NBTEditor.set(temp, null, *Config.nbtPathLore)
        }
        val setting = ItemSettings.getSetting(item, true) as ItemSetting
        // 有主人
        if (owner != null) {
            val player = Bukkit.getPlayer(owner) ?: Bukkit.getOfflinePlayer(owner)
            val loreStr = setting.getStringList("item.lore").map { str ->
                var t = str
                t = t.replace("%player%", player.name!!)
                t = PlaceHolderHook.setPlaceHolder(t, player)
                t
            }
//            println(loreStr)
            //添加lore
            temp.applyMeta {
                lore = if (hasLore()) {
                    val lore = LinkedList(lore!!)
                    var index = setting.getInt("item.lore-index")
                    if (setting.removeLore && !setting.lorePatterns.isNullOrEmpty()) {
                        val lorePatterns = setting.lorePatterns!!
                        val patternIter = lorePatterns.iterator()
                        var match = true
                        var pattern = patternIter.next()
                        val indexOfFirst = lore.indexOfFirst {
                            pattern.matcher(if (stripLoreColor) it else it.noColor()).find()
                        }
                        //lore大小小于正则肯定不匹配
                        if (lore.size < indexOfFirst + lorePatterns.size) {
                            match = false
                        } else {
                            //除了第一个lore匹配其他的也得匹配
                            for (i in (indexOfFirst + 1) until (indexOfFirst + lorePatterns.size)) {
                                pattern = patternIter.next()
                                val s = lore[i]
                                if (!pattern.matcher(if (stripLoreColor) s else s.noColor()).find()) {
                                    match = false
                                    break
                                }
                            }
                        }
                        if (match) {
                            repeat(lorePatterns.size) {
                                lore.removeAt(indexOfFirst)
                            }
                            if (setting.getBoolean(
                                    "item.lore-replace-matched",
                                    owner.toString(),
                                    player as? HumanEntity
                                )
                            ) {
                                index = indexOfFirst
                            }
                        }
                    }
                    if (index > lore.size - 1) {
                        lore.addAll(loreStr)
                    } else {
                        lore.addAll(index, loreStr)
                    }
                    lore
                } else loreStr
            }
            //记录历史
            if (player.hasPlayedBefore()) {
                val compound = NBTEditor.getEmptyNBTCompound()
                for ((i, s) in loreStr.withIndex()) {
                    val index = if (i < 10) "0$i" else i.toString()
                    compound.set("", "$s$index")
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

    /**
     * 获取物品绑定的物主
     * @param item 目标物品
     * @return 物主的名字，不存在物主则返回null
     */
    @JvmStatic
    fun getOwnerName(item: ItemStack): String? {
        val owner = getOwner(item) ?: return null
        val player = Bukkit.getPlayer(owner) ?: Bukkit.getOfflinePlayer(owner)
        return player.name
    }

    /**
     * 获取某个uuid的玩家名字
     * @param uuid
     * @return 玩家名字，没有则返回nmull
     */
    @JvmStatic
    fun getOwnerName(uuid: UUID): String? {
        val player = Bukkit.getPlayer(uuid) ?: Bukkit.getOfflinePlayer(uuid)
        return if (player.hasPlayedBefore()) player.name else null
    }

    /**
     * 获取物品的设置
     * @param item
     * @return 绑定设置
     */
    @JvmStatic
    fun getItemSetting(item: ItemStack): BaseSetting {
        return ItemSettings.getSetting(item)
    }

    /**
     * 获取方块的拥有者
     * @param block
     * @return 物主的uuid，没有则返回null
     */
    @JvmStatic
    fun getBlockOwner(block: Block): String? {
        if (!isBlockEnable()) throw IllegalStateException("方块监听器未启用，请在config.yml中打开 'block-listener'")
        return BlockCache.getBlockInfo(block)?.first
    }

    /**
     * 获取方块的信息
     * @param block
     * @return 方块绑定信息
     */
    @JvmStatic
    fun getBlockInfo(block: Block): Pair<String, ItemSetting>? {
        if (!isBlockEnable()) throw IllegalStateException("方块监听器未启用，请在config.yml中打开 'block-listener'")
        return BlockCache.getBlockInfo(block)
    }

    /**
     * 获取方块的设置
     * @param block
     * @return 绑定设置，没有则返回null
     */
    @JvmStatic
    fun getBlockSetting(block: Block): BaseSetting? {
        if (!isBlockEnable()) throw IllegalStateException("方块监听器未启用，请在config.yml中打开 'block-listener'")
        return BlockCache.getBlockInfo(block)?.second
    }

    /**
     * 获取实体的绑定信息
     * @param entity
     * @return 实体的绑定信息
     */
    @JvmStatic
    fun getEntityInfo(entity: Entity): Pair<String, ItemSetting>? {
        if (!isEntityEnable()) throw IllegalStateException("实体监听器未启用，请在config.yml中打开 'block-listener'")
        return EntityCache.getEntityInfo(entity)
    }

    /**
     * 获取实体的拥有者
     * @param entity
     * @return 物主的uuid，没有则返回null
     */
    @JvmStatic
    fun getEntityOwner(entity: Entity): String? {
        if (!isEntityEnable()) throw IllegalStateException("实体监听器未启用，请在config.yml中打开 'block-listener'")
        return EntityCache.getEntityInfo(entity)?.first
    }

    /**
     * 获取方块的设置
     * @param entity
     * @return 绑定设置，没有则返回null
     */
    @JvmStatic
    fun getEntitySetting(entity: Entity): BaseSetting? {
        if (!isEntityEnable()) throw IllegalStateException("实体监听器未启用，请在config.yml中打开 'block-listener'")
        return EntityCache.getEntityInfo(entity)?.second
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
    @JvmStatic
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
                player.sendColorMessage(Lang.send_back_inventory)
        } else release = items.toMutableList()

        /**
         * 开启末影箱缓存下，背包已满但末影箱有空间
         */
        if (player != null && Config.ender_chest_cache && release.isNotEmpty()) {
            val release2 = mutableListOf<ItemStack>()
            val enderChest = player.enderChest
            for (itemStack in release) {
                val addItem = enderChest.addItem(itemStack)
                if (addItem.isNotEmpty()) {
                    release2.addAll(addItem.values)
                }
            }
            if (release.size != release2.size && !EasyCoolDown.check(uuid, 1000))
                player.sendColorMessage(Lang.send_back_ender_chest)
            release = release2
        }

        // 延迟发送队列
        if (release.isNotEmpty()) {
            DelaySender.sendItem(uuid, release)
        }
        return emptyList()
    }


    /**
     * 检查物品行为是否被某个设置禁止
     * @param item 待检测的物品
     * @param player 检测的玩家
     * @param key 检测的选项
     * @return true 表示应该禁止行为
     */
    @JvmStatic
    fun checkDenyBySetting(item: ItemStack?, player: HumanEntity?, key: String): Boolean {
        if (item.checkAir()) return false
        val owner = getOwner(item!!) ?: return false
        return ItemSettings.getSetting(item)
            .getBoolean(key, owner.toString(), player)
    }

    /**
     * 是否启用方块绑定
     */
    @JvmStatic
    fun isBlockEnable() = Config.block_listener

    /**
     * 是否启用实体绑定
     */
    @JvmStatic
    fun isEntityEnable() = Config.entity_listener

}