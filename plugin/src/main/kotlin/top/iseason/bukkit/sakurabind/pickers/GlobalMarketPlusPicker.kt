package top.iseason.bukkit.sakurabind.pickers

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import studio.trc.bukkit.globalmarketplus.api.Mailbox
import studio.trc.bukkit.globalmarketplus.mailbox.ItemMailType
import top.iseason.bukkit.sakurabind.hook.GlobalMarketPlusHook
import java.util.UUID

object GlobalMarketPlusPicker : BasePicker("GlobalMarketPlus") {

    override fun pickup(
        uuid: UUID,
        items: Array<ItemStack>,
        notify: Boolean
    ): Array<ItemStack>? {
        if (!GlobalMarketPlusHook.hasHooked) return items
        val player = Bukkit.getPlayer(uuid) ?: Bukkit.getOfflinePlayer(uuid)
        return pickup(player, items, notify)

    }

    override fun pickup(
        player: OfflinePlayer,
        items: Array<ItemStack>,
        notify: Boolean
    ): Array<ItemStack>? {
        if (!GlobalMarketPlusHook.hasHooked) return items
        val uniqueId = player.uniqueId
        val mailbox = Mailbox.getMailbox(player.uniqueId)
        for (stack in items) {
            mailbox.addMail(
                uniqueId,
                player.name,
                ItemMailType.OTHER_SOURCE,
                System.currentTimeMillis(),
                -1L,
                stack,
                null,
                null,
                stack.amount
            )
        }
        return emptyArray()
    }

}