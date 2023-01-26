package top.iseason.bukkit.sakurabind.event;

import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import top.iseason.bukkit.sakurabind.config.BaseSetting;
import top.iseason.bukkit.sakurabind.logger.BindType;

import java.util.UUID;

public class EntityUnBindEvent extends UnBindEvent {
    private static final HandlerList handlers = new HandlerList();


    private final Entity entity;

    public EntityUnBindEvent(Entity entity, BaseSetting setting, UUID owner, BindType bindType) {
        super(setting, owner, bindType);
        this.entity = entity;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}

