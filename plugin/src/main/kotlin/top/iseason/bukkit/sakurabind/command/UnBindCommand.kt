package top.iseason.bukkit.sakurabind.command

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.command.*
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.submit

object UnBindCommand : CommandNode(
    name = "unBind",
    description = "解绑定某玩家手上的物品、前面的方块、前面的实体",
    default = PermissionDefault.OP,
    params = listOf(
        Param("<type>", suggest = listOf("item", "block", "entity")),
        Param("<player>", suggestRuntime = ParamSuggestCache.playerParam)
    ),
    async = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val type = params.next<String>()
        val player = params.next<Player>()
        val isSilent = params.hasParma("-silent")

        when (type.lowercase()) {
            "item" -> {
                val itemInMainHand = player.getHeldItem()
                if (itemInMainHand.checkAir()) return@CommandNodeExecutor
                if (!SakuraBindAPI.hasBind(itemInMainHand!!)) throw ParmaException(Lang.command__unbind_not_bind)
                SakuraBindAPI.unBind(itemInMainHand, BindType.COMMAND_UNBIND_ITEM)
                if (!isSilent) {
                    sender.sendColorMessage(Lang.command__unbind_item.formatBy(player.name))
                    MessageTool.messageCoolDown(player, Lang.item_unbind_hand)
                }
            }

            "block" -> {
                val targetBlock = player.getTargetBlock(null, 5)
                if (targetBlock == null || targetBlock.isEmpty) throw ParmaException("目标前方没有一个有效的方块")
                if (SakuraBindAPI.getBlockOwner(targetBlock) == null) throw ParmaException(Lang.command__unbind_not_bind)
                SakuraBindAPI.unbindBlock(targetBlock, BindType.COMMAND_UNBIND_BLOCK)
                if (!isSilent) {
                    sender.sendColorMessage(Lang.command__unbind_block.formatBy(player.name))
                    MessageTool.messageCoolDown(player, Lang.block_unbind)
                }
            }

            "entity" -> {
                if (!NBTEditor.getMinecraftVersion().greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_13)) {
                    throw ParmaException("实体绑定命令只在1.13或以上生效")
                }
                val eyeLocation = player.eyeLocation
                val direction = player.eyeLocation.direction
                submit {
                    val rayTraceEntities =
                        player.world.rayTraceEntities(eyeLocation.clone().add(direction), direction, 5.0)
                            ?: return@submit
                    val hitEntity = rayTraceEntities.hitEntity ?: return@submit
                    if (SakuraBindAPI.getEntityOwner(hitEntity) == null) throw ParmaException(Lang.command__unbind_not_bind)
                    SakuraBindAPI.unbindEntity(hitEntity, BindType.COMMAND_UNBIND_ENTITY)
                    if (!isSilent) {
                        sender.sendColorMessage(Lang.command__unbind_entity.formatBy(player.name))
                        MessageTool.messageCoolDown(player, Lang.entity_unbind)
                    }
                }
            }

            else -> throw ParmaException("未知的绑定类型!")
        }
    }
}