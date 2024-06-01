package top.iseason.bukkit.sakurabind.hook

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.count
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.dto.PlayerItems
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.other.WeakCoolDown
import java.util.*


object PlaceHolderExpansion : PlaceholderExpansion() {
    private val papiCache = WeakHashMap<String, String>()
    private val coolDown = WeakCoolDown<String>()
    override fun getIdentifier(): String {
        return "sakurabind"
    }

    override fun getAuthor(): String {
        return "Iseason"
    }

    override fun getVersion(): String {
        return BukkitTemplate.getPlugin().description.version
    }

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return null
        when (params.lowercase()) {
            "has_lost" -> {
                val key = "has_lost_${player.uniqueId}$params"
                var result = papiCache[key]
                if (result != null && coolDown.check(key, 3000)) {
                    return result
                }
                if (!DatabaseConfig.isConnected) return null
                result = dbTransaction {
                    val iterator =
                        PlayerItems.select(PlayerItems.id).where { PlayerItems.uuid eq player.uniqueId }.limit(1)
                            .iterator()
                    iterator.hasNext().toString()
                }
                papiCache[key] = result
                return result
            }

            "lost_count" -> {
                val key = "lost_count_${player.uniqueId}$params"
                var result = papiCache[key]
                if (result != null && coolDown.check(key, 1000)) {
                    return result
                }
                if (!DatabaseConfig.isConnected) return null
                result = dbTransaction {
                    PlayerItems
                        .select(PlayerItems.id.count())
                        .where { PlayerItems.uuid eq player.uniqueId }
                        .firstOrNull()?.get(PlayerItems.id.count())?.toString()
                } ?: "0"
                papiCache[key] = result
                return result
            }

            "hasbind_held" -> {
                if (player is Player) {
                    val heldItem = player.getHeldItem() ?: return "null"
                    return SakuraBindAPI.hasBind(heldItem).toString()
                } else return "false"
            }

            "owner_uuid_held" -> {
                if (player is Player) {
                    val heldItem = player.getHeldItem() ?: return "null"
                    return SakuraBindAPI.getOwner(heldItem).toString()
                } else return "null"
            }

            "owner_name_held" -> {
                if (player is Player) {
                    val heldItem = player.getHeldItem() ?: return "null"
                    return SakuraBindAPI.getOwnerName(heldItem).toString()
                } else return "null"
            }

            else -> return null
        }

    }
}