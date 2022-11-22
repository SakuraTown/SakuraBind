package top.iseason.bukkit.sakurabind

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.scheduler.BukkitTask
import top.iseason.bukkit.sakuramail.config.SystemMailsYml
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.other.submit

@FilePath("config.yml")
object Config : SimpleYAMLConfig() {

    @Key
    @Comment("", "识别绑定玩家的NBT路径，数据是玩家uuid")
    var nbt_path_uuid = "PublicBukkitValues.sakurabind:sakura_bind"
    var nbtPathUuid = arrayOf<String>()

    @Key
    @Comment("", "识别绑定Lore的NBT路径，数据是玩家uuid")
    var nbt_path_lore = "PublicBukkitValues.sakurabind:sakura_bind_lore"
    var nbtPathLore = arrayOf<String>()

    @Key
    @Comment("", "禁止交互(包括方块物品放置)")
    var denyInteract = true

    @Key
    @Comment("", "禁止实体交互")
    var denyInteractEntity = true

    @Key
    @Comment("", "禁止丢弃")
    var denyDrop = true

    @Key
    @Comment("", "禁止捡起")
    var denyPickup = true

    @Key
    @Comment("", "禁止拿走不属于自己的物品")
    var denyClick = true

    @Key
    @Comment("", "禁止铁砧")
    var denyAnvil = true

    @Key
    @Comment("", "禁止合成")
    var denyCraft = true

    @Key
    @Comment("", "禁止发射器射出")
    var denyDispense = true

    @Key
    @Comment("", "禁止放入展示框")
    var denyItemFrame = true

    @Key
    @Comment("", "禁止消耗")
    var denyConsume = true

    @Key
    @Comment("", "禁止方块物品被其他玩家破坏")
    var block__denyBreak = true

    @Key
    @Comment("", "禁止方块物品被其他玩家互动")
    var block__denyInteract = true

    @Key
    @Comment("", "显示的lore,玩家名称占位符为 %player%")
    var lore = "&a灵魂绑定: &6%player%"

    @Key
    @Comment("", "显示的lore位置")
    var loreIndex = 0

    @Key
    @Comment("", "当物品丢失时(被损坏、消失等)发送邮件给物主")
    var sendLost = true

    @Key
    @Comment("", "当物品作为掉落物时立刻归还物主(在线则发背包，否则发邮件)")
    var sendLostImmediately = true

    @Key
    @Comment(
        "",
        "如果要发送丢失物品邮件",
        "填入SakuraMail的邮件id，丢失物品将会替换邮件的物品",
        "按顺序替换，不够的将会删除, 多余的将会在另外的邮件里"
    )
    var mailId = "bind_mail"

    @Key
    @Comment("", "自动绑定设置")
    var auto_bind = ""

    @Key
    @Comment("", "是否开启")
    var auto_bind__enable = true

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
    @Comment("", "定时扫描所有玩家背包, 此为扫描周期,单位tick，0表示关闭")
    var auto_bind__scanner = 0L
    var task: BukkitTask? = null

    @Key
    @Comment("", "自动绑定的物品材质 https://bukkit.windit.net/javadoc/org/bukkit/Material.html")
    var auto_bind__materials = listOf<String>()


    var abMaterial = hashSetOf<Material>()

    override fun onLoaded(section: ConfigurationSection) {
        nbtPathUuid = nbt_path_uuid.split('.').toTypedArray()
        nbtPathLore = nbt_path_lore.split('.').toTypedArray()
        abMaterial = auto_bind__materials.mapNotNull {
            Material.matchMaterial(it)
        }.toHashSet()
        if (SakuraMailHook.hasHook) {
            SystemMailsYml.getMailYml(mailId) ?: info("&c邮件&7 $mailId &c不存在!")
        }
        task?.cancel()
        if (auto_bind__scanner > 0L && abMaterial.isNotEmpty()) {
            task = submit(period = auto_bind__scanner, async = true) {
                Bukkit.getOnlinePlayers().forEach {
                    val view = it.openInventory
                    for (i in 0 until view.countSlots()) {
                        val item = view.getItem(i) ?: continue
                        if (item.checkAir()) continue
                        if (abMaterial.contains(item.type) && !SakuraBindAPI.hasBind(item)) {
                            SakuraBindAPI.bind(item, it)
                        }
                    }
                }
            }
        }
    }

}