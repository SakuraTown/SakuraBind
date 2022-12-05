package top.iseason.bukkit.sakurabind.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack

object DefaultSetting : Setting("global-setting", YamlConfiguration().apply {
    set("match.material", ".*")
    set("settings.a", "")
}) {
    override fun match(item: ItemStack): Boolean {
        return true
    }
}