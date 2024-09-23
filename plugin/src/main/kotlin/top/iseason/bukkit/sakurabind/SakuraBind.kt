package top.iseason.bukkit.sakurabind


import de.tr7zw.nbtapi.utils.MinecraftVersion
import org.bstats.bukkit.Metrics
import top.iseason.bukkit.sakurabind.cache.BlockCache
import top.iseason.bukkit.sakurabind.cache.CacheManager
import top.iseason.bukkit.sakurabind.cache.EntityCache
import top.iseason.bukkit.sakurabind.cache.FallingBlockCache
import top.iseason.bukkit.sakurabind.command.*
import top.iseason.bukkit.sakurabind.config.*
import top.iseason.bukkit.sakurabind.config.matcher.MatcherManager
import top.iseason.bukkit.sakurabind.dto.BindLogs
import top.iseason.bukkit.sakurabind.dto.PlayerItems
import top.iseason.bukkit.sakurabind.dto.SendBackLogs
import top.iseason.bukkit.sakurabind.dto.UniqueLogs
import top.iseason.bukkit.sakurabind.hook.*
import top.iseason.bukkit.sakurabind.listener.*
import top.iseason.bukkit.sakurabind.module.BindItem
import top.iseason.bukkit.sakurabind.module.UniqueItem
import top.iseason.bukkit.sakurabind.pickers.BasePicker
import top.iseason.bukkit.sakurabind.task.DropItemList
import top.iseason.bukkit.sakurabind.task.EntityRemoveQueue
import top.iseason.bukkittemplate.BukkitPlugin
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.command.CommandHandler
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.debug.warn
import top.iseason.bukkittemplate.hook.PlaceHolderHook
import top.iseason.bukkittemplate.utils.bukkit.EventUtils.registerListener
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.toColor

object SakuraBind : BukkitPlugin {

    override fun onLoad() {
        Metrics(javaPlugin, 16968)
    }

    override fun onEnable() {
        checkHooks()
        BukkitTemplate.getPlugin().saveResource("placeholders.txt", true)
        SimpleLogger.prefix = "&a[&6${javaPlugin.description.name}&a]&r ".toColor()
        SimpleYAMLConfig.notifyMessage = "&6配置文件 &f%s &6已重载!"
        try {
            initCommands()
        } catch (e: Exception) {
            e.printStackTrace()
            warn("命令注册异常,请重启以恢复命令...")
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
    fun initConfig() {
        info("&6配置初始化中...")
        GlobalSettings.load(false)
        Lang.load(false)
        ItemSettings.load(false)
        BasePicker.init()
        Config.load(false)
        DatabaseConfig.load(false)
        if (Config.send_back_database)
            DatabaseConfig.initTables(PlayerItems, BindLogs, SendBackLogs, UniqueLogs)
        else
            DatabaseConfig.initTables(BindLogs, SendBackLogs, UniqueLogs)
        BindLogger.load(false)
        SendBackLogger.load(false)
        UniqueItemConfig.load(false)
        BindItemConfig.load(false)
        info("&a配置初始化完毕!")
        Config.setupScanner()
    }

    /**
     * 检查插件钩子
     */
    private fun checkHooks() {
        PlaceHolderHook.checkHooked()
        SakuraMailHook.checkHooked()
        AuthMeHook.checkHooked()
        MMOItemsHook.checkHooked()
        ItemsAdderHook.checkHooked()
        OraxenHook.checkHooked()
        BanItemHook.checkHooked()
        GermHook.checkHooked()
        McMMoHook.checkHooked()
        GlobalMarketPlusHook.checkHooked()
        SweetMailHook.checkHooked()
        PlayerDataSQLHook.checkHooked()
        HuskSyncHook.checkHooked()
        InvSyncHook.checkHooked()
        if (PlaceHolderHook.hasHooked) PlaceHolderExpansion.register()
        if (MMOItemsHook.hasHooked) {
            MatcherManager.addMatcher(MMOItemsMatcher())
            MMOItemsHook.registerListener()
        }
        if (ItemsAdderHook.hasHooked) MatcherManager.addMatcher(ItemsAdderMatcher())
        if (OraxenHook.hasHooked) MatcherManager.addMatcher(OraxenMatcher())
        if (McMMoHook.hasHooked) McMMoHook.registerListener()
        if (PlayerDataSQLHook.hasHooked) PlayerDataSQLHook.registerListener()
        if (HuskSyncHook.hasHooked) HuskSyncHook.registerListener()
        if (InvSyncHook.hasHooked) InvSyncHook.registerListener()

    }

    /**
     * 开启任务
     */
    private fun initTasks() {
        DropItemList.runTaskTimerAsynchronously(javaPlugin, 0, 1)
        EntityRemoveQueue.runTaskTimer(javaPlugin, 0, 1)
    }

    /**
     * 注册监听器
     */
    private fun initListeners() {
        ItemListener.registerListener()
        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_8_R3)) {
            ItemListenerMC108.registerListener()
        }

        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_9_R1)) {
            ItemListenerMC119.registerListener()
        }

        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_16_R1)) {
            ItemListenerMC116.registerListener()
        }

        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_14_R1)) {
            PickupItemListener.registerListener()
        } else {
            LegacyPickupItemListener.registerListener()
        }

        SelectListener.registerListener()
        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_8_R3)) {
            SelectListenerMC108.registerListener()
        }

        if (AuthMeHook.hasHooked) {
            LoginAuthMeListener.registerListener()
        } else {
            LoginListener.registerListener()
        }
        if (Config.block_listener) {
            BlockListener.registerListener()
            if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_8_R3)) {
                BlockListenerMC108.registerListener()
            }
            if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_14_R1)) {
                BlockListenerMC114.registerListener()
            }
            info("&a已启用方块监听器!")
        }
        if (Config.entity_listener) {
            CacheManager.register(EntityCache)
            EntityListener.registerListener()
            if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_8_R3)) {
                EntityListenerMC108.registerListener()
            }
            info("&a已启用实体监听器!")
        }

        BindActionListener.registerListener()
        if (UniqueItemConfig.enable) {
            UniqueItem.registerListener()
        }
        if (BindItemConfig.enable) {
            BindItem.registerListener()
        }
        if (PaperListener.isPaper()) {
            info("&a当前为 Paper 或下游服务端,已开启装备穿戴检测功能")
            PaperListener.registerListener()
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
            addSubNode(SelectCommand)
            addSubNode(DebugCommand)
            addSubNode(OpenLostCommand)
            addSubNode(ReloadCommand)
            addSubNode(CallbackCommand)
            addSubNode(SuperCallbackCommand)
            addSubNode(TestCommand)
            addSubNode(NBTCommand)
            addSubNode(TestCacheCommand)
        }
        TestCommand.addSubNode(TestMatchCommand)
        TestCommand.addSubNode(TestTryMatchCommand)

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
//        try {
////            BlockCache.tempBlockCache.close()
////            ItemSettings.settingCache2.close()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
        try {
            CacheManager.save()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        info("&a插件已注销.")
    }
}