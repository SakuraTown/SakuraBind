package top.iseason.bukkit.sakurabind.event;

import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import top.iseason.bukkit.sakurabind.config.BaseSetting;
import top.iseason.bukkit.sakurabind.utils.BindType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BlockBindEvent extends BindEvent {
    private static final HandlerList handlers = new HandlerList();
    /**
     * 绑定的物品
     */
    private final Block block;

    private List<String> extraData = null;

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

    /**
     * 此处的数据会随着方块主人、配置一起储存。
     * 最终会使用类似 String::join 的方式组合成一个字符串
     * 分隔符 delimiter 是制表符 '\t' 请不要在数据中出现 '\t'
     */
    public List<String> getExtraData() {
        if (extraData == null) {
            extraData = new ArrayList<>();
        }
        return extraData;
    }

    public void setExtraData(List<String> extraData) {
        this.extraData = extraData;
    }

    public Block getBlock() {
        return block;
    }

}
