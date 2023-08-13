package top.iseason.bukkit.sakurabind.command

import com.google.common.cache.CacheStats
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor

object TestCacheCommand : CommandNode(
    name = "cache",
    description = "输出缓存信息",
    default = PermissionDefault.OP,
    async = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val itemCache = ItemSettings.getCacheStats().toStr()
        sender.sendMessage("物品缓存统计: $itemCache")
        if (Config.data_migration__enable) {
            val migCache = Config.getDataMigrationCacheStat()!!.toStr()
            sender.sendMessage("迁移缓存统计: $migCache")
        }
    }

    private fun CacheStats.toStr(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("请求数: ")
        stringBuilder.append(requestCount())
        stringBuilder.append(" | 命中率: ")
        stringBuilder.append("%.2f".format(hitRate()))
        stringBuilder.append(" | 非命中平均耗时: ")
        stringBuilder.append("%.2f".format(averageLoadPenalty() / 1000000))
        stringBuilder.append(" 毫秒")
        return stringBuilder.toString()
    }
}