package top.iseason.bukkit.sakurabind

import org.bukkit.event.block.BlockPhysicsEvent
import top.iseason.bukkit.sakurabind.cache.BlockCacheManager
import top.iseason.bukkit.sakurabind.command.mainCommand
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.dto.PlayerItems
import top.iseason.bukkit.sakurabind.hook.PlaceHolderHook
import top.iseason.bukkit.sakurabind.hook.SakuraMailHook
import top.iseason.bukkit.sakurabind.listener.BindListener
import top.iseason.bukkit.sakurabind.listener.BindListener194
import top.iseason.bukkit.sakurabind.listener.BlockListener
import top.iseason.bukkit.sakurabind.listener.BlockListener1132
import top.iseason.bukkittemplate.KotlinPlugin
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.debug.warn
import top.iseason.bukkittemplate.utils.bukkit.EventUtils.register
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.toColor

object SakuraBind : KotlinPlugin() {

    override fun onEnable() {
        SimpleLogger.prefix = "&a[&6${javaPlugin.description.name}&a]&r ".toColor()
        SimpleYAMLConfig.notifyMessage = "配置 %s 已重载!"
        SakuraMailHook.checkHooked()
        PlaceHolderHook.checkHooked()
        Lang.load(false)
        DatabaseConfig.load(false)
        DatabaseConfig.initTables(PlayerItems)
        Config.load(false)
        BindListener.register()
        BindListener194.register()
        if (Config.block__enable) {
            BlockCacheManager
            BlockListener.register()
            try {
                BlockPhysicsEvent::class.java.getMethod("getSourceBlock")
                BlockListener1132.register()
            } catch (_: Exception) {
            }
        }
        try {
            mainCommand()
        } catch (e: Exception) {
            warn("命令注册异常!")
            e.printStackTrace()
        }
        info("&a插件已启用")

    }

    override fun onDisable() {
        if (Config.block__enable)
            BlockCacheManager.save()
        info("&6插件已卸载")
    }
}