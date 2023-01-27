package top.iseason.bukkit.sakurabind.event;

import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import top.iseason.bukkit.sakurabind.config.BaseSetting;
import top.iseason.bukkit.sakurabind.utils.BindType;

import java.util.UUID;

public class ItemBindEvent extends BindEvent {
    private static final HandlerList handlers = new HandlerList();
    /**
     * 绑定的物品
     */
    private final ItemStack item;


    public ItemBindEvent(ItemStack item, BaseSetting setting, UUID uuid, BindType bindType) {
        super(setting, uuid, bindType);
        this.item = item;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public ItemStack getItem() {
        return item;
    }

}
