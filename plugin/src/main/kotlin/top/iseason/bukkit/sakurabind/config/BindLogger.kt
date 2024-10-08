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
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.getDisplayName
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.noColor
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.toColor
import top.iseason.bukkittemplate.utils.other.runAsync
import java.io.File
import java.time.LocalDateTime
import java.util.*
import java.util.logging.FileHandler
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

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
    var console = true

    @Key
    @Comment("", "将绑定信息输出到数据库中，可跨服")
    var database = false

    @Key
    @Comment("", "将绑定信息输出到独立的文件中")
    var file = false

    @Key
    @Comment("", "独立的文件的位置,修改需重启生效")
    var file_path = File(BukkitTemplate.getPlugin().dataFolder, "log${File.separatorChar}bind-log-%g-%u.log").toString()

    @Key
    @Comment("", "独立的文件的最大数量，每个5M,修改需重启生效")
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

    @Key("bind-type-description-v2")
    @Comment("", "绑定类型翻译")
    var bind_type_description: ConfigurationSection =
        config.createSection("bind-type-description-v2", BindType.values().associate { it.name to it.description })

    @Key
    @Comment("", "日志显示格式")
    var format = "物主: {0} 行为: {1} 配置: {2} 信息: {3}"

    @Key
    @Comment("", "物品显示格式, 替换logger.format的 {3}", "占位符分别为 类型、名字、数量、子ID")
    var format_item = "物品 {0}{3} {1} x {2}"

    @Key
    @Comment("", "方块显示格式, 替换logger.format的 {3}", "占位符分别为 类型、位置")
    var format_block = "方块: {0} {1}"

    @Key
    @Comment("", "实体显示格式, 替换logger.format的 {3}", "占位符分别为 类型、名字、UUID、位置")
    var format_entity = "实体: {0} {1} {2} {3} {4}"

    private val file_logger = Logger.getLogger("SakuraBind-BindLogger")
        .apply {
            useParentHandlers = false
            File(file_path).parentFile.mkdirs()
            val handler = FileHandler(file_path, 5120000, file_max_count, true)
            handler.formatter = Formatter()
            handler.encoding = "UTF-8"
            addHandler(handler)
        }

    class Formatter : SimpleFormatter() {
        private val format = "[%1\$tF %1\$tT] %2\$s %n"
        override fun format(record: LogRecord): String {
            return String.format(
                format,
                Date(record.millis),
                record.message
            )
        }
    }

    init {
        DisableHook.addTask {
            file_logger.handlers.forEach { it.close() }
        }
    }

    override fun onLoaded(section: ConfigurationSection) {
        bind_type_description.getValues(false).forEach { (t, u) ->
            try {
                BindType.valueOf(t).description = u.toString()
            } catch (_: Exception) {
                warn("$t 不是有效的类型!")
            }
        }
    }

    fun log(owner: UUID, type: BindType, setting: BaseSetting, item: ItemStack) {
        if (!enable || filter.contains(type.name)) return
        val name = item.getDisplayName() ?: ""
        val subId = item.data?.data ?: 0
        val attach = format_item.formatBy(item.type, name, item.amount, if (subId > 0) subId else "")
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
            var message: String? = null
            if (console) {
                message = format.formatBy(owner, type.description, setting.keyPath, attach)
                Bukkit.getConsoleSender().sendMessage((SimpleLogger.prefix + message).toColor())
            }
            if (file) {
                if (message == null)
                    message = format.formatBy(owner, type.description, setting.keyPath, attach)
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