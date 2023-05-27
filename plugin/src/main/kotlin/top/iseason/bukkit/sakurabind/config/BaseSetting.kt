package top.iseason.bukkit.sakurabind.config

import org.bukkit.command.CommandSender
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.matcher.BaseMatcher

/**
 * 绑定物品设置
 */
interface BaseSetting {
    /**
     * 设置的配置路径
     */
    val keyPath: String
    val matchers: List<BaseMatcher>
    /**
     * 匹配物品
     * @param item 待匹配的物品
     * @return true 匹配成功, false 匹配失败
     */
    fun match(item: ItemStack): Boolean

    /**
     * 匹配物品
     * @param item 待匹配的物品
     * @param sender 发送调试信息的人
     * @return true 匹配成功, false 匹配失败
     */
    fun match(item: ItemStack, sender: CommandSender?): Boolean
    fun getString(key: String): String
    fun getStringList(key: String): List<String>
    fun getInt(key: String): Int
    fun getLong(key: String): Long
    fun getDouble(key: String): Double

    /**
     * 是否禁止操作
     * @param key 获取的配置key
     * @param owner 物主的uuid
     * @param player 操作的玩家
     * @return true 表示禁止操作,false 表示允许操作
     */
    fun getBoolean(key: String, owner: String?, player: HumanEntity?): Boolean
}