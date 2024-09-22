package top.iseason.bukkit.sakurabind.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import top.iseason.bukkit.sakurabind.utils.SendBackType;

import java.util.UUID;

/*
 * 物品批量送回事件，可取消
 * */
public class ItemSendBackEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final ItemStack[] items;
    private final UUID owner;
    private final SendBackType type;
    private boolean isCancelled = false;

    public ItemSendBackEvent(UUID owner, SendBackType type, ItemStack[] item) {
        super(!Bukkit.isPrimaryThread());
        this.owner = owner;
        this.type = type;
        this.items = item;
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

    public ItemStack[] getItems() {
        return items;
    }

    public UUID getOwner() {
        return owner;
    }

    public SendBackType getType() {
        return type;
    }

}
