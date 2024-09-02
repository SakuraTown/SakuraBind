package top.iseason.bukkit.sakurabind.pickers

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import studio.trc.bukkit.globalmarketplus.api.Mailbox
import studio.trc.bukkit.globalmarketplus.api.Merchant
import studio.trc.bukkit.globalmarketplus.mailbox.ItemMailType
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.hook.GlobalMarketPlusHook
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
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
        val senderName = Config.market_sender_name
        val seconds = Config.market_sender_time
        val time = System.currentTimeMillis()
        val expire = if (seconds <= 0) -1L else (time + seconds * 1000)
        // 邮箱上限
        val mailQuantityLimit = Merchant.getMerchant(uniqueId).group.mailQuantityLimit

        var mail = items
        var remaining: Array<ItemStack> = emptyArray()
        if (mailQuantityLimit > 0) {
            // 剩余空间
            val size = mailQuantityLimit - mailbox.itemMails.size
            // 没空间
            if (size <= 0) return items
            mail = items.take(size).toTypedArray()
            val mailSize = mail.size
            if (mailSize < items.size)
                remaining = items.drop(mailSize).toTypedArray()
        }
        var count = 0
        for (stack in mail) {
            count += stack.amount
            mailbox.addMail(
                uniqueId,
                player.name,
                ItemMailType.OTHER_SOURCE,
                time,
                expire,
                stack,
                null,
                senderName,
                stack.amount
            )
        }
        if (player is Player) {
            MessageTool.sendNormal(player, Lang.send_back__global_market_plus.formatBy(count))
        }
        return remaining
    }

}