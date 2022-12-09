package top.iseason.bukkit.sakurabind.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import top.iseason.bukkit.sakurabind.SakuraBind;
import top.iseason.bukkit.sakurabind.config.BaseSetting;

public class ItemMatchedEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    /**
     * 被匹配的物品
     */
    private final ItemStack item;
    private boolean isCancelled = false;
    /**
     * 匹配的设置,设置为null将会使用全局设置
     */
    private BaseSetting matchSetting;

    public ItemMatchedEvent(ItemStack item, BaseSetting matchSetting) {
        super(Thread.currentThread() != SakuraBind.mainThread);
        this.item = item;
        this.matchSetting = matchSetting;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    public BaseSetting getMatchSetting() {
        return matchSetting;
    }

    public void setMatchSetting(BaseSetting matchSetting) {
        this.matchSetting = matchSetting;
    }

    public ItemStack getItem() {
        return item;
    }
}
