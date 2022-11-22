package top.iseason.bukkit.sakurabind

import org.bukkit.event.block.BlockPhysicsEvent
import top.iseason.bukkit.sakurabind.listener.BindListener
import top.iseason.bukkit.sakurabind.listener.BindListener1132
import top.iseason.bukkit.sakurabind.listener.BindListener194
import top.iseason.bukkittemplate.KotlinPlugin
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.utils.bukkit.EventUtils.register
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.toColor

object SakuraBind : KotlinPlugin() {

    override fun onAsyncLoad() {
    }

    override fun onAsyncEnable() {
        SimpleLogger.prefix = "&a[&6${javaPlugin.description.name}&a]&r ".toColor()
        Config.load(false)
        BindListener.register()
        BindListener194.register()
        try {
            BlockPhysicsEvent::class.java.getMethod("getSourceBlock")
            BindListener1132.register()
        } catch (_: Exception) {
        }
        mainCommand()
        BlockCacheManager
        info("&a插件已启用")
    }

    override fun onDisable() {
        BlockCacheManager.save()
        info("&6插件已卸载")
    }
}