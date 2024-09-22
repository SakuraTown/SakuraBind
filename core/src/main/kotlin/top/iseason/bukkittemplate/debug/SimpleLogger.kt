package top.iseason.bukkittemplate.debug

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

/**
 * 输出日志
 */
fun info(message: Any?) {
    Bukkit.getConsoleSender().sendColorMessage(message, SimpleLogger.prefix)
}

/**
 * 输出debug日志,只有当 SimpleLogger.isDebug 为true 时输出
 */
inline fun debug(message: () -> String?) {
    if (SimpleLogger.isDebug) {
        debug(message())
    }
}

fun debug(message: String?) = Bukkit.getConsoleSender()
    .sendColorMessage("${ChatColor.GRAY}[DEBUG] $message", SimpleLogger.prefix)

inline fun onDebug(action: () -> Unit) {
    if (SimpleLogger.isDebug)
        action()
}

/**
 * 输出警告日志
 */
fun warn(message: Any?) {
    BukkitTemplate.getPlugin().logger.warning(message.toString())
}

/**
 * 日志配置类
 */
object SimpleLogger {
    /**
     * 消息前缀
     */
    var prefix = "&a[&6${BukkitTemplate.getPlugin().description.name}&a] &f"

    /**
     * 是否是debug模式
     */
    var isDebug = false
}