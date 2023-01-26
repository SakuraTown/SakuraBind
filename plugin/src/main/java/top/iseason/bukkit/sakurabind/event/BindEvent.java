package top.iseason.bukkit.sakurabind.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import top.iseason.bukkit.sakurabind.config.BaseSetting;
import top.iseason.bukkit.sakurabind.logger.BindType;

import java.util.UUID;

public abstract class BindEvent extends Event implements Cancellable {

    private boolean isCancelled = false;
    private BaseSetting setting;
    private BindType bindType;
    private UUID owner;

    public BindEvent(BaseSetting setting, UUID owner, BindType bindType) {
        super(!Bukkit.isPrimaryThread());
        this.setting = setting;
        this.owner = owner;
        this.bindType = bindType;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    public BaseSetting getSetting() {
        return setting;
    }

    public void setSetting(BaseSetting setting) {
        this.setting = setting;
    }

    public BindType getBindType() {
        return bindType;
    }

    public void setBindType(BindType bindType) {
        this.bindType = bindType;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

}
