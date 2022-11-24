package top.iseason.bukkit.sakurabind.config

import org.bukkit.configuration.ConfigurationSection
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils

@Key
@FilePath("lang.yml")
object Lang : SimpleYAMLConfig() {
    var prefix = "&a[&6${BukkitTemplate.getPlugin().description.name}&a] &f"
    var send_back_all = "&7你的遗失物品已全部放入你的背包"
    var send_back = "&7你的部分遗失物品已放入你的背包, 剩下的已发往邮箱"

    var item__deny_command = "&6你不能拿着此物品输入该命令!"
    var item__deny_drop = "&6该物品不能丢出!"
    var block__deny_break = "&6你不能破坏这个方块,这个方块属于 &b{0}"
    var block__deny_interact = "&6你没有这个方块的使用权限,这个方块属于 &b{0}"

    var get_item = "&a你领取了 &b{0} &a个遗失物品,请腾出空间领取剩余物品!"
    var get_empty = "&6没有遗失物品!"
    var get_full = "&背包空间不足!"
    var get_all = "&a你领取了所有的遗失物品!"

    var command_coolDown = "&6你输入得太快了!"
    var has_lost_item = "&a你有遗失的物品,请输入 &6'/sakurabind get' &a领取"
    override fun onLoaded(section: ConfigurationSection) {
        MessageUtils.defaultPrefix = prefix
    }
}