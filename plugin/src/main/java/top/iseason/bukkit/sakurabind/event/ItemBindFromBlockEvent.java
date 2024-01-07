package top.iseason.bukkit.sakurabind.event;

import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import top.iseason.bukkit.sakurabind.cache.BlockInfo;
import top.iseason.bukkit.sakurabind.utils.BindType;

public class ItemBindFromBlockEvent extends ItemBindEvent {
    private static final HandlerList handlers = new HandlerList();

    private BlockInfo blockInfo;

    public ItemBindFromBlockEvent(ItemStack item, BlockInfo blockInfo, BindType bindType) {
        super(item, blockInfo.getSetting(), blockInfo.getOwnerUUID(), bindType);
        this.blockInfo = blockInfo;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public BlockInfo getBlockInfo() {
        return blockInfo;
    }

    public void setBlockInfo(BlockInfo blockInfo) {
        if (blockInfo == null) throw new NullPointerException("blockInfo must not be null!");
        this.blockInfo = blockInfo;
    }
}
