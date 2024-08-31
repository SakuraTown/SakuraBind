package top.iseason.bukkit.sakurabind.command


import de.tr7zw.nbtapi.utils.MinecraftVersion
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.command.*
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.other.submit

object BindToCommand : CommandNode(
    name = "bindTo",
    description = "绑定 手上的物品、前方的方块、前方的实体 给某玩家",
    default = PermissionDefault.OP,
    params = listOf(
        Param("<type>", suggest = listOf("item", "block", "entity")),
        Param("<player>", suggestRuntime = ParamSuggestCache.playerParam),
        Param("[setting]", suggestRuntime = { ItemSettings.getSettingsName() }),
        Param("[-noLore]", suggest = listOf("-noLore"))
    ),
    isPlayerOnly = true,
    async = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = sender as Player
        val type = params.next<String>()
        val target = params.next<Player>()
        val showLore = !params.hasParma("-noLore")
        val isSilent = params.hasParma("-silent")
        when (type.lowercase()) {
            "item" -> {
                val itemInMainHand = player.getHeldItem() ?: throw ParmaException("请拿着物品")
                SakuraBindAPI.bind(itemInMainHand, target, showLore, BindType.COMMAND_BIND_ITEM)
                if (!isSilent) {
                    MessageTool.bindMessageCoolDown(
                        player,
                        Lang.command__bindTo_item.formatBy(target.name),
                        ItemSettings.getSetting(itemInMainHand),
                        itemInMainHand
                    )
                }
            }

            "block" -> {
                val targetBlock = player.getTargetBlock(null, 5)
                if (targetBlock == null || targetBlock.isEmpty) throw ParmaException("目标前方没有一个有效的方块")
                val settingStr = params.next<String>()
                val setting = ItemSettings.getSetting(settingStr)
                SakuraBindAPI.bindBlock(targetBlock, target.uniqueId, setting, BindType.COMMAND_BIND_BLOCK)
                MessageTool.messageCoolDown(player, Lang.command__bindTo_block.formatBy(target.name))
            }

            "entity" -> {
                if (!MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_13_R1)) {
                    throw ParmaException("实体绑定命令只在1.13或以上生效")
                }
                val eyeLocation = player.eyeLocation
                val direction = player.eyeLocation.direction
                submit {
                    val rayTraceEntities =
                        player.world.rayTraceEntities(eyeLocation.clone().add(direction), direction, 5.0)
                            ?: return@submit
                    val hitEntity = rayTraceEntities.hitEntity ?: return@submit
                    val settingStr = params.next<String>()
                    val setting = ItemSettings.getSetting(settingStr)
                    SakuraBindAPI.bindEntity(hitEntity, target, setting, BindType.COMMAND_BIND_ENTITY)
                    MessageTool.messageCoolDown(player, Lang.command__bindTo_entity.formatBy(target.name))
                }
            }

            else -> throw ParmaException("未知的绑定类型!")
        }
    }
}