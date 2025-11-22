package top.iseason.bukkit.sakurabind.dto

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import top.iseason.bukkittemplate.config.DatabaseConfig

object UniqueLogs : IntIdTable() {
    override val tableName: String get() = "${DatabaseConfig.table_prefix}${super.tableName}"

    var uuid = char("owner", 36)
    var unique = varchar("unique", 255)
    var type = varchar("type", 32)
    var log = text("log")
    var time = datetime("time")

    init {
        index(false, uuid, unique)
    }
}