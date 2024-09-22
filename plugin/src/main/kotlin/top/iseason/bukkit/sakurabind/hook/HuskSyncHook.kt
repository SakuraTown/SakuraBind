package top.iseason.bukkit.sakurabind.hook

import net.william278.husksync.event.BukkitSyncCompleteEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import top.iseason.bukkit.sakurabind.listener.SelectListener
import top.iseason.bukkittemplate.hook.BaseHook

object HuskSyncHook : BaseHook("HuskSync"), org.bukkit.event.Listener {
    override fun checkHooked() {
        super.checkHooked()
        if (hasHooked) SelectListener.hasSyncPlugin = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDataSync(event: BukkitSyncCompleteEvent) {
        SelectListener.noScanning.remove(event.user.uuid)
    }

}