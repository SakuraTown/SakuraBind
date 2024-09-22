package top.iseason.bukkit.sakurabind.pickers

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.SendBackLogger
import top.iseason.bukkit.sakurabind.hook.SweetMailHook
import top.iseason.bukkit.sakurabind.utils.SendBackType
import top.mrxiaom.sweetmail.IMail
import top.mrxiaom.sweetmail.SweetMail
import top.mrxiaom.sweetmail.attachments.AttachmentItem
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.map

object SweetMailPicker : BasePicker("SweetMail") {

    private val map = ConcurrentHashMap<UUID, LinkedList<ItemStack>>()

    override fun pickup(
        uuid: UUID,
        items: Array<ItemStack>,
        type: SendBackType,
        notify: Boolean
    ): Array<ItemStack>? {
        if (!SweetMailHook.hasHooked) return null
        addCache(map, uuid, type, items, SweetMailPicker::sendBack)
        return emptyArray()
    }

    fun sendBack(uuid: UUID, type: SendBackType) {
        val items = map.remove(uuid) ?: return
        val sender = if (SweetMail.getInstance().isOnlineMode) {
            uuid.toString()
        } else Bukkit.getOfflinePlayer(uuid).name
        val mail = IMail.api()
            .createSystemMail(Config.sweetMailSender)
            .setIcon(Config.sweetMailIcon) // 设置图标，详见源码注释
            .setTitle(Config.sweetMailTitle)
            .addContent(Config.sweetMailContent)
            .setAttachments(items.map { AttachmentItem.build(it) })
            .setReceiver(sender)
        val sweetMailExpire = Config.sweetMailExpire
        if (sweetMailExpire > 0)
            mail.outdateTime = System.currentTimeMillis() + sweetMailExpire * 1000L
        val send = mail.send()
        if (!send.ok()) {
            continuePickup(SweetMailPicker, uuid, type, items.toTypedArray())
        } else {
            SendBackLogger.log(uuid, type, name, items)
        }
    }

    override fun pickup(
        player: OfflinePlayer,
        items: Array<ItemStack>,
        type: SendBackType,
        notify: Boolean
    ): Array<ItemStack>? {
        if (!SweetMailHook.hasHooked) return null
        return pickup(player.uniqueId, items, type, notify)
    }

}