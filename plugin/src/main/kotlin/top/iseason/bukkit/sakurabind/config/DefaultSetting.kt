package top.iseason.bukkit.sakurabind.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack

object DefaultSetting : Setting(YamlConfiguration().apply { set("match.material", ".*") }) {
    override fun match(item: ItemStack): Boolean {
        return true
    }
}