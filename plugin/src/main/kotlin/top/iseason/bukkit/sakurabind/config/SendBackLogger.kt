package top.iseason.bukkit.sakurabind.config

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.batchInsert
import top.iseason.bukkit.sakurabind.config.BindLogger.Formatter
import top.iseason.bukkit.sakurabind.dto.SendBackLogs
import top.iseason.bukkit.sakurabind.utils.SendBackType
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
import java.util.logging.Logger
import kotlin.collections.component1
import kotlin.collections.component2

@FilePath("send-back-logger.yml")
object SendBackLogger : SimpleYAMLConfig() {

    @Key
    @Comment("", "物品送回日志系统")
    var readme = ""

    @Key
    @Comment("", "总开关")
    var enable = false

    @Key
    @Comment("", "将物品送回输出到控制台")
    var console = true

    @Key
    @Comment("", "将物品送回输出到数据库中，可跨服")
    var database = false

    @Key
    @Comment("", "将物品送回输出到独立的文件中")
    var file = false

    @Key
    @Comment("", "独立的文件的位置,修改需重启生效")
    var file_path =
        File(BukkitTemplate.getPlugin().dataFolder, "log${File.separatorChar}send-back-log-%g-%u.log").toString()

    @Key
    @Comment("", "独立的文件的最大数量，每个5M,修改需重启生效")
    var file_max_count = 10

    @Key("send-back-type-description")
    @Comment("", "送回类型翻译")
    var send_back_type_description: ConfigurationSection =
        config.createSection("send-back-type-description", SendBackType.entries.associate { it.name to it.description })

    @Key
    @Comment(
        "",
        "日志忽略特定送回类型, 从 send-back-type-description 选择键"
    )
    var filter = setOf<String>()

    @Key
    @Comment("", "日志显示格式")
    var format = "[物品送回] 物主: {0} 类型: {1} 途径: {2} 物品: {3}"

    @Key
    @Comment("", "物品显示格式, 替换上面format的 {3}", "占位符分别为 类型、名字、数量、子ID")
    var format_item = "物品 {0}{3} {1} x {2}"

    private val file_logger = Logger.getLogger("SakuraBind-SendBackLogger")
        .apply {
            useParentHandlers = false
            File(file_path).parentFile.mkdirs()
            val handler = FileHandler(file_path, 5120000, file_max_count, true)
            handler.formatter = Formatter()
            handler.encoding = "UTF-8"
            addHandler(handler)
        }

    init {
        DisableHook.addTask {
            file_logger.handlers.forEach { it.close() }
        }
    }

    override fun onLoaded(section: ConfigurationSection) {
        send_back_type_description.getValues(false).forEach { (t, u) ->
            try {
                SendBackType.valueOf(t).description = u.toString()
            } catch (_: Exception) {
                warn("$t 不是有效的类型!")
            }
        }
    }

    fun log(owner: UUID, type: SendBackType, dest: String, items: Array<ItemStack>) {
        if (!enable || filter.contains(type.name) || items.isEmpty()) return
        log(owner, type, dest, items.toList())
    }

    fun log(owner: UUID, type: SendBackType, dest: String, items: Collection<ItemStack>) {
        if (!enable || filter.contains(type.name) || items.isEmpty()) return
        runAsync {
            val arrayList = if (database) ArrayList<String>(items.size) else null
            for (item in items) {
                val name = item.getDisplayName() ?: ""
                val subId = item.data?.data ?: 0
                val attach = format_item.formatBy(item.type, name, item.amount, if (subId > 0) subId else "")
                arrayList?.add(attach)
                var message: String? = null
                if (console) {
                    message = format.formatBy(owner, type.description, dest, attach)
                    Bukkit.getConsoleSender().sendMessage((SimpleLogger.prefix + message).toColor())
                }
                if (file) {
                    if (message == null) message = format.formatBy(owner, type.description, dest, attach)
                    file_logger.info(message.toColor().noColor())
                }
            }
            if (database) {
                dbTransaction {
                    SendBackLogs.batchInsert(arrayList!!) { attach ->
                        this[SendBackLogs.uuid] = owner
                        this[SendBackLogs.type] = type
                        this[SendBackLogs.dest] = dest
                        this[SendBackLogs.time] = LocalDateTime.now()
                        this[SendBackLogs.attach] = attach
                    }
                }
            }
        }
    }
}