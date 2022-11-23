package top.iseason.bukkit.sakurabind.config

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.hook.SakuraMailHook
import top.iseason.bukkit.sakuramail.config.SystemMailsYml
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.other.submit
import java.util.*
import java.util.regex.Pattern

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
    @Comment("", "显示的lore,玩家名称占位符为 %player%")
    var lore = "&a灵魂绑定: &6%player%"

    @Key
    @Comment("", "显示的lore位置")
    var lore_index = 0

    @Key
    @Comment("", "当物品丢失时(掉虚空、消失等)归还物主(在线则发背包，否则发邮件)")
    var send_when_lost = true

    @Key
    @Comment("", "当物品作为掉落物时立刻归还物主(在线则发背包，否则发邮件)")
    var send_immediately = false

    @Key
    @Comment("", "遗失物品使用 SakuraMail 发送而不是暂存箱")
    var sakuraMail_hook = false

    @Key
    @Comment(
        "",
        "如果要发送丢失物品邮件",
        "填入SakuraMail的邮件id，丢失物品将会替换邮件的物品",
        "按顺序替换，不够的将会删除, 多余的将会在另外的邮件里"
    )
    var mailId = "bind_mail"


    @Key
    @Comment("", "", "物品禁用设置")
    var item_deny = true

    @Key
    @Comment("", "手上拿着绑定物品时禁止交互(包括方块物品放置)")
    var item_deny__interact = true

    @Key
    @Comment("", "允许物主交互(包括方块物品放置),deny-interact 需为true")
    var item_deny__interact_allow_owner = true

    @Key
    @Comment("", "非物主不能实体交互")
    var item_deny__interact_entity = true

    @Key
    @Comment("", "禁止丢弃")
    var item_deny__drop = true

    @Key
    @Comment("", "禁止捡起")
    var item_deny__pickup = true

    @Key
    @Comment("", "禁止拿走不属于自己的物品")
    var item_deny__click = true

    @Key
    @Comment("", "禁止铁砧(1.9以上)")
    var item_deny__anvil = true

    @Key
    @Comment("", "禁止合成")
    var item_deny__craft = true

    @Key
    @Comment("", "禁止发射器射出")
    var item_deny__dispense = true

    @Key
    @Comment("", "禁止放入展示框")
    var item_deny__itemFrame = true

    @Key
    @Comment("", "禁止消耗(吃)")
    var item_deny__consume = true

    @Key
    @Comment("", "手上拿着绑定物品时禁止输入命令")
    var item_deny__command = true

    @Key
    @Comment("", "允许物主输入命令,deny-interact 需为true")
    var item_deny__command_allow_owner = false

    @Key
    @Comment("", "匹配的命令正则表达式: '.*' 表示全部。测试: https://www.bejson.com/othertools/regex/")
    var item_deny__command_pattern = listOf(".*")

    var itemDenyCommands = emptyList<Pattern>()

    @Key
    @Comment("", "禁止绑定物品放入特定标题的容器里")
    var item_deny__inventory = true

    @Key
    @Comment("", "禁止绑定物品放入特定标题的容器里,正则表达式")
    var item_deny__inventory_pattern = listOf("^垃圾桶$")

    var itemDenyInventories = emptyList<Pattern>()

    @Key
    @Comment("", "", "方块物品相关设置", "由于监听方块物品需要较多的资源，如果不绑定方块物品关闭以节省性能")
    var block = true

    @Key
    @Comment("", "方块物品检测开关，需要重启生效")
    var block__enable = true

    @Key
    @Comment("", "禁止方块物品被其他玩家破坏")
    var block__deny_break = true

    @Key
    @Comment("", "禁止方块物品被其他玩家互动")
    var block__deny_interact = true

    @Key
    @Comment("", "禁止方块物品被爆炸损坏")
    var block__deny_explode = true

    @Key
    @Comment("", "禁止方块物品被活塞推动/拉动")
    var block__deny_piston = true

    @Key
    @Comment("", "禁止流水/岩浆破坏,如关闭被冲走的绑定物品将送回玩家或发邮件")
    var block__deny_flow = true

    @Key
    @Comment("", "", "自动绑定设置")
    var auto_bind = ""

    @Key
    @Comment("", "是否开启")
    var auto_bind__enable = false

    @Key
    @Comment("", "点击物品时绑定")
    var auto_bind__onClick = true

    @Key
    @Comment("", "捡起物品时绑定")
    var auto_bind__onPickup = true

    @Key
    @Comment("", "丢弃物品时绑定")
    var auto_bind__onDrop = false

    @Key
    @Comment("", "定时扫描所有玩家背包(materials不为空才会开启), 此为扫描周期,单位tick，0表示关闭")
    var auto_bind__scanner = 0L

    @Key
    @Comment("", "扫描玩家时如果发现不属于这个玩家的物品则送回去")
    var auto_bind__scanner_sendBack = false

    var task: BukkitTask? = null

    @Key
    @Comment("", "自动绑定的物品材质 https://bukkit.windit.net/javadoc/org/bukkit/Material.html")
    var auto_bind__materials = listOf<String>()


    var abMaterial = hashSetOf<Material>()
        private set

    override fun onLoaded(section: ConfigurationSection) {
        nbtPathUuid = nbt_path_uuid.split('.').toTypedArray()
        nbtPathLore = nbt_path_lore.split('.').toTypedArray()

        itemDenyCommands =
            item_deny__command_pattern.mapNotNull { kotlin.runCatching { Pattern.compile(it) }.getOrNull() }
        itemDenyInventories =
            item_deny__inventory_pattern.mapNotNull { kotlin.runCatching { Pattern.compile(it) }.getOrNull() }

        abMaterial = auto_bind__materials.mapNotNull {
            Material.matchMaterial(it)
        }.toHashSet()

        if (SakuraMailHook.hasHooked) {
            SystemMailsYml.getMailYml(mailId) ?: info("&c邮件&7 $mailId &c不存在!")
        }
        task?.cancel()
        if (auto_bind__scanner > 0L && abMaterial.isNotEmpty()) {
            task = submit(period = auto_bind__scanner, async = true) {
                val mutableMapOf = mutableMapOf<UUID, MutableList<ItemStack>>()
                Bukkit.getOnlinePlayers().forEach {
                    val inventory = it.openInventory.bottomInventory
                    try {
                        //为了兼容mod，获取到的格子数不一致
                        for (i in 0 until inventory.size) {
                            val item = inventory.getItem(i) ?: continue
                            if (item.checkAir()) continue
                            val owner = SakuraBindAPI.getOwner(item)
                            if (SakuraMailHook.hasHooked && auto_bind__scanner_sendBack && owner != null && owner != it.uniqueId) {
                                mutableMapOf.computeIfAbsent(owner) { mutableListOf() }.add(item)
                                inventory.setItem(i, null)
                                continue
                            }
                            if (abMaterial.contains(item.type) && owner == null) {
                                SakuraBindAPI.bind(item, it)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
                if (SakuraMailHook.hasHooked && auto_bind__scanner_sendBack && mutableMapOf.isNotEmpty()) {
                    mutableMapOf.forEach { (uid, list) ->
                        SakuraBindAPI.sendBackItem(uid, list)
                    }
                }
            }
        } else task = null
    }

}