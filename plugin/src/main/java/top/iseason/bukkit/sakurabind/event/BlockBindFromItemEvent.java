package top.iseason.bukkit.sakurabind.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import top.iseason.bukkit.sakurabind.config.BaseSetting;
import top.iseason.bukkit.sakurabind.utils.BindType;

import java.util.UUID;

public class BlockBindFromItemEvent extends BlockBindEvent {
    private static final HandlerList handlers = new HandlerList();

    /**
     * 玩家手上的物品
     */
    private final ItemStack handItem;

    private final Player player;

    private final boolean isMultiPlace;

    public BlockBindFromItemEvent(Block block, BaseSetting setting, UUID uuid, BindType bindType, ItemStack handItem, Player player, boolean isMultiPlace) {
        super(block, setting, uuid, bindType);
        this.handItem = handItem;
        this.player = player;
        this.isMultiPlace = isMultiPlace;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * 玩家手上的物品
     */
    public ItemStack getHandItem() {
        return handItem;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isMultiPlace() {
        return isMultiPlace;
    }
}
