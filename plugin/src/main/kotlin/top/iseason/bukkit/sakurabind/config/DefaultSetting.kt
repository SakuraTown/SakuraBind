package top.iseason.bukkit.sakurabind.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.ItemStack

object DefaultSetting : Setting("global-setting", YamlConfiguration().apply {
    set("match.material", ".*")
    set("settings.a", "")
}) {
    override fun match(item: ItemStack): Boolean {
        return true
    }

    override fun getBoolean(key: String, owner: String?, player: HumanEntity?): Boolean {
        //权限检查
        if (player != null) {
            if (player.hasPermission("sakurabind.settings.$key.true")) {
                return true
            } else if (player.hasPermission("sakurabind.settings.$key.false")) {
                return false
            }
        }
        var isOwner = false
        if (owner != null) {
            isOwner =
                (owner == player?.uniqueId.toString()) || player?.hasPermission("sakurabind.bypass.$owner") == true
        }
        if (GlobalSettings.config.contains("$key@")) {
            return if (isOwner)
                !GlobalSettings.config.getBoolean("$key@")
            else
                GlobalSettings.config.getBoolean("$key@")
        }
        return GlobalSettings.config.getBoolean(key)
    }
}