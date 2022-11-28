package top.iseason.bukkit.sakurabind.hook

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.jetbrains.exposed.sql.select
import top.iseason.bukkit.sakurabind.dto.PlayerItems
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.dbTransaction


object PlaceHolderExpansion : PlaceholderExpansion() {
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
        if (params.equals("has_lost", ignoreCase = true)) {
            if (!DatabaseConfig.isConnected) return null
            return dbTransaction {
                val iterator =
                    PlayerItems.slice(PlayerItems.id).select { PlayerItems.uuid eq player.uniqueId }.limit(1).iterator()
                iterator.hasNext().toString()
            }
        }
        return null
    }
}