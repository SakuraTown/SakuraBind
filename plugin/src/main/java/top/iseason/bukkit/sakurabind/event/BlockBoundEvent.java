package top.iseason.bukkit.sakurabind.event;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import top.iseason.bukkit.sakurabind.config.BaseSetting;
import top.iseason.bukkit.sakurabind.utils.BindType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * 方块已经绑定的回调事件，无法取消
 */
public class BlockBoundEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final BaseSetting setting;
    private final BindType bindType;
    private final UUID owner;
    /**
     * 绑定的物品
     */
    private final Block block;

    private List<String> extraData;

    public BlockBoundEvent(Block block, BaseSetting setting, UUID uuid, BindType bindType, List<String> extraData) {
        super(!Bukkit.isPrimaryThread());
        this.block = block;
        this.setting = setting;
        this.owner = uuid;
        this.bindType = bindType;
        this.extraData = extraData;
    }

    public static HandlerList getHandlerList() {
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

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public Block getBlock() {
        return block;
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
