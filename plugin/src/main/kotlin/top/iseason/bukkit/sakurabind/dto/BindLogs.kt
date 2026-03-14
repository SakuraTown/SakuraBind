package top.iseason.bukkit.sakurabind.dto

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.javatime.datetime

import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkittemplate.config.DatabaseConfig

object BindLogs : IntIdTable() {

    override val tableName: String get() = "${DatabaseConfig.table_prefix}${super.tableName}_v2"

    var uuid = char("uuid", 36)
    var bindType = enumeration<BindType>("type")
    var setting = varchar("setting", 255)
    var time = datetime("time")
    var attach = text("attach")

    init {
        index(false, uuid, bindType)
    }
}
