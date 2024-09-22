package top.iseason.bukkit.sakurabind.dto

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import top.iseason.bukkit.sakurabind.utils.SendBackType

object SendBackLogs : IntIdTable() {
    var uuid = uuid("owner")
    var type = enumeration<SendBackType>("type")
    var dest = varchar("dest", 255)
    var time = datetime("time")
    var attach = text("attach")

    init {
        index(false, uuid, type)
    }
}

class SendBackLog(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SendBackLog>(SendBackLogs)

    var uuid by SendBackLogs.uuid
    var type by SendBackLogs.type
    var dest by SendBackLogs.dest
    var time by SendBackLogs.time
    var attach by SendBackLogs.attach

}