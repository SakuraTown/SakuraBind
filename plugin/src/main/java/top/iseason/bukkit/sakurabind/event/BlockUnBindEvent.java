package top.iseason.bukkit.sakurabind.event;

import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import top.iseason.bukkit.sakurabind.config.BaseSetting;
import top.iseason.bukkit.sakurabind.logger.BindType;

import java.util.UUID;

public class BlockUnBindEvent extends UnBindEvent {
    private static final HandlerList handlers = new HandlerList();
    private final Block block;

    public BlockUnBindEvent(Block block, BaseSetting setting, UUID owner, BindType bindType) {
        super(setting, owner, bindType);
        this.block = block;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
