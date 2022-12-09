package top.iseason.bukkit.sakurabind.event;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import top.iseason.bukkit.sakurabind.SakuraBind;
import top.iseason.bukkit.sakurabind.config.BaseSetting;

public class AutoBindMessageEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final ItemStack item;
    private boolean isCancelled = false;
    private final HumanEntity player;
    private final BaseSetting setting;
    /**
     * 提示的消息
     */
    private String message;
    /**
     * 是否正在冷却中，是由config.yml中的 message-coolDown 控制
     */
    private Boolean isCoolDown;


    public AutoBindMessageEvent(HumanEntity player, BaseSetting setting, String message, Boolean isCoolDown, ItemStack item) {
        super(Thread.currentThread() != SakuraBind.mainThread);
        this.player = player;
        this.setting = setting;
        this.message = message;
        this.isCoolDown = isCoolDown;
        this.item = item;
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

    public BaseSetting getSetting() {
        return setting;
    }

    public HumanEntity getPlayer() {
        return player;
    }
}
