package top.iseason.bukkit.sakurabind.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import top.iseason.bukkit.sakurabind.SakuraBind;
import top.iseason.bukkit.sakurabind.config.BaseSetting;

public class PlayerDenyMessageEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final HumanEntity player;
    private final BaseSetting setting;
    private final ItemStack item;
    private final Block block;
    private final Entity entity;
    private boolean isCancelled = false;
    /**
     * 提示的消息
     */
    private String message;
    /**
     * 是否正在冷却中，是由config.yml中的 message-coolDown 控制
     */
    private Boolean isCoolDown;

    public PlayerDenyMessageEvent(
            HumanEntity player,
            BaseSetting setting,
            String message,
            Boolean isCoolDown,
            ItemStack item,
            Block block,
            Entity entity
    ) {
        super(Thread.currentThread() != SakuraBind.mainThread);
        this.player = player;
        this.setting = setting;
        this.message = message;
        this.isCoolDown = isCoolDown;
        this.item = item;
        this.block = block;
        this.entity = entity;
    }

    public PlayerDenyMessageEvent(
            HumanEntity player,
            BaseSetting setting,
            String message,
            Boolean isCoolDown,
            ItemStack item) {
        this(player, setting, message, isCoolDown, item, null, null);
    }

    public PlayerDenyMessageEvent(
            HumanEntity player,
            BaseSetting setting,
            String message,
            Boolean isCoolDown,
            Block block
    ) {
        this(player, setting, message, isCoolDown, null, block, null);
    }

    public PlayerDenyMessageEvent(
            HumanEntity player,
            BaseSetting setting,
            String message,
            Boolean isCoolDown,
            Entity entity
    ) {
        this(player, setting, message, isCoolDown, null, null, entity);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    public HumanEntity getPlayer() {
        return player;
    }

    public BaseSetting getSetting() {
        return setting;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getCoolDown() {
        return isCoolDown;
    }

    public void setCoolDown(Boolean coolDown) {
        isCoolDown = coolDown;
    }

    public ItemStack getItem() {
        return item;
    }

    public Block getBlock() {
        return block;
    }

    public Entity getEntity() {
        return entity;
    }
}
