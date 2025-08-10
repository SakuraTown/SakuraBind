package top.iseason.bukkit.sakurabind.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.entity.HumanEntity
import org.bukkit.scheduler.BukkitTask
import top.iseason.bukkit.sakurabind.pickers.BasePicker
import top.iseason.bukkit.sakurabind.task.Scanner
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.debug.warn

@FilePath("config.yml")
object Config : SimpleYAMLConfig() {

    @Key
    @Comment("", "识别绑定玩家的NBT路径")
    var nbt_path_uuid = "sakura_bind_uuid"

    @Key
    @Comment("", "识别绑定Lore的NBT路径,数据是玩家旧的lore")
    var nbt_path_lore = "sakura_bind_lore"

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
    @Comment("", "是否开启暂存箱功能, 使用插件自带数据库功能进行物品存储，关闭时会禁用数据库功能, 重启生效")
    var send_back_database = true

    @Key
    @Comment(
        "",
        "丢失物品返还顺序, 满了才下一个顺序",
        "player: 玩家背包 仅玩家活着有效",
        "ender-chest: 末影箱 仅玩家在线有效",
        "database: 插件自带暂存箱",
        "GlobalMarketPlus: GlobalMarketPlus插件的邮箱",
        "SweetMail: SweetMail 邮件",
    )
    var send_back_queue = listOf("player", "ender-chest", "database")


    @Key("global-market-plus")
    @Comment("", "物品送回 GlobalMarketPlus 的设置")
    var market_sender: MemorySection? = null

    @Key("global-market-plus.name")
    @Comment("", "邮件发送者名称")
    var market_sender_name = "绑定系统"

    @Key("global-market-plus.expire")
    @Comment("", "邮件有效期, 单位 秒, -1 表示不过期")
    var market_sender_time = -1L

    @Key("sweet-mail")
    @Comment("", "物品送回 SweetMail 的设置")
    var sweetMailSection: MemorySection? = null

    @Key("sweet-mail.sender")
    @Comment("", "邮件发送者名称")
    var sweetMailSender = "绑定系统"

    @Key("sweet-mail.icon")
    @Comment("", "邮件图标, SweetMail的格式")
    var sweetMailIcon = "BOOK"

    @Key("sweet-mail.title")
    @Comment("", "邮件标题")
    var sweetMailTitle = "绑定系统"

    @Key("sweet-mail.content")
    @Comment("", "邮件内容,一行一页\\n换行")
    var sweetMailContent = listOf("以下是你丢失的绑定物品\n请查收")

    @Key("sweet-mail.expire")
    @Comment("", "邮件有效时间(秒) -1 无限")
    var sweetMailExpire = -1L

    @Key
    @Comment(
        "", "启用配置权限检查, 在获取绑定设置前优先从权限中读取。",
        "绑定全局设置权限 `sakurabind.settings.{键名}.true|false` 不支持`键名@`的形式",
        "绑定设置权限 `sakurabind.setting.{设置名}.{键名}.true|false` 设置名是 `settings.yml` 匹配键,覆盖全局权限 不支持`键名@`"
    )
    var enable_setting_permission_check = false

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
    @Comment("", "插件加载之后多少秒开始检查主线程卡顿")
    var thread_dump_protection__delay = 30

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
    @Comment("", "物品读取设置的缓存时间(毫秒),建议值大于 扫描器时间")
    var setting_cache_time = 3500L

    override fun onLoaded(section: ConfigurationSection) {
        setupScanner()
        BasePicker.configPickers.clear()
        val allPickers = BasePicker.allPickers
        for (pickerName in send_back_queue) {
            val basePicker = allPickers[pickerName.lowercase()]
            if (basePicker == null) {
                warn("config.yml 中 send-back-queue 的  $pickerName 不是一个有效的配置， 已忽略")
                continue
            }
            BasePicker.configPickers.add(basePicker)
        }
    }

    fun setupScanner() {
        task?.cancel()
        task = null
        if (scanner_period > 0L) {
            info("&a定时扫描任务已启动,周期: $scanner_period tick")
            task = Scanner().runTaskTimerAsynchronously(BukkitTemplate.getPlugin(), scanner_period, scanner_period)
        }
    }

    /**
     * 检查是否不检查
     */
    fun checkByPass(player: HumanEntity): Boolean {
        return player.hasPermission("sakurabind.bypass.all")
    }

}