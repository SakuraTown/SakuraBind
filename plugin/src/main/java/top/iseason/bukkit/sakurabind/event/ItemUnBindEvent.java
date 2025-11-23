package top.iseason.bukkit.sakurabind.event;

import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import top.iseason.bukkit.sakurabind.config.BaseSetting;
import top.iseason.bukkit.sakurabind.utils.BindType;

import java.util.UUID;

/**
 * 物品解绑事件，可以取消
 */
public class ItemUnBindEvent extends UnBindEvent {
    private static final HandlerList handlers = new HandlerList();
    private final ItemStack item;

    public ItemUnBindEvent(ItemStack item, BaseSetting setting, UUID uuid, BindType bindType) {
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
