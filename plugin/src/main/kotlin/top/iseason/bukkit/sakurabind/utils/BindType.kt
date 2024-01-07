package top.iseason.bukkit.sakurabind.utils

enum class BindType(var description: String) {

    UNKNOWN_BIND_ITEM("未知的方式绑定物品"),
    UNKNOWN_UNBIND_ITEM("未知的方式解绑物品"),
    UNKNOWN_BIND_BLOCK("未知的方式绑定方块"),
    UNKNOWN_UNBIND_BLOCK("未知的方式解绑方块"),
    UNKNOWN_BIND_ENTITY("未知的方式绑定实体"),
    UNKNOWN_UNBIND_ENTITY("未知的方式解绑实体"),

    API_BIND_ITEM("API绑定物品"),
    API_UNBIND_ITEM("API解绑物品"),
    API_BIND_BLOCK("API绑定方块"),
    API_UNBIND_BLOCK("API解绑方块"),
    API_BIND_ENTITY("API绑定实体"),
    API_UNBIND_ENTITY("API解绑实体"),

    COMMAND_BIND_ITEM("命令绑定物品"),
    COMMAND_UNBIND_ITEM("命令解绑物品"),
    COMMAND_BIND_BLOCK("命令绑定方块"),
    COMMAND_UNBIND_BLOCK("命令解绑方块"),
    COMMAND_BIND_ENTITY("命令绑定实体"),
    COMMAND_UNBIND_ENTITY("命令解绑实体"),

    SCANNER_BIND_ITEM("扫描器绑定物品"),
    SCANNER_UNBIND_ITEM("扫描器解绑物品"),

    CLICK_BIND_ITEM("点击物品绑定物品"),
    CLICK_UNBIND_ITEM("点击物品解绑物品"),

    PICKUP_BIND_ITEM("捡起物品绑定物品"),
    PICKUP_UNBIND_ITEM("捡起物品解绑物品"),

    DROP_BIND_ITEM("丢弃物品绑定物品"),
    DROP_UNBIND_ITEM("丢弃物品解绑物品"),

    ITEM_TO_BLOCK_BIND("物品转为方块绑定"),
    ITEM_TO_BLOCK_UNBIND("物品转为方块解绑"),
    BLOCK_TO_ENTITY_BIND("方块转为实体绑定"),
    BLOCK_TO_ENTITY_UNBIND("方块转为实体解绑"),
    BLOCK_MOVE_BIND("方块移动绑定"),
    BLOCK_MOVE_UNBIND("方块移动解绑"),
    ENTITY_TO_ITEM_BIND("实体转为物品绑定"),
    ENTITY_TO_ITEM_UNBIND("实体转为物品解绑"),
    ITEM_TO_ENTITY_BIND("物品转为实体绑定"),
    ITEM_TO_ENTITY_UNBIND("物品转为实体解绑"),
    BLOCK_TO_ITEM_BIND("方块转为物品绑定"),
    BLOCK_TO_ITEM_UNBIND("方块转为物品解绑"),
    ENTITY_TO_BLOCK_BIND("实体转为方块绑定"),
    ENTITY_TO_BLOCK_UNBIND("实体转为方块解绑"),

    USE_BIND_ITEM("消耗耐久物品绑定物品"),
    USE_UNBIND_ITEM("消耗耐久解绑物品"),

    LEFT_BIND_ITEM("拿着物品左键绑定物品"),
    LEFT_UNBIND_ITEM("拿着物品左键解绑物品"),

    RIGHT_BIND_ITEM("拿着物品右键绑定物品"),
    RIGHT_UNBIND_ITEM("拿着物品右键解绑物品"),

    MIGRATION_FROM_LORE_ITEM("从其他插件lore迁移"),
    MIGRATION_FROM_NBT_ITEM("从其他插件NBT迁移"),

    EQUIP_BIND_ITEM("装备物品时绑定"),
    EQUIP_UNBIND_ITEM("装备物品时解绑")
}