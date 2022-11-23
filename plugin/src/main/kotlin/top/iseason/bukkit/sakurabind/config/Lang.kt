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
    var block___deny_break = "&6你不能破坏这个方块,这个方块属于 &b{0}"
    var block___deny_interact = "&6你没有这个方块的使用权限,这个方块属于 &b{0}"

    override fun onLoaded(section: ConfigurationSection) {
        MessageUtils.defaultPrefix = prefix
    }
}