package top.iseason.bukkit.sakurabind.event

import org.bukkit.entity.HumanEntity
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.SakuraBind
import top.iseason.bukkit.sakurabind.config.BaseSetting

class AutoBindMessageEvent(
    val player: HumanEntity,
    val setting: BaseSetting,
    /**
     * 提示的消息
     */
    var message: String,
    /**
     * 是否正在冷却中，是由config.yml中的 message-coolDown 控制
     */
    val isCoolDown: Boolean,
    val item: ItemStack

) : Event(Thread.currentThread() != SakuraBind.mainThread), Cancellable {

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