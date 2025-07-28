package top.iseason.bukkit.sakurabind.config

import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.command.DebugCommand
import top.iseason.bukkit.sakurabind.config.matcher.BaseMatcher
import top.iseason.bukkit.sakurabind.config.matcher.MatcherManager
import top.iseason.bukkittemplate.debug.info

open class ItemSetting(override val keyPath: String, protected val section: ConfigurationSection) : BaseSetting {

    var setting: ConfigurationSection
    final override val matchers: List<BaseMatcher>

    init {
        val matcherSection = section.getConfigurationSection("match") ?: section.createSection("match")
        matchers = MatcherManager.parseSection(matcherSection)
        setting = section.getConfigurationSection("settings") ?: section.createSection("settings")
    }

    override fun match(item: ItemStack): Boolean {
        return match(item, null)
    }

    override fun match(item: ItemStack, sender: CommandSender?): Boolean {
        if (matchers.isEmpty()) return false
        if (sender == null) return matchers.all { it.tryMatch(item) }
        else matchers.forEach { it.onDebug(item, sender) }
        return matchers.all { it.tryMatch(item) }
    }

    override fun getString(key: String): String {
        return setting.getString(key, GlobalSettings.config.getString(key)) ?: ""
    }

    override fun getStringList(key: String): List<String> {
        return if (setting.contains(key))
            setting.getStringList(key)
        else GlobalSettings.config.getStringList(key)
    }

    override fun getIntList(key: String): List<Int> {
        return if (setting.contains(key))
            setting.getIntegerList(key)
        else GlobalSettings.config.getIntegerList(key)
    }

    override fun getInt(key: String): Int = setting.getInt(key, GlobalSettings.config.getInt(key))

    override fun getLong(key: String): Long = setting.getLong(key, GlobalSettings.config.getLong(key))
    override fun getDouble(key: String): Double = setting.getDouble(key, GlobalSettings.config.getDouble(key))

    override fun getBoolean(key: String, owner: String?, player: HumanEntity?): Boolean {
        //权限检查
        val result = run {
            val checkPermission = Config.enable_setting_permission_check && player != null
            if (checkPermission) {
                if (player.hasPermission("sakurabind.setting.$keyPath.$key.true")) {
                    return@run true
                } else if (player.hasPermission("sakurabind.setting.$keyPath.$key.false")) {
                    return@run false
                } else if (player.hasPermission("sakurabind.settings.$key.true")) {
                    return@run true
                } else if (player.hasPermission("sakurabind.settings.$key.false")) {
                    return@run false
                }
            }
            //是物主或者拥有物主的权限
            var isOwner = false
            if (owner != null && player != null) {
                isOwner =
                    (owner == player.uniqueId.toString()) ||
                            (checkPermission && player.hasPermission("sakurabind.bypass.$owner"))
                if (isOwner && setting.contains("$key@")) {
                    return@run !setting.getBoolean("$key@")
                }
            }
            if (setting.contains(key)) return@run setting.getBoolean(key)

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