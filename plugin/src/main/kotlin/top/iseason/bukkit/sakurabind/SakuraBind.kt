package top.iseason.bukkit.sakurabind

import fr.xephi.authme.events.LoginEvent
import org.bstats.bukkit.Metrics
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.player.PlayerLoginEvent
import top.iseason.bukkit.sakurabind.cache.CacheManager
import top.iseason.bukkit.sakurabind.command.mainCommand
import top.iseason.bukkit.sakurabind.config.*
import top.iseason.bukkit.sakurabind.dto.PlayerItems
import top.iseason.bukkit.sakurabind.hook.AuthMeHook
import top.iseason.bukkit.sakurabind.hook.PlaceHolderExpansion
import top.iseason.bukkit.sakurabind.hook.SakuraMailHook
import top.iseason.bukkit.sakurabind.listener.BindListener
import top.iseason.bukkit.sakurabind.listener.BindListener194
import top.iseason.bukkit.sakurabind.listener.BlockListener
import top.iseason.bukkit.sakurabind.listener.BlockListener1132
import top.iseason.bukkit.sakurabind.task.DelaySender
import top.iseason.bukkit.sakurabind.task.DropItemList
import top.iseason.bukkittemplate.KotlinPlugin
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.debug.warn
import top.iseason.bukkittemplate.hook.PlaceHolderHook
import top.iseason.bukkittemplate.utils.bukkit.EventUtils.listen
import top.iseason.bukkittemplate.utils.bukkit.EventUtils.register
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.toColor

object SakuraBind : KotlinPlugin() {
    lateinit var mainThread: Thread
    override fun onAsyncEnable() {
        Metrics(javaPlugin, 16968)
    }

    override fun onEnable() {
        mainThread = Thread.currentThread()
        SimpleLogger.prefix = "&a[&6${javaPlugin.description.name}&a]&r ".toColor()
        SimpleYAMLConfig.notifyMessage = "&6配置 &f%s &6已重载!"
        SakuraMailHook.checkHooked()
        if (PlaceHolderHook.hasHooked) {
            PlaceHolderExpansion.register()
        }
        try {
            mainCommand()
        } catch (e: Exception) {
            warn("命令注册异常,请重新启动......")
            e.printStackTrace()
        }
        GlobalSettings.load(false)
        Lang.load(false)
        DatabaseConfig.load(false)
        DatabaseConfig.initTables(PlayerItems)
        Config.load(false)
        ItemSettings.load(false)
        DefaultItemSetting
        BindListener.register()
        BindListener194.register()
        DropItemList.runTaskTimerAsynchronously(javaPlugin, 0, 1)
        AuthMeHook.checkHooked()
        SakuraBindAPI
        if (AuthMeHook.hasHooked) {
            listen<LoginEvent> {
                BindListener.onLogin(this.player)
            }
        } else {
            listen<PlayerLoginEvent> {
                BindListener.onLogin(this.player)
            }
        }
        if (Config.block_listener) {
            try {
                CacheManager
            } catch (e: Exception) {
                warn("缓存初始化异常!")
            }
            BlockListener.register()
            info("&a已启用方块监听器!")
            try {
                BlockPhysicsEvent::class.java.getMethod("getSourceBlock")
                BlockListener1132.register()
            } catch (_: Exception) {
            }
        }
        info("&a插件已启用!")
    }

    override fun onDisable() {
        try {
            DropItemList.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            CacheManager.save()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            DelaySender.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            ItemSettings.settingCache.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        info("&6插件已卸载")
    }
}