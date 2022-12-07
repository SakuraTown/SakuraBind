package top.iseason.bukkit.sakurabind.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.SakuraBind
import top.iseason.bukkit.sakurabind.config.BaseSetting

/**
 * 在匹配到设置时调用
 */
class ItemMatchedEvent(
    /**
     * 被匹配的物品
     */
    val item: ItemStack,
    /**
     * 匹配的设置,设置为null将会使用全局设置
     */
    var matchSetting: BaseSetting?
) : Event(Thread.currentThread() != SakuraBind.mainThread) {

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlers

    }

    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }

}