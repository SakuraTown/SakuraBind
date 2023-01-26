package top.iseason.bukkit.sakurabind.logger

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.BaseSetting
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.dto.BindLog
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.debug.SimpleLogger
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


object BindLogger {
    private val file_logger = Logger.getLogger("SakuraBind-FileLogger")

    init {
        File(Config.logger__file_path).parentFile.mkdirs()
    }

    private val fileHandler =
        FileHandler(Config.logger__file_path, 1024000, Config.logger__file_max_count, true)

    init {
        fileHandler.formatter = object : Formatter() {
            private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            override fun format(record: LogRecord?): String {
                record ?: return ""
                return "[${LocalDateTime.now().format(format)}] ${record.message}"
            }
        }
        file_logger.useParentHandlers = false
        file_logger.addHandler(fileHandler)
    }

    fun log(owner: UUID, type: BindType, setting: BaseSetting, item: ItemStack) {
        if (!Config.logger__enable || Config.logger__filter.contains(type.name)) return
        val name = if (item.hasItemMeta() && item.itemMeta!!.hasDisplayName()) {
            item.itemMeta!!.displayName
        } else ""
        val attach = Config.logger__format_item.formatBy(item.type, name, item.amount)
        log(owner, type, setting, attach)
    }

    fun log(owner: UUID, type: BindType, setting: BaseSetting, block: Block) {
        if (!Config.logger__enable || Config.logger__filter.contains(type.name)) return
        val attach =
            Config.logger__format_block.formatBy(block.type, "${block.world.name},${block.x},${block.y},${block.z}")
        log(owner, type, setting, attach)
    }

    fun log(owner: UUID, type: BindType, setting: BaseSetting, entity: Entity) {
        if (!Config.logger__enable || Config.logger__filter.contains(type.name)) return
        val attach = Config.logger__format_entity.formatBy(
            entity.type,
            entity.customName ?: "",
            entity.uniqueId,
            formatLocation(entity.location)
        )
        log(owner, type, setting, attach)
    }

    private fun log(owner: UUID, type: BindType, setting: BaseSetting, attach: String) {
        runAsync {
            val message = Config.logger__format.formatBy(owner, type.description, setting.keyPath, attach)
            if (Config.logger__console) {
                Bukkit.getConsoleSender().sendMessage((SimpleLogger.prefix + message).toColor())
            }
            if (Config.logger__file) {
                file_logger.info(message.toColor().noColor())
            }
            if (Config.logger__database) {
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