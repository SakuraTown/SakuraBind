package top.iseason.bukkit.sakurabind.config

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.dto.BindLog
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.DisableHook
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.debug.warn
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.noColor
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.toColor
import top.iseason.bukkittemplate.utils.other.runAsync
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.util.logging.Logger

@FilePath("logger.yml")
object BindLogger : SimpleYAMLConfig() {

    @Key
    @Comment("", "日志系统，用于记录绑定信息")
    var readme = ""

    @Key
    @Comment("", "总开关")
    var enable = false

    @Key
    @Comment("", "将绑定信息输出到控制台")
    var console = false

    @Key
    @Comment("", "将绑定信息输出到数据库中，可跨服")
    var database = false

    @Key
    @Comment("", "将绑定信息输出到独立的文件中")
    var file = true

    @Key
    @Comment("", "独立的文件的位置,修改需重启生效")
    var file_path = File(BukkitTemplate.getPlugin().dataFolder, "log${File.separatorChar}bind-log").toString()

    @Key
    @Comment("", "独立的文件的最大数量，每个1M,修改需重启生效")
    var file_max_count = 10

    @Key
    @Comment(
        "",
        "日志忽略特定绑定类型, 从下面选"
    )
    var filter = setOf(
        "ITEM_TO_BLOCK_BIND",
        "ITEM_TO_BLOCK_UNBIND",
        "BLOCK_TO_ENTITY_BIND",
        "BLOCK_TO_ENTITY_UNBIND",
        "BLOCK_MOVE_BIND",
        "BLOCK_MOVE_UNBIND",
        "ENTITY_TO_ITEM_BIND",
        "ENTITY_TO_ITEM_UNBIND",
        "ITEM_TO_ENTITY_BIND",
        "ITEM_TO_ENTITY_UNBIND",
        "BLOCK_TO_ITEM_BIND",
        "BLOCK_TO_ITEM_UNBIND",
        "ENTITY_TO_BLOCK_BIND",
        "ENTITY_TO_BLOCK_UNBIND",
    )

    @Key
    @Comment("", "绑定类型翻译")
    var bind_type_description: ConfigurationSection =
        config.createSection("bind-type-description", BindType.values().associate { it.name to it.description })

    @Key
    @Comment("", "日志显示格式")
    var format = "物主: {0} 行为: {1} 配置: {2} 信息: {3}"

    @Key
    @Comment("", "物品显示格式, 替换logger.format的 {3}", "占位符分别为 类型、名字、数量、子ID")
    var format_item = "物品 {0}{4} {1} x {2}"

    @Key
    @Comment("", "方块显示格式, 替换logger.format的 {3}", "占位符分别为 类型、位置")
    var format_block = "方块: {0} {1}"

    @Key
    @Comment("", "实体显示格式, 替换logger.format的 {3}", "占位符分别为 类型、名字、UUID、位置")
    var format_entity = "实体: {0} {1} {2} {3} {4}"

    private val file_logger = Logger.getLogger("SakuraBind-FileLogger").apply { useParentHandlers = false }

    init {
        DisableHook.addTask {
            file_logger.handlers.forEach { it.close() }
        }
    }

    private object FileFormatter : Formatter() {
        private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        override fun format(record: LogRecord?): String {
            record ?: return ""
            return "[${LocalDateTime.now().format(format)}] ${record.message}\n"
        }
    }

    private var fileHandler: FileHandler? = null
    override fun onLoaded(section: ConfigurationSection) {
        bind_type_description.getValues(false).forEach { (t, u) ->
            try {
                BindType.valueOf(t).description = u.toString()
            } catch (_: Exception) {
                warn("$t 不是有效的类型!")
            }
        }
        if (!enable) return
        File(file_path).parentFile.mkdirs()
        file_logger.handlers.forEach { it.close() }
        file_logger.removeHandler(fileHandler)
        fileHandler = FileHandler(file_path, 1024000, file_max_count, true)
        fileHandler!!.formatter = FileFormatter
        file_logger.addHandler(fileHandler)
    }

    fun log(owner: UUID, type: BindType, setting: BaseSetting, item: ItemStack) {
        if (!enable || filter.contains(type.name)) return
        val name = if (item.hasItemMeta() && item.itemMeta!!.hasDisplayName()) {
            item.itemMeta!!.displayName
        } else ""
        val subId = item.data?.data ?: 0
        val attach = format_item.formatBy(item.type, name, item.amount, if (subId > 0) subId else null)
        log(owner, type, setting, attach)
    }

    fun log(owner: UUID, type: BindType, setting: BaseSetting, block: Block) {
        if (!enable || filter.contains(type.name)) return
        val attach =
            format_block.formatBy(block.type, "${block.world.name},${block.x},${block.y},${block.z}")
        log(owner, type, setting, attach)
    }

    fun log(owner: UUID, type: BindType, setting: BaseSetting, entity: Entity) {
        if (!enable || filter.contains(type.name)) return
        val attach = format_entity.formatBy(
            entity.type,
            entity.customName ?: "",
            entity.uniqueId,
            formatLocation(entity.location)
        )
        log(owner, type, setting, attach)
    }

    private fun log(owner: UUID, type: BindType, setting: BaseSetting, attach: String) {
        runAsync {
            val message = format.formatBy(owner, type.description, setting.keyPath, attach)
            if (console) {
                Bukkit.getConsoleSender().sendMessage((SimpleLogger.prefix + message).toColor())
            }
            if (file) {
                file_logger.info(message.toColor().noColor())
            }
            if (database) {
                dbTransaction {
                    BindLog.new {
                        this.uuid = owner
                        this.bindType = type
                        this.setting = setting.keyPath
                        this.time = LocalDateTime.now()
                        this.attach = attach
                    }
                }
            }
        }
    }

    private fun formatLocation(loc: Location): String {
        val x = String.format("%.2f", loc.x)
        val y = String.format("%.2f", loc.y)
        val z = String.format("%.2f", loc.z)
        return "${loc.world?.name},$x,$y,$z"
    }

}