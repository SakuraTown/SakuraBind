package top.iseason.bukkit.sakurabind.config

import com.google.common.cache.Cache
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import top.iseason.bukkit.sakurabind.hook.SakuraMailHook
import top.iseason.bukkit.sakurabind.task.MigrationScanner
import top.iseason.bukkit.sakurabind.task.Scanner
import top.iseason.bukkit.sakuramail.config.SystemMailsYml
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.debug.warn
import java.util.regex.Pattern

@FilePath("config.yml")
object Config : SimpleYAMLConfig() {

    @Key
    @Comment("", "识别绑定玩家的NBT路径(由tag开始)，'.' 为路径分隔符,数据是玩家uuid")
    var nbt_path_uuid = "sakura_bind_uuid"
    var nbtPathUuid = arrayOf<String>()

    @Key
    @Comment("", "识别绑定Lore的NBT路径(由tag开始)，'.' 为路径分隔符,数据是玩家旧的lore")
    var nbt_path_lore = "sakura_bind_lore"
    var nbtPathLore = arrayOf<String>()

    @Key
    @Comment("", "遗失物品使用 SakuraMail 发送而不是暂存箱")
    var sakuraMail_hook = false

    @Key
    @Comment(
        "",
        "如果要发送丢失物品邮件",
        "填入SakuraMail的邮件id，丢失物品将会替换邮件的物品",
        "按顺序替换，不够的将会删除, 多余的将会在另外的邮件里"
    )
    var mailId = ""

    @Key
    @Comment("", "登入时如果暂存箱有物品则提醒，此为延迟，单位tick, 设置小于0以关闭提示")
    var login_message_delay = 100L

    @Key
    @Comment("", "方块物品检测开关，需要重启生效。打开才能支持方块物品，同时性能损耗也会增加")
    var block_listener = false

    @Key
    @Comment("", "实体检测开关，需要重启生效。打开才能支持实体绑定，同时性能损耗也会增加")
    var entity_listener = false

    @Key
    @Comment(
        "",
        "定时扫描所有玩家背包, 此为扫描周期,单位tick，0表示关闭",
        "此项关闭将影响 scanner开头的设置"
    )
    var scanner_period = 60L

    @Key
    @Comment("", "玩家禁用消息的冷却时间, 单位毫秒")
    var message_coolDown = 1000L

    @Key
    @Comment("", "丢失物品返还玩家时如果玩家在线且背包满了优先进入玩家末影箱，末影箱满了再进入暂存箱")
    var ender_chest_cache = false

    @Key
    @Comment(
        "", "启用配置权限检查, 在获取绑定设置前优先从权限中读取。",
        "绑定全局设置权限 `sakurabind.settings.{键名}.true|false` 不支持`键名@`的形式",
        "绑定设置权限 `sakurabind.setting.{设置名}.{键名}.true|false` 设置名是 `settings.yml` 匹配键,覆盖全局权限 不支持`键名@`"
    )
    var enable_setting_permission_check = true

    private var task: BukkitTask? = null

    @Key
    @Comment("", "识别到此NBT就自动绑定物主")
    var auto_bind_nbt = "sakura_auto_bind"

    @Key
    @Comment("", "选择命令的最大超时时间,单位毫秒")
    var command_select_timeout = 30000L

    @Key
    @Comment("", "允许打开空的暂存箱")
    var command_openLost_open_empty = false

    @Key
    @Comment("", "暂存箱标题，支持placeholder")
    var temp_chest_title = "&a{0} 的暂存箱"

    @Key
    @Comment(
        "",
        "当玩家退出登陆时优化暂存箱数据库(整理分散的物品数据)",
        "全服5分钟内只能优化一次 每玩家2小时冷却"
    )
    var temp_chest_purge_on_quit = false

    @Key
    @Comment("", "当主线程卡住时可能会导致放下的方块丢失绑定, 重启生效")
    var thread_dump_protection: MemorySection? = null

    @Key
    @Comment("", "是否启用主线程卡顿监控")
    var thread_dump_protection__enable = false

    @Key
    @Comment("", "主线程卡顿判断时间，单位秒")
    var thread_dump_protection__timeout = 50

    @Key
    @Comment("", "当主线程卡顿时, 注销插件")
    var thread_dump_protection__disable_plugin = false

    @Key
    @Comment("", "当主线程卡顿时, 关闭服务器")
    var thread_dump_protection__stop_server = false

    @Key
    @Comment("", "物品读取设置的缓存个数,建议值是 玩家背包格子数量*玩家数量")
    var setting_cache_size = 2000L

    @Key
    @Comment(
        "",
        "数据迁移设置, 从其他lore类型的绑定插件中读取数据重新绑定",
        "启用将会定时扫描玩家背包和打开的容器"
    )
    var data_migration: MemorySection? = null

    @Key
    @Comment("", "是否启用")
    var data_migration__enable = false

    @Key
    @Comment("", "扫描的周期, 单位tick")
    var data_migration__period = 10L

    @Key
    @Comment(
        "",
        "从物品中读取lore，将其转换为SakuraBind的物品, 正则表达式",
        "默认为: '\\[绑定]: (.*)' 可以匹配lore: '[绑定]: Iseason', 如果lore中有符号请在前面加上'\\' 请将代表玩家名的地方使用()包围",
        "在这测试你的正则表达式: https://www.bejson.com/othertools/regex/"
    )
    var data_migration__lore = listOf("\\[绑定]: (.*)")
    var dataMigrationLore = listOf<Pattern>()

    @Key
    @Comment("", "lore中不是玩家名，而是玩家的uuid")
    var data_migration__is_uuid = false

    @Key
    @Comment("", "如果检测不到这个玩家的数据, 强行将物品绑定至该名称对应的UUID里")
    var data_migration__force_bind = false

    @Key
    @Comment("", "删除匹配到的lore")
    var data_migration__remove_lore = true

    @Key
    @Comment("", "不绑定，如果打开了，且上面也是打开的，那么就是删除旧的绑定lore的效果")
    var data_migration__dont_bind = false

    @Key
    @Comment("", "匹配到的设置,留空或者填写错误都会使用常规匹配")
    var data_migration__setting = ""
    var dataMigrationSetting: BaseSetting? = null

    private var dataMigrationTask: BukkitTask? = null

    private var dataMigrationCache: Cache<ItemStack, Any>? = null

    fun getDataMigrationCacheStat() = dataMigrationCache?.stats()

    override fun onLoaded(section: ConfigurationSection) {
        nbtPathUuid = nbt_path_uuid.split('.').toTypedArray()
        nbtPathLore = nbt_path_lore.split('.').toTypedArray()

        if (SakuraMailHook.hasHooked && mailId.isNotBlank()) {
            SystemMailsYml.getMailYml(mailId) ?: info("&c邮件&7 $mailId &c不存在!")
        }
        task?.cancel()
        task = null
        dataMigrationTask?.cancel()
        dataMigrationTask = null
        dataMigrationCache = null
        if (scanner_period > 0L && (DatabaseConfig.isConnected || (SakuraMailHook.hasHooked && sakuraMail_hook))) {
            info("&a定时扫描任务已启动,周期: $scanner_period tick")
            task = Scanner().runTaskTimer(BukkitTemplate.getPlugin(), scanner_period, scanner_period)
        }
        if (data_migration__enable) {
            dataMigrationLore = data_migration__lore.map { Pattern.compile(it) }
            info("&a数据迁移扫描任务已启动,周期: $data_migration__period tick")
            val migrationScanner = MigrationScanner()
            dataMigrationTask = migrationScanner.runTaskTimerAsynchronously(
                BukkitTemplate.getPlugin(),
                data_migration__period,
                data_migration__period
            )
            dataMigrationCache = migrationScanner.cache
            dataMigrationSetting =
                if (data_migration__setting.isNotBlank()) ItemSettings.getSettingNullable(data_migration__setting) else null
            dataMigrationSetting
                ?: warn("config.yml 中 data-migration.setting  $data_migration__setting 不是一个有效的配置,将通过匹配器匹配")

        }
    }

    /**
     * 检查是否不检查
     */
    fun checkByPass(player: HumanEntity): Boolean {
        return player.isOp || player.hasPermission("sakurabind.bypass.all")
    }

}