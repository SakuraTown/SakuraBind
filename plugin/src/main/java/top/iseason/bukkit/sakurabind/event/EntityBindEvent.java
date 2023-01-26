package top.iseason.bukkit.sakurabind.event;

import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import top.iseason.bukkit.sakurabind.config.BaseSetting;
import top.iseason.bukkit.sakurabind.logger.BindType;

import java.util.UUID;

public class EntityBindEvent extends BindEvent {
    private static final HandlerList handlers = new HandlerList();
    /**
     * 绑定的物品
     */
    private final Entity entity;

    public EntityBindEvent(Entity entity, BaseSetting setting, UUID uuid, BindType bindType) {
        super(setting, uuid, bindType);
        this.entity = entity;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public Entity getEntity() {
        return entity;
    }

}
