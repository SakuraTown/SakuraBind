package top.iseason.bukkit.sakurabind.config.matcher

import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack

/**
 * 使用原型模式
 */
abstract class BaseMatcher {
    /**
     * 该配置接受的键
     */
    abstract fun getKeys(): Array<String>

    /**
     * 从配置中读取数据
     */
    abstract fun fromSetting(key: String, any: Any): BaseMatcher?

    /**
     * 尝试匹配某个物品
     */
    abstract fun tryMatch(item: ItemStack): Boolean

    /**
     * Debug
     */
    abstract fun onDebug(item: ItemStack, debugHolder: CommandSender)

    open fun onBind(item: ItemStack) {}
}