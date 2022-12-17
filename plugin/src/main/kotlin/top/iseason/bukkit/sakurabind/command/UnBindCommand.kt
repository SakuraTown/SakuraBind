package top.iseason.bukkit.sakurabind.command

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.command.*
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
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
                SakuraBindAPI.unBind(itemInMainHand!!)
                if (!isSilent) {
                    MessageTool.messageCoolDown(player, Lang.command__unbind_item)
                }
            }

            "block" -> {
                val targetBlock = player.getTargetBlock(null, 5)
                if (targetBlock == null || targetBlock.isEmpty) throw ParmaException("目标前方没有一个有效的方块")
                SakuraBindAPI.unbindBlock(targetBlock)
                if (!isSilent) {
                    MessageTool.messageCoolDown(player, Lang.command__unbind_block)
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
                    SakuraBindAPI.unbindEntity(hitEntity)
                    if (!isSilent) {
                        MessageTool.messageCoolDown(player, Lang.command__unbind_entity)
                    }
                }
            }

            else -> throw ParmaException("未知的绑定类型!")
        }
    }
}