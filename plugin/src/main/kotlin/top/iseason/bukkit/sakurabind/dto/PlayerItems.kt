package top.iseason.bukkit.sakurabind.dto

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import top.iseason.bukkittemplate.config.DatabaseConfig


object PlayerItems : IntIdTable() {
    override val tableName: String get() = "${DatabaseConfig.table_prefix}${super.tableName}"

    var uuid = javaUUID("uuid").index()
    var item = blob("item")
}
