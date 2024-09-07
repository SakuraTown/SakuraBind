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
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.drop
import kotlin.collections.take

object GlobalMarketPlusPicker : BasePicker("GlobalMarketPlus") {
    private val map = ConcurrentHashMap<UUID, LinkedList<ItemStack>>()

    override fun pickup(
        uuid: UUID,
        items: Array<ItemStack>,
        notify: Boolean
    ): Array<ItemStack>? {
        if (!GlobalMarketPlusHook.hasHooked) return items
        addCache(map, uuid, items, GlobalMarketPlusPicker::sendBack)
        return emptyArray()
    }

    override fun pickup(
        player: OfflinePlayer,
        items: Array<ItemStack>,
        notify: Boolean
    ): Array<ItemStack>? {
        if (!GlobalMarketPlusHook.hasHooked) return items
        return pickup(player.uniqueId, items, notify)
    }

    fun sendBack(uuid: UUID) {
        var temp = sendBack0(uuid) ?: return
        continuePickup(GlobalMarketPlusPicker, uuid, temp)
    }

    fun sendBack0(uuid: UUID): Array<ItemStack>? {
        val mailbox = Mailbox.getMailbox(uuid)
        val senderName = Config.market_sender_name
        val seconds = Config.market_sender_time
        val time = System.currentTimeMillis()
        val expire = if (seconds <= 0) -1L else (time + seconds * 1000)
        // 邮箱上限
        val mailQuantityLimit = Merchant.getMerchant(uuid).group.mailQuantityLimit

        val items = map.remove(uuid) ?: return null
        var mail: List<ItemStack> = items
        var remaining: List<ItemStack> = ArrayList<ItemStack>()
        if (mailQuantityLimit > 0) {
            // 剩余空间
            val size = mailQuantityLimit - mailbox.itemMails.size
            // 没空间
            if (size <= 0) return items.toTypedArray()
            mail = items.take(size)
            val mailSize = mail.size
            if (mailSize < items.size)
                remaining = items.drop(mailSize)
        }
        var count = 0
        val player = Bukkit.getPlayer(uuid) ?: Bukkit.getOfflinePlayer(uuid)
        val name = player.name

        for (stack in mail) {
            count += stack.amount
            mailbox.addMail(
                uuid,
                name,
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
        return remaining.toTypedArray()
    }

    fun shutdown() {
        map.keys.forEach {
            sendBack(it)
        }
    }

}