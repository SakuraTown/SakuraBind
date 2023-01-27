package top.iseason.bukkit.sakurabind.dto

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import top.iseason.bukkit.sakurabind.utils.BindType

object BindLogs : IntIdTable() {
    var uuid = uuid("owner")
    var bindType = enumeration<BindType>("type")
    var setting = varchar("setting", 255)
    var time = datetime("time")
    var attach = text("attach")

    init {
        index(false, uuid, bindType)
    }
}

class BindLog(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BindLog>(BindLogs)

    var uuid by BindLogs.uuid
    var bindType by BindLogs.bindType
    var setting by BindLogs.setting
    var time by BindLogs.time
    var attach by BindLogs.attach

}