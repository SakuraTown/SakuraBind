package top.iseason.bukkit.sakurabind.utils

enum class SendBackType(var description: String) {
    API("API"),
    COMMON_CALLBACK("callback命令"),
    COMMON_SUPER_CALLBACK("supercallback命令"),
    BLOCK_FROM_TO("方块变化替换"),
    PLAYER_DROP("玩家丢弃"),
    CONTAINER_BREAK("容器破坏"),
    ITEM_DAMAGE("物品实体被毁坏"),
    ITEM_DE_SPAWN("物品实体自然消失"),
    PLAYER_DEATH("玩家死亡"),
    PLAYER_PICKUP("玩家捡起物品"),
    DROP("掉落物保护"),
    CONTAINER_DROP("容器掉落物中的物品保护"),
    SCANNER("扫描器"),
}