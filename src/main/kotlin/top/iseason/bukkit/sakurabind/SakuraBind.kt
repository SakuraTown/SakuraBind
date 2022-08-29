package top.iseason.bukkit.sakurabind

import top.iseason.bukkit.bukkittemplate.KotlinPlugin
import top.iseason.bukkit.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkit.bukkittemplate.debug.info
import top.iseason.bukkit.bukkittemplate.utils.toColor

object SakuraBind : KotlinPlugin() {

    override fun onAsyncLoad() {
    }

    override fun onEnable() {
        SimpleLogger.prefix = "&a[&6${javaPlugin.description.name}&a]&r ".toColor()
    }

    override fun onAsyncEnable() {
        Config.load(false)
        registerListeners(BindListener)
        command()
        info("&a插件已启用!")
    }

    override fun onDisable() {
        info("&6插件已卸载!")
    }


}