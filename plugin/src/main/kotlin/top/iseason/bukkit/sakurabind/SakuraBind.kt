package top.iseason.bukkit.sakurabind

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bstats.bukkit.Metrics
import org.bukkit.event.block.BlockPhysicsEvent
import top.iseason.bukkit.sakurabind.cache.BlockCache
import top.iseason.bukkit.sakurabind.cache.CacheManager
import top.iseason.bukkit.sakurabind.cache.EntityCache
import top.iseason.bukkit.sakurabind.cache.FallingBlockCache
import top.iseason.bukkit.sakurabind.command.*
import top.iseason.bukkit.sakurabind.config.*
import top.iseason.bukkit.sakurabind.dto.PlayerItems
import top.iseason.bukkit.sakurabind.hook.AuthMeHook
import top.iseason.bukkit.sakurabind.hook.PlaceHolderExpansion
import top.iseason.bukkit.sakurabind.hook.SakuraMailHook
import top.iseason.bukkit.sakurabind.listener.*
import top.iseason.bukkit.sakurabind.task.DelaySender
import top.iseason.bukkit.sakurabind.task.DropItemList
import top.iseason.bukkittemplate.KotlinPlugin
import top.iseason.bukkittemplate.command.CommandHandler
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.debug.warn
import top.iseason.bukkittemplate.hook.PlaceHolderHook
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
        checkHooks()
        try {
            initCommands()
        } catch (e: Exception) {
            e.printStackTrace()
            warn("命令注册异常,请重新启动......")
        }
        try {
            initConfig()
        } catch (e: Exception) {
            e.printStackTrace()
            warn("配置或数据库初始化异常!")
        }
        initCaches()
        initListeners()
        initTasks()
        info("&a插件已启用!")
    }

    /**
     * 初始化配置与数据库
     */
    @Throws(Exception::class)
    private fun initConfig() {
        GlobalSettings.load(false)
        Lang.load(false)
        ItemSettings.load(false)
        DatabaseConfig.load(false)
        DatabaseConfig.initTables(PlayerItems)
        Config.load(false)
        info("&a配置已初始化!")
    }

    /**
     * 检查插件钩子
     */
    private fun checkHooks() {
        SakuraMailHook.checkHooked()
        AuthMeHook.checkHooked()
        if (PlaceHolderHook.hasHooked) {
            PlaceHolderExpansion.register()
        }

    }

    /**
     * 开启任务
     */
    private fun initTasks() {
        DropItemList.runTaskTimerAsynchronously(javaPlugin, 0, 1)
    }

    /**
     * 注册监听器
     */
    private fun initListeners() {
        BindListener.register()
        if (NBTEditor.getMinecraftVersion().greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_9)) {
            BindListener194.register()
        }
        if (AuthMeHook.hasHooked) {
            LoginAuthMeListener.register()
        } else {
            LoginListener.register()
        }
        if (Config.block_listener) {
            BlockListener.register()
            try {
                BlockPhysicsEvent::class.java.getMethod("getSourceBlock")
                BlockListener1132.register()
            } catch (_: Exception) {
            }
            info("&a已启用方块监听器!")
        }
        if (Config.entity_listener) {
            CacheManager.register(EntityCache)
            EntityListener.register()
            info("&a已启用实体监听器!")
        }
    }

    /**
     * 初始化缓存
     */
    private fun initCaches() {
        if (Config.block_listener) {
            CacheManager.register(BlockCache)
            CacheManager.register(FallingBlockCache)
        }
        if (Config.entity_listener) {
            CacheManager.register(EntityCache)
        }
        try {
            CacheManager.build()
        } catch (e: Exception) {
            e.printStackTrace()
            warn("缓存初始化异常,请重启!")
        }
        info("&a缓存初始化成功!")
    }

    /**
     * 注册命令
     */
    private fun initCommands() {
        CommandNode.usageFooter = "&7所有命令加上 '-silent' 参数可以不显示提示消息\n "
        with(RootCommand) {
            addSubNode(BindCommand)
            addSubNode(BindToCommand)
            addSubNode(BindAllCommand)
            addSubNode(UnBindCommand)
            addSubNode(UnBindAllCommand)
            addSubNode(GetLostCommand)
            addSubNode(AutoBindCommand)
            addSubNode(DebugCommand)
        }
        CommandHandler.register(RootCommand)
        CommandHandler.updateCommands()
    }

    override fun onDisable() {
        info("&6插件注销中...")
        try {
            DropItemList.cancel()
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
        try {
            CacheManager.save()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        info("&a插件已注销")
    }
}