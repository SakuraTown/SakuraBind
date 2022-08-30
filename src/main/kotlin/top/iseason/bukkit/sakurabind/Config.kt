package top.iseason.bukkit.sakurabind

import org.bukkit.configuration.ConfigurationSection
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.Comment
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key
import top.iseason.bukkit.bukkittemplate.debug.info
import top.iseason.bukkit.sakuramail.config.SystemMailsYml

@FilePath("config.yml")
object Config : SimpleYAMLConfig() {

    @Key
    @Comment("", "禁止交互")
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
    @Comment("", "禁止消耗")
    var denyConsume = true


    @Key
    @Comment("", "显示的lore,玩家名称占位符为 %player%")
    var lore = "&a灵魂绑定: &6%player%"

    @Key
    @Comment("", "显示的lore位置")
    var loreIndex = 0

    @Key
    @Comment("", "当物品丢失时发送邮件给物主")
    var sendLost = true

    @Key
    @Comment(
        "",
        "如果要发送丢失物品邮件",
        "填入SakuraMail的邮件id，丢失物品将会替换邮件的物品",
        "按顺序替换，不够的将会删除, 多余的将会在另外的邮件里"
    )
    var mailId = "bind_mail"

    override val onLoaded: ConfigurationSection.() -> Unit = {
        if (SakuraMailHook.hasHook) {
            SystemMailsYml.getMailYml(mailId) ?: info("&c邮件&7 $mailId &c不存在!")
        }
    }

}