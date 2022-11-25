package top.iseason.bukkit.sakurabind.dto

import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils


object PlayerItems : IntIdTable() {
    var uuid = uuid("uuid").index()
    var item = blob("item")
}

class PlayerItem(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerItem>(PlayerItems)

    var uuid by PlayerItems.uuid
    var item by PlayerItems.item

    fun getItemStacks(): List<ItemStack> {
        return ItemUtils.fromByteArrays(item.bytes)
    }
}