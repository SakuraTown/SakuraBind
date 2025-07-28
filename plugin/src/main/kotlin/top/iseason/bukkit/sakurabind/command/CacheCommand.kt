package top.iseason.bukkit.sakurabind.command

import com.google.common.cache.CacheStats
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.module.AutoUnBindConfig
import top.iseason.bukkit.sakurabind.config.module.MigrationConfig
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor

object CacheCommand : CommandNode(
    name = "cache",
    description = "输出缓存信息",
    default = PermissionDefault.OP,
    async = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        sender.sendMessage("物品匹配缓存统计: ${ItemSettings.getCacheStats().toStr()}")
        if (MigrationConfig.enable) {
            sender.sendMessage("[模块]自动迁移-缓存统计: ${MigrationConfig.getCacheStat().toStr()}")
        }
        if (AutoUnBindConfig.enable) {
            sender.sendMessage("[模块]自动解绑-缓存统计: ${AutoUnBindConfig.getCacheStat().toStr()}")
        }
    }

    private fun CacheStats?.toStr(): String {
        if (this == null) return "无缓存"
        val stringBuilder = StringBuilder()
        stringBuilder.append("请求数: ")
        stringBuilder.append(requestCount())
        stringBuilder.append(" | 命中率: ")
        stringBuilder.append("%.2f".format(hitRate()))
        stringBuilder.append(" | 非命中平均耗时: ")
        stringBuilder.append("%.4f".format(averageLoadPenalty() / 1000000.0))
        stringBuilder.append(" 毫秒")
        return stringBuilder.toString()
    }
}