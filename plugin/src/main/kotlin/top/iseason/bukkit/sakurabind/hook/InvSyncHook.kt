package top.iseason.bukkit.sakurabind.hook

import com.xbaimiao.invsync.api.events.PlayerDataSyncDoneEvent
import org.bukkit.event.EventHandler
import top.iseason.bukkit.sakurabind.listener.SelectListener
import top.iseason.bukkittemplate.hook.BaseHook

object InvSyncHook : BaseHook("InvSync"), org.bukkit.event.Listener {
    override fun checkHooked() {
        super.checkHooked()
        if (hasHooked) SelectListener.hasSyncPlugin = true
    }

    @EventHandler
    fun onDataLoad(event: PlayerDataSyncDoneEvent) {
        SelectListener.noScanning.remove(event.player.uniqueId)
    }

}