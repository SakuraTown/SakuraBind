package top.iseason.bukkit.sakurabind.dto

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object UniqueLogs : IntIdTable() {
    var uuid = char("owner", 36)
    var unique = varchar("unique", 255)
    var type = varchar("type", 32)
    var log = text("log")
    var time = datetime("time")

    init {
        index(false, uuid, unique)
    }
}

class UniqueLog(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UniqueLog>(UniqueLogs)

    var uuid by UniqueLogs.uuid
    var unique by UniqueLogs.unique
    var type by UniqueLogs.type
    var log by UniqueLogs.log
    var time by UniqueLogs.time

}