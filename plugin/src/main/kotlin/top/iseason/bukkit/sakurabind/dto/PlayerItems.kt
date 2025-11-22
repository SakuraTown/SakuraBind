package top.iseason.bukkit.sakurabind.dto

import org.jetbrains.exposed.dao.id.IntIdTable
import top.iseason.bukkittemplate.config.DatabaseConfig


object PlayerItems : IntIdTable() {
    override val tableName: String get() = "${DatabaseConfig.table_prefix}${super.tableName}"

    var uuid = uuid("uuid").index()
    var item = blob("item")
}
