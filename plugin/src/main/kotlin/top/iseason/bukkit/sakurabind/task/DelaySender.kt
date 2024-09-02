package top.iseason.bukkit.sakurabind.task

import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.dto.PlayerItem
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.debug.warn
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toByteArray
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.submit
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class DelaySender private constructor(private val uuid: UUID) : BukkitRunnable() {

    private val inv = Bukkit.createInventory(null, 36)

    //缓冲5秒
    @Volatile
    private var coolDown = 3
    private var onShutDown = false
    override fun run() {
        if (coolDown < 0) {
            cancel()
            return
        }
        coolDown--
    }

    @Synchronized
    private fun addItem(items: Array<ItemStack>) {

        coolDown = 3
        val addItem = inv.addItem(*items)
        //缓存满了
        if (addItem.isNotEmpty()) {
            sendItem()
            addItem(addItem.values.toTypedArray())
            return
        }

    }

    override fun cancel() {
        super.cancel()
        sendItem(!onShutDown)
        remove(uuid)
    }

    private fun sendItem(async: Boolean = true) {
        val itemStacks = inv.filterNotNull()
        inv.clear()
        if (DatabaseConfig.isConnected) {
            if (async) submit(async = true) {
                sendToDataBase(uuid, itemStacks)
            } else sendToDataBase(uuid, itemStacks)
        } else {
            warn("数据库未启用,无法发送暂存箱子!")
        }
        Bukkit.getPlayer(uuid)?.sendColorMessage(Lang.lost_item_send_when_online)
    }

    companion object {
        private val map = ConcurrentHashMap<UUID, DelaySender>()
        private val plugin = BukkitTemplate.getPlugin()

        fun sendItem(uuid: UUID, items: Array<ItemStack>) {
            var sender = map[uuid]
            if (!plugin.isEnabled) {
                if (sender == null) sender = DelaySender(uuid)
                sender.addItem(items)
                sender.sendItem(false)
            } else if (sender == null) {
                sender = DelaySender(uuid)
                map[uuid] = sender
                sender.addItem(items)
                sender.runTaskTimerAsynchronously(plugin, 20, 20)
            }
        }

        fun remove(uuid: UUID) = map.remove(uuid)
        fun shutdown() {
            map.values.forEach {
                it.onShutDown = true
                it.cancel()
            }
        }

        fun sendToDataBase(uid: UUID, items: List<ItemStack>) {
            dbTransaction {
                PlayerItem.new {
                    this.uuid = uid
                    this.item = ExposedBlob(items.toByteArray())
                }
            }
        }
    }
}