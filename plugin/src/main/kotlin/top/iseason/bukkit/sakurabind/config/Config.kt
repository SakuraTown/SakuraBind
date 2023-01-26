package top.iseason.bukkit.sakurabind.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.HumanEntity
import org.bukkit.scheduler.BukkitTask
import top.iseason.bukkit.sakurabind.hook.SakuraMailHook
import top.iseason.bukkit.sakurabind.task.Scanner
import top.iseason.bukkit.sakuramail.config.SystemMailsYml
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.info
import java.io.File
import java.util.*

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
        "定时扫描所有玩家背包(materials不为空才会开启), 此为扫描周期,单位tick，0表示关闭",
        "此项关闭将影响 scanner开头的设置"
    )
    var scanner_period = 60L

    @Key
    @Comment("", "玩家禁用消息的冷却时间, 单位毫秒")
    var message_coolDown = 1000L

    @Key
    @Comment("", "丢失物品返还玩家时如果玩家在线且背包满了优先进入玩家末影箱，末影箱满了再进入暂存箱")
    var ender_chest_cache = false

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
    @Comment("", "日志系统，用于记录绑定信息")
    var logger = ""

    @Key
    @Comment("", "开关")
    var logger__enable = false

    @Key
    @Comment("", "将绑定信息输出到控制台")
    var logger__console = false

    @Key
    @Comment("", "将绑定信息输出到数据库中，可跨服")
    var logger__database = false

    @Key
    @Comment("", "将绑定信息输出到独立的文件中")
    var logger__file = true

    @Key
    @Comment("", "独立的文件的位置,修改需重启生效")
    var logger__file_path = File(BukkitTemplate.getPlugin().dataFolder, "log${File.separatorChar}bind-log").toString()

    @Key
    @Comment("", "独立的文件的最大数量，每个1M,修改需重启生效")
    var logger__file_max_count = 10


    @Key
    @Comment("", "日志显示格式")
    var logger__format = "物主: {0} 行为: {1} 配置: {2} 信息: {3}"

    @Key
    @Comment("", "物品显示格式, 替换logger.format的 {4}", "占位符分别为 类型、名字、数量")
    var logger__format_item = "物品 {0} {1} x {2}"

    @Key
    @Comment("", "物品显示格式, 替换logger.format的 {4}", "占位符分别为 类型、位置")
    var logger__format_block = "方块: {0} {1}"

    @Key
    @Comment("", "物品显示格式, 替换logger.format的 {4}", "占位符分别为 类型、名字、UUID、位置")
    var logger__format_entity = "实体: {0} {1} {2} {3} {4}"

    override fun onLoaded(section: ConfigurationSection) {
        nbtPathUuid = nbt_path_uuid.split('.').toTypedArray()
        nbtPathLore = nbt_path_lore.split('.').toTypedArray()

        if (SakuraMailHook.hasHooked) {
            SystemMailsYml.getMailYml(mailId) ?: info("&c邮件&7 $mailId &c不存在!")
        }
        task?.cancel()
        if (scanner_period > 0L && (DatabaseConfig.isConnected || (SakuraMailHook.hasHooked && sakuraMail_hook))) {
            info("&a定时扫描任务已启动,周期: $scanner_period tick")
            task = Scanner().runTaskTimerAsynchronously(BukkitTemplate.getPlugin(), scanner_period, scanner_period)
        } else task = null
    }

    /**
     * 检查是否不检查
     */
    fun checkByPass(player: HumanEntity): Boolean {
        if (player.isOp || player.hasPermission("sakurabind.bypass.all")) return true
        return false
    }
}