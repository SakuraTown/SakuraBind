package top.iseason.bukkit.sakurabind.event;

import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import top.iseason.bukkit.sakurabind.config.BaseSetting;
import top.iseason.bukkit.sakurabind.logger.BindType;

import java.util.UUID;

public class BlockBindEvent extends BindEvent {
    private static final HandlerList handlers = new HandlerList();
    /**
     * 绑定的物品
     */
    private final Block block;

    public BlockBindEvent(Block block, BaseSetting setting, UUID uuid, BindType bindType) {
        super(setting, uuid, bindType);
        this.block = block;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }


    public Block getBlock() {
        return block;
    }

}
