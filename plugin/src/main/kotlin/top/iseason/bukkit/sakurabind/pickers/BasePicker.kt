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

        fun continuePickup(base: BasePicker, uuid: UUID, items: Array<ItemStack>) {
            val player = Bukkit.getPlayer(uuid)
            val isOnline = player != null
            var find = false
            var temp = items
            for (picker in configPickers) {
                if (!find) {
                    if (picker == base) {
                        find = true
                    }
                    continue
                }
                temp = if (isOnline)
                    picker.pickup(player, temp, true) ?: continue
                else
                    picker.pickup(uuid, temp, false) ?: continue
                if (temp.isEmpty()) break
            }
        }
    }
}