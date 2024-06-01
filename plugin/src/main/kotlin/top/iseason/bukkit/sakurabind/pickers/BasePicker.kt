package top.iseason.bukkit.sakurabind.pickers

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

abstract class BasePicker(val name: String) {

    // 离线拾取
    abstract fun pickup(uuid: UUID, items: Array<ItemStack>, notify: Boolean): Array<ItemStack>?

    // 在线拾取
    abstract fun pickup(player: Player, items: Array<ItemStack>, notify: Boolean): Array<ItemStack>?

    fun register() {
        allPickers[name] = this
    }

    companion object {
        val allPickers: HashMap<String, BasePicker> = hashMapOf()
        val configPickers = ArrayList<BasePicker>()
        fun init() {
            PlayerInvPicker.register()
            EnderChestPicker.register()
            DataBasePicker.register()
        }

        fun pickup(uuid: UUID, items: Array<ItemStack>, notify: Boolean): Array<ItemStack> {
            var temp = items
            val player = Bukkit.getPlayer(uuid)
            // 在线
            if (player != null && player.isOnline) {
                for (picker in configPickers) {
                    temp = picker.pickup(player, temp, notify) ?: continue
                    if (temp.isEmpty()) break
                }
            } else {
                for (picker in configPickers) {
                    temp = picker.pickup(uuid, temp, notify) ?: continue
                    if (temp.isEmpty()) break
                }
            }
            return temp
        }
    }
}