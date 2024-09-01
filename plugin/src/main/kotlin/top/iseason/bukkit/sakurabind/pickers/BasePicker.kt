package top.iseason.bukkit.sakurabind.pickers

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.hook.GlobalMarketPlusHook
import java.util.*

abstract class BasePicker(val name: String) {

    // 离线拾取
    abstract fun pickup(uuid: UUID, items: Array<ItemStack>, notify: Boolean): Array<ItemStack>?

    // 在线拾取
    abstract fun pickup(player: OfflinePlayer, items: Array<ItemStack>, notify: Boolean): Array<ItemStack>?

    fun register() {
        allPickers[name.lowercase()] = this
    }

    companion object {
        val allPickers: HashMap<String, BasePicker> = hashMapOf()
        val configPickers = ArrayList<BasePicker>()
        fun init() {
            PlayerInvPicker.register()
            EnderChestPicker.register()
            DataBasePicker.register()
            if (GlobalMarketPlusHook.hasHooked) {
                GlobalMarketPlusPicker.register()
            }
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