package top.iseason.bukkit.sakurabind.logger

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
}