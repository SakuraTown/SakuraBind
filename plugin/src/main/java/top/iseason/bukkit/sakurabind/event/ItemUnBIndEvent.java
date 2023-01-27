package top.iseason.bukkit.sakurabind.event;

import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import top.iseason.bukkit.sakurabind.config.BaseSetting;
import top.iseason.bukkit.sakurabind.utils.BindType;

import java.util.UUID;

public class ItemUnBIndEvent extends UnBindEvent {
    private static final HandlerList handlers = new HandlerList();
    private final ItemStack item;

    public ItemUnBIndEvent(ItemStack item, UUID uuid, BaseSetting setting, BindType bindType) {
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
