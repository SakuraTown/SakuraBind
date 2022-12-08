package top.iseason.bukkit.sakurabind.listener

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.*
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.cache.EntityCache
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.Defenders
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.toColor
import top.iseason.bukkittemplate.utils.other.submit
import java.util.*

object EntityListener : Listener {
    private val temp = HashMap<String, Pair<Player, ItemStack>>()

    //某个实体的防卫者
    private val defenderMap = WeakHashMap<Entity, Defenders>()

    /**
     * 检测生物蛋的模块
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) return
        val item = event.item ?: return
        if (Config.checkByPass(event.player)) {
            return
        }
        val owner = SakuraBindAPI.getOwner(item) ?: return
        if (ItemSettings.getSetting(item).getBoolean("entity.spawn-check", owner.toString(), event.player)) {
            return
        }
        val player = event.player
        val clickedBlock = event.clickedBlock
        var location: Location? = null
        if (clickedBlock != null) {
            location = if (clickedBlock.type.isSolid) {
                clickedBlock.getRelative(event.blockFace).location
            } else clickedBlock.location

        } else {
            val block = player.eyeLocation.block
            if (!block.type.checkAir()) {
                location = player.eyeLocation
            }
        }
        if (location == null) return
        val str = "${location.blockX},${location.blockY},${location.blockZ}"
        temp[str] = event.player to item
        submit {
            temp.remove(str)
        }
    }

    /**
     * 检测生物蛋的模块
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCreatureSpawnEvent(event: CreatureSpawnEvent) {
        if (event.spawnReason != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) return
        val location = event.entity.location
        val str = "${location.blockX},${location.blockY},${location.blockZ}"
        val pair = temp[str] ?: return
        temp.remove(str)
        val player = pair.first
        if (pair.second != player.getHeldItem()) return
        val owner = SakuraBindAPI.getOwner(pair.second)
        val setting = ItemSettings.getSetting(pair.second)
        val entity = event.entity
        EntityCache.addEntity(entity, owner.toString(), setting.keyPath)
        // 1.9 才有这个API
        if (NBTEditor.getMinecraftVersion().greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_9) &&
            setting.getBoolean("entity-deny.ai", owner.toString(), player)
        ) {
            entity.setAI(false)
        }
        if (NBTEditor.getMinecraftVersion().greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_10) &&
            setting.getBoolean("entity-deny.gravity", owner.toString(), player)
        ) {
            entity.setGravity(false)
        }
        val name = setting.getString("entity.bind-name")
        if (name.isNotEmpty()) {
            entity.customName = name.formatBy(player.name, entity.type.name).toColor()
        }
        MessageTool.messageCoolDown(player, Lang.entity_bind_on_spawner_egg)
    }

    /**
     * 禁止交互
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInteractAtEntityEvent(event: PlayerInteractAtEntityEvent) {
        if (Config.checkByPass(event.player)) {
            return
        }
        val rightClicked = event.rightClicked
        val entityOwner = EntityCache.getEntityOwner(rightClicked) ?: return
        if (entityOwner.second.getBoolean("entity-deny.interact", entityOwner.first, event.player)) {
            MessageTool.denyMessageCoolDown(
                event.player,
                Lang.entity__deny_interact.formatBy(SakuraBindAPI.getOwnerName(UUID.fromString(entityOwner.first))),
                entityOwner.second,
                entity = rightClicked
            )
            event.isCancelled = true
        }
    }

    /**
     * 禁止被实体攻击
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
        val entity = event.entity
        val player = event.damager as? Player
        if (player != null && Config.checkByPass(player)) {
            return
        }
        val entityOwner = EntityCache.getEntityOwner(entity) ?: return
        if (player != null) {
            if (entityOwner.second.getBoolean("entity-deny.damage-by-player", entityOwner.first, player)) {
                event.isCancelled = true
                MessageTool.denyMessageCoolDown(
                    player,
                    Lang.entity__deny_damage.formatBy(SakuraBindAPI.getOwnerName(UUID.fromString(entityOwner.first))),
                    entityOwner.second,
                    entity = entity
                )
            }
            return
        }
        if (entityOwner.second.getBoolean(
                "entity-deny.damage-by-entity",
                entityOwner.first,
                player
            )
        ) {
            event.isCancelled = true
        }
    }

    /**
     * 禁止受伤
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onEntityDamageEvent(event: EntityDamageEvent) {
        val entity = event.entity
        val entityOwner = EntityCache.getEntityOwner(entity) ?: return
        if (entityOwner.second.getBoolean("entity-deny.damage", null, null)) {
            event.isCancelled = true
        }
    }

    /**
     * 实体死亡去掉缓存
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDeathEvent(event: EntityDeathEvent) {
        val entity = event.entity
        if (EntityCache.getEntityOwner(entity) == null) {
            return
        }
        EntityCache.removeEntity(entity)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityTargetEvent(event: EntityTargetEvent) {
        //被攻击者
        val target = event.target ?: return
        val player = target as? Player
        if (player != null && Config.checkByPass(player)) {
            return
        }
        // 攻击者
        val entity = event.entity
        val entityOwner = EntityCache.getEntityOwner(entity)
        if (entityOwner != null && !entityOwner.second.getBoolean("entity-deny.friendly", entityOwner.first, player)) {
            if (!entityOwner.second.getBoolean("entity-deny.defend", entityOwner.first, player)) {
                defenderMap.computeIfAbsent(target) { Defenders(target, entityOwner.second) }.addDefender(entity)
            }
            event.isCancelled = true
        }
        if (event.isCancelled) return
        val defenders = defenderMap[target]
        if (defenders != null) {
            val defender = defenders.getDefender()
            if (defender == null) {
                defenderMap.remove(target)
                return
            }
            event.target = defender
        }
    }


}