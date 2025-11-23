package top.iseason.bukkit.sakurabind.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.iseason.bukkit.sakurabind.config.BaseSetting;
import top.iseason.bukkit.sakurabind.utils.BindType;

import java.util.UUID;


/**
 * 物品已经解绑的回调事件，无法取消
 */
public class ItemUnBoundEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final BaseSetting setting;
    private final BindType bindType;
    private final UUID owner;
    /**
     * 解绑后的物品
     */
    private final ItemStack item;


    public ItemUnBoundEvent(ItemStack item, BaseSetting setting, UUID uuid, BindType bindType) {
        super(!Bukkit.isPrimaryThread());
        this.item = item;
        this.setting = setting;
        this.owner = uuid;
        this.bindType = bindType;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public ItemStack getItem() {
        return item;
    }

    public BaseSetting getSetting() {
        return setting;
    }

    public BindType getBindType() {
        return bindType;
    }

    public UUID getOwner() {
        return owner;
    }
}
