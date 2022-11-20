package top.iseason.bukkit.sakurabind

import top.iseason.bukkittemplate.KotlinPlugin
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.utils.bukkit.EventUtils.register
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.toColor

object SakuraBind : KotlinPlugin() {

    override fun onAsyncLoad() {
    }

    override fun onEnable() {
        SimpleLogger.prefix = "&a[&6${javaPlugin.description.name}&a]&r ".toColor()
    }

    override fun onAsyncEnable() {
        Config.load(false)
        BindListener.register()
        mainCommand()
        info("&a插件已启用")
    }

    override fun onDisable() {
        info("&6插件已卸载")
    }
}