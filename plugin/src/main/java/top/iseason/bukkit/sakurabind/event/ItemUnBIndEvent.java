package top.iseason.bukkit.sakurabind.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import top.iseason.bukkit.sakurabind.SakuraBind;
import top.iseason.bukkit.sakurabind.config.BaseSetting;

public class ItemUnBIndEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final ItemStack item;
    private final BaseSetting setting;
    private boolean isCancelled = false;

    public ItemUnBIndEvent(ItemStack item, BaseSetting setting) {
        super(Thread.currentThread() != SakuraBind.mainThread);
        this.item = item;
        this.setting = setting;
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

    public ItemStack getItem() {
        return item;
    }

    public BaseSetting getSetting() {
        return setting;
    }
}
