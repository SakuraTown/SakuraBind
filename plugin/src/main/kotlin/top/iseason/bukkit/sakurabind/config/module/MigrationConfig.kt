package top.iseason.bukkit.sakurabind.config.module

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheStats
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import top.iseason.bukkit.sakurabind.config.BaseSetting
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.task.MigrationScanner
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.debug.warn
import top.iseason.bukkittemplate.utils.other.submit
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

@FilePath("modules/migration.yml")
object MigrationConfig : SimpleYAMLConfig() {

    @Key
    @Comment(
        "",
        "数据迁移模块, 从其他lore类型的绑定插件中读取数据重新绑定",
        "启用将会定时扫描玩家背包和打开的容器"
    )
    var readme: MemorySection? = null

    @Key
    @Comment("", "是否启用")
    var enable = false

    @Key
    @Comment("", "扫描的周期, 单位tick")
    var scanning_period = 10L

    @Key
    @Comment("", "是否在主线程扫描, 如果你的服务端在扫描时报错请开启此选项")
    var is_sync_scanning = false

    @Key
    @Comment(
        "",
        "从物品中读取lore，将其转换为SakuraBind的物品, 正则表达式",
        "默认为: '\\[绑定]: (.*)' 可以匹配lore: '[绑定]: Iseason', 如果lore中有符号请在前面加上'\\' 请将代表玩家名的地方使用()包围",
        "在这测试你的正则表达式: https://www.bejson.com/othertools/regex/"
    )
    var match_lore = listOf("\\[绑定]: (.*)")
    var dataMigrationLore = listOf<Pattern>()

    @Key
    @Comment("", "lore中不是玩家名，而是玩家的uuid")
    var lore_is_uuid = false

    @Key
    @Comment(
        "",
        "如果检测不到这个玩家的数据, 强行绑定物品",
        "如果安装了AuthMe将尝试通过AuthMe获取UUID",
        "如果没有则使用该名字对应的离线UUID",
    )
    var force_bind = false

    @Key
    @Comment("", "删除匹配到的lore")
    var remove_lore = true

    @Key
    @Comment("", "不绑定，如果打开了，且上面也是打开的，那么就是删除旧的绑定lore的效果")
    var dont_bind = false

    @Key
    @Comment("", "匹配到的设置(settings.yml),留空或者填写错误都会使用常规匹配")
    var bind_setting = ""

    var bindSetting: BaseSetting? = null

    private var scannerTask: BukkitTask? = null

    @Key
    @Comment("", "物品读取设置的缓存个数,建议值是 玩家背包格子数量*玩家数量, 重启生效")
    var cache_size = 2000L

    var cache: Cache<ItemStack, Any>? = null

    fun getCacheStat(): CacheStats? = cache?.stats()

    override fun onLoaded(section: ConfigurationSection) {
        if (cache == null) {
            cache = CacheBuilder.newBuilder()
                .recordStats()
                .maximumSize(cache_size)
                .weakKeys()
                .expireAfterAccess(30000L, TimeUnit.MILLISECONDS)
                .build()
        } else {
            cache!!.invalidateAll()
            cache!!.cleanUp()
        }
        scannerTask?.cancel()
        scannerTask = null

        if (enable) {
            dataMigrationLore = match_lore.map { Pattern.compile(it) }
            info("&a数据迁移扫描任务已启动,周期: $scanning_period tick")
            val migrationScanner = MigrationScanner()
            scannerTask =
                submit(scanning_period, scanning_period, !is_sync_scanning, migrationScanner)
            bindSetting =
                if (bind_setting.isNotBlank()) ItemSettings.getSettingNullable(bind_setting) else null
            bindSetting
                ?: warn("config.yml 中 data-migration.setting  $bind_setting 不是一个有效的配置,将通过匹配器匹配")
        }
    }
}