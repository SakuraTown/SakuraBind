package top.iseason.bukkit.sakurabind.config

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.hook.SakuraMailHook
import top.iseason.bukkit.sakuramail.config.SystemMailsYml
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.submit
import java.util.*

@FilePath("config.yml")
object Config : SimpleYAMLConfig() {

    @Key
    @Comment("", "识别绑定玩家的NBT路径，数据是玩家uuid")
    var nbt_path_uuid = "PublicBukkitValues.sakurabind:sakura_bind"
    var nbtPathUuid = arrayOf<String>()

    @Key
    @Comment("", "识别绑定Lore的NBT路径，数据是玩家旧的lore")
    var nbt_path_lore = "PublicBukkitValues.sakurabind:sakura_bind_lore"
    var nbtPathLore = arrayOf<String>()

    @Key
    @Comment("", "遗失物品使用 SakuraMail 发送而不是暂存箱")
    var sakuraMail_hook = false

    @Key
    @Comment("", "登入时如果暂存箱有物品则提醒，此为延迟，单位tick, 设置小于0以关闭提示")
    var login_message_delay = 100L

    @Key
    @Comment(
        "",
        "如果要发送丢失物品邮件",
        "填入SakuraMail的邮件id，丢失物品将会替换邮件的物品",
        "按顺序替换，不够的将会删除, 多余的将会在另外的邮件里"
    )
    var mailId = "bind_mail"

    @Key
    @Comment("", "方块物品检测开关，需要重启生效。打开才能支持方块物品，同时性能损耗也会增加")
    var block_listener = true

    @Key
    @Comment(
        "",
        "定时扫描所有玩家背包(materials不为空才会开启), 此为扫描周期,单位tick，0表示关闭",
        "此项关闭将影响 scanner开头的设置"
    )
    var scanner_period = 60L

    private var task: BukkitTask? = null

    @Key
    @Comment("", "识别到此NBT就自动绑定物主")
    var auto_bind_nbt = "sakura_auto_bind"

    override fun onLoaded(section: ConfigurationSection) {
        nbtPathUuid = nbt_path_uuid.split('.').toTypedArray()
        nbtPathLore = nbt_path_lore.split('.').toTypedArray()

        if (SakuraMailHook.hasHooked) {
            SystemMailsYml.getMailYml(mailId) ?: info("&c邮件&7 $mailId &c不存在!")
        }
        task?.cancel()
        if (scanner_period > 0L && (DatabaseConfig.isConnected || (SakuraMailHook.hasHooked && sakuraMail_hook))) {
            info("&a定时扫描任务已启动,周期: $scanner_period tick")
            task = submit(period = scanner_period, async = true) {
                val mutableMapOf = mutableMapOf<UUID, MutableList<ItemStack>>()
                Bukkit.getOnlinePlayers().forEach {
                    if (checkByPass(it)) return@forEach
//                    info("正在检查 ${it.name} ${it.uniqueId} 的背包")
//                    info("送回物品功能: $auto_bind__scanner_send_back")
                    var hasFound = false
                    val inventory = it.openInventory.bottomInventory
                    try {
                        //为了兼容mod，获取到的格子数不一致
                        for (i in 0 until inventory.size) {
                            val item = inventory.getItem(i) ?: continue
                            if (item.checkAir()) continue
                            val owner = SakuraBindAPI.getOwner(item)
                            val setting = ItemSettings.getSetting(item, owner != null)
                            if (setting.getBoolean(
                                    "scanner-send-back",
                                    owner.toString(),
                                    it
                                ) && owner != null && owner != it.uniqueId
                            ) {
//                                info("找到一个违规物品${item.type} 属于 ${owner}")
                                mutableMapOf.computeIfAbsent(owner) { mutableListOf() }.add(item)
                                inventory.setItem(i, null)
                                hasFound = true
                                continue
                            }
                            if (owner == null &&
                                (setting.getBoolean("auto-bind.enable") || NBTEditor.contains(
                                    item, auto_bind_nbt
                                ))
                            ) {
//                                info("已绑定物品 ${item.type}")
                                SakuraBindAPI.bind(item, it)
                            }
                        }
                    } catch (_: Exception) {
                    }
                    if (hasFound) it.sendColorMessage(Lang.scanner_item_send_back)
                }
                if (mutableMapOf.isNotEmpty()) {
//                    info("正在送回物品")
                    mutableMapOf.forEach { (uid, list) ->
                        SakuraBindAPI.sendBackItem(uid, list)
                    }
                }
            }
        } else task = null
    }

    /**
     * 检查是否不检查
     */
    fun checkByPass(player: HumanEntity): Boolean {
        if (player.isOp || player.hasPermission("sakurabind.bypass.all")) return true
        return false
    }
}