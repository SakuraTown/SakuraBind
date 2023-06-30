package top.iseason.bukkit.sakurabind.task

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSetting
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkittemplate.debug.debug
import top.iseason.bukkittemplate.debug.warn
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.getDisplayName
import java.util.*
import java.util.concurrent.TimeUnit

class MigrationScanner : BukkitRunnable() {

    private val cache: Cache<ItemStack, Any> = CacheBuilder
        .newBuilder()
        .expireAfterAccess(Config.data_migration__period * 60, TimeUnit.SECONDS)
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .build()

    override fun run() {
        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            var index = 0
            val openInventory = onlinePlayer.openInventory
            try {
                while (true) {
                    val item = openInventory.getItem(index++) ?: continue
                    if (item.checkAir()) continue
                    // 10秒缓存不再检查
                    if (cache.getIfPresent(item) != null) {
                        continue
                    }
                    val pair = checkMigration(item)
                    if (pair == null) {
                        cache.put(item, true)
                        continue
                    }
                    val (player, start) = pair
                    if (Config.data_migration__dont_bind) continue
                    var setting = Config.dataMigrationSetting ?: ItemSettings.getSetting(item, false)
                    val uuid =
                        if (Config.data_migration__is_uuid) runCatching { UUID.fromString(player) }.getOrNull() else {
                            val offlinePlayer = Bukkit.getPlayer(player) ?: Bukkit.getOfflinePlayer(player)
                            if (offlinePlayer.hasPlayedBefore()) offlinePlayer.uniqueId else null
                        }
                    if (uuid == null) {
                        warn("数据迁移功能在 ${onlinePlayer.name} 身上检测到了lore绑定信息 $player ，但它不是一个有效的玩家名字或者uuid, 已忽略")
                        continue
                    }
                    // 兼容位置选择
                    if (Config.data_migration__remove_lore && setting.getBoolean(
                            "item.lore-replace-matched",
                            uuid.toString(),
                            onlinePlayer
                        )
                    ) {
                        setting = setting.clone()
                        (setting as? ItemSetting)?.setting?.set("item.lore-index", start)
                    }
                    SakuraBindAPI.bind(
                        item,
                        uuid,
                        setting = setting,
                        type = BindType.MIGRATION_FROM_LORE_ITEM
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun checkMigration(item: ItemStack): Pair<String, Int>? {
        if (!item.hasItemMeta()) return null
        if (SakuraBindAPI.hasBind(item)) {
            return null
        }
        var player: String? = null
        val removeLore = Config.data_migration__remove_lore
        val itemMeta = item.itemMeta!!
        var start = -1
        with(itemMeta) {
            if (!hasLore()) return null
            val lore = lore!!
            val dataMigrationLore = Config.dataMigrationLore
            if (dataMigrationLore.isEmpty() || dataMigrationLore.size > lore.size) return null
            val patternIterator = Config.dataMigrationLore.iterator()
            var pattern = patternIterator.next()
            var end = -1
            for ((i, s) in lore.withIndex()) {
                val matcher = pattern.matcher(s)
                if (matcher.find()) {
                    if (start < 0) start = i
                    end = i
                    if (player == null && matcher.groupCount() >= 1) {
                        player = matcher.group(1).trim()
                        if (!removeLore) break
                    }
                    if (patternIterator.hasNext()) pattern = patternIterator.next()
                    else break
                } else if (start >= 0) {
                    if (patternIterator.hasNext()) player = null
                    break
                }
            }
            if (removeLore && player != null) {
                repeat(end - start + 1) {
                    lore.removeAt(start)
                }
                itemMeta.lore = lore
            }
        }
        if (removeLore && player != null) item.itemMeta = itemMeta
        if (player != null) debug("&7从物品 &f${item.getDisplayName() ?: item.type} &7中检测到 &6$player &7的可迁移绑定的数据，已迁移。")

        return if (player == null) null else player!! to start
    }
}