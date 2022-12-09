package top.iseason.bukkit.sakurabind.event

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.block.Block
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import top.iseason.bukkit.sakurabind.SakuraBind
import top.iseason.bukkit.sakurabind.config.BaseSetting
import java.util.*

/**
 * 在物品绑定某个uuid时触发
 */
class BlockBindEvent(
    /**
     * 绑定的物品
     */
    val block: Block,
    var setting: BaseSetting,
    /**
     * 绑定的玩家的uuid
     */
    var uuid: UUID
) : Event(Thread.currentThread() != SakuraBind.mainThread), Cancellable {
    /**
     * 获取要绑定的玩家, 懒加载
     */
    val player: OfflinePlayer by lazy { Bukkit.getPlayer(uuid) ?: Bukkit.getOfflinePlayer(uuid) }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlers

    }

    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }

    private var cancel = false
    override fun isCancelled() = cancel

    override fun setCancelled(cancel: Boolean) {
        this.cancel = cancel
    }
}