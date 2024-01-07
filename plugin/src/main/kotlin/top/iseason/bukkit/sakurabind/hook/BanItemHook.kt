package top.iseason.bukkit.sakurabind.hook

import cc.bukkitPlugin.banitem.api.invGettor.InvGettorManager
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import top.iseason.bukkittemplate.hook.BaseHook

object BanItemHook : BaseHook("BanItem") {

    fun getModInventories(player: Player): Collection<Inventory> = InvGettorManager.getAllInv(player)
    override fun checkHooked() {
        try {
            Class.forName("cc.bukkitPlugin.banitem.BanItem")
            super.checkHooked()
        } catch (_: Exception) {
        }
    }
}