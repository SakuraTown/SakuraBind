package top.iseason.bukkit.sakurabind.dto

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.javatime.datetime
import top.iseason.bukkit.sakurabind.utils.SendBackType
import top.iseason.bukkittemplate.config.DatabaseConfig

object SendBackLogs : IntIdTable() {
    override val tableName: String get() = "${DatabaseConfig.table_prefix}${super.tableName}_v2"

    var uuid = char("uuid", 36)
    var type = enumeration<SendBackType>("type")
    var dest = varchar("dest", 255)
    var time = datetime("time")
    var attach = text("attach")

    init {
        index(false, uuid, type)
    }
}