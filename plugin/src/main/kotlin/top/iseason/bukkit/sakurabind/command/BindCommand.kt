package top.iseason.bukkit.sakurabind.command

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.command.*
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessages
import top.iseason.bukkittemplate.utils.other.submit

object BindCommand : CommandNode(
    name = "bind",
    description = "绑定某玩家 手上的物品、前面的方块、前面的实体",
    default = PermissionDefault.OP,
    params = listOf(
        Param("<type>", suggest = listOf("item", "block", "entity")),
        Param("<player>", suggestRuntime = ParamSuggestCache.playerParam),
        Param("[setting]", suggestRuntime = { ItemSettings.getSettingsName() }),
        Param("[-noLore]", suggest = listOf("-noLore"))
    ),
    async = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val type = params.next<String>()
        val player = params.next<Player>()
        val showLore = !params.hasParma("-noLore")
        val isSilent = params.hasParma("-silent")
        when (type.lowercase()) {
            "item" -> {
                val itemInMainHand = player.getHeldItem()
                if (itemInMainHand.checkAir()) return@CommandNodeExecutor
                SakuraBindAPI.bind(itemInMainHand!!, player, showLore)
                if (!isSilent) {
                    sender.sendColorMessages(Lang.command__bind_item.formatBy(player.name))
                    MessageTool.bindMessageCoolDown(
                        player,
                        Lang.item_bind_hand,
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
                SakuraBindAPI.bindBlock(targetBlock, player.uniqueId, setting)
                if (!isSilent) {
                    sender.sendColorMessages(Lang.command__bind_block.formatBy(player.name))
                    MessageTool.messageCoolDown(player, Lang.block_bind)
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
                    val settingStr = params.next<String>()
                    val setting = ItemSettings.getSetting(settingStr)
                    SakuraBindAPI.bindEntity(hitEntity, player, setting)
                    if (!isSilent) {
                        sender.sendColorMessages(Lang.command__bind_entity.formatBy(player.name))
                        MessageTool.messageCoolDown(player, Lang.entity_bind)
                    }
                }
            }

            else -> throw ParmaException("未知的绑定类型!")
        }
    }
}