package top.iseason.bukkit.sakurabind.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import top.iseason.bukkit.sakurabind.SakuraBind;
import top.iseason.bukkit.sakurabind.config.BaseSetting;

import java.util.UUID;

public class ItemBindEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    /**
     * 绑定的物品
     */
    private final ItemStack item;
    private boolean isCancelled = false;
    private BaseSetting setting;
    /**
     * 绑定的玩家的uuid
     */
    private UUID uuid;

    public ItemBindEvent(ItemStack item, BaseSetting setting, UUID uuid) {
        super(Thread.currentThread() != SakuraBind.mainThread);
        this.item = item;
        this.setting = setting;
        this.uuid = uuid;
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

    public BaseSetting getSetting() {
        return setting;
    }

    public void setSetting(BaseSetting setting) {
        this.setting = setting;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public ItemStack getItem() {
        return item;
    }
}
