package top.iseason.bukkit.sakurabind.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.command.DebugCommand
import top.iseason.bukkittemplate.debug.info

object DefaultItemSetting : ItemSetting("global-setting", YamlConfiguration()) {

    override fun match(item: ItemStack): Boolean {
        return true
    }

    override fun getBoolean(key: String, owner: String?, player: HumanEntity?): Boolean {
        //权限检查
        val result = run {
            val checkPermission = Config.enable_setting_permission_check && player != null
            if (checkPermission) {
                if (player.hasPermission("sakurabind.settings.$key.true")) {
                    return@run true
                } else if (player.hasPermission("sakurabind.settings.$key.false")) {
                    return@run false
                }
            }
            var isOwner = false
            if (owner != null) {
                isOwner =
                    (owner == player?.uniqueId.toString()) ||
                            (checkPermission && player.hasPermission("sakurabind.bypass.$owner"))
            }
            val globalConfig = GlobalSettings.config
            if (globalConfig.contains("$key@")) {
                return@run if (isOwner) !globalConfig.getBoolean("$key@")
                else globalConfig.getBoolean("$key@")
            }
            return@run globalConfig.getBoolean(key)
        }
        if (player != null && DebugCommand.debugPlayer.contains(player.uniqueId)) {
            info("检查玩家 ${player.name} 配置: $keyPath 配置键: $key 结果: $result")
        }
        return result
    }

    override fun clone(): BaseSetting {
        val yamlConfiguration = YamlConfiguration()
        yamlConfiguration.set("clone", section)
        val reader = yamlConfiguration.saveToString().reader()
        val newSection = YamlConfiguration.loadConfiguration(reader).getConfigurationSection("clone")!!
        return ItemSetting(keyPath, newSection)
    }
}