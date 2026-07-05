package top.iseason.bukkit.sakurabind.module

import de.tr7zw.nbtapi.NBT
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.messaging.PluginMessageListener

import top.iseason.bukkit.sakurabind.config.module.RetrieveConfig
import top.iseason.bukkit.sakurabind.event.ItemBoundEvent
import top.iseason.bukkit.sakurabind.event.ItemUnBoundEvent
import java.util.*

object Retrieve : org.bukkit.event.Listener, PluginMessageListener {
    private const val SETTING_KEY = "module.retrieve"

    @EventHandler
    fun onItemBoundEvent(event: ItemBoundEvent) {
        val setting = event.setting
        if (!setting.getBoolean(SETTING_KEY, event.owner.toString(), null)) {
            return
        }
        setUUID(event.item)
    }

    @EventHandler
    fun onItemUnBoundEvent(event: ItemUnBoundEvent) {
        val setting = event.setting
        if (!setting.getBoolean(SETTING_KEY, event.owner.toString(), null)) {
            return
        }
        delUUID(event.item)
    }

    fun setUUID(item: ItemStack) {
        val nbtPath = RetrieveConfig.nbtPath
        NBT.modify<UUID>(item) {
            if (it.hasTag(nbtPath)) return@modify null
            val uuid = UUID.randomUUID()
            it.setUUID(nbtPath, uuid)
            uuid
        } ?: return
        // todo
    }

    fun delUUID(item: ItemStack) {
        val nbtPath = RetrieveConfig.nbtPath
        NBT.modify<UUID>(item) {
            val uuid = it.getUUID(nbtPath) ?: return@modify null
            it.removeKey(nbtPath)
            uuid
        } ?: return
        // todo
    }

    override fun onPluginMessageReceived(
        channel: String,
        player: Player,
        message: ByteArray?
    ) {
        return
    }

}
