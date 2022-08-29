package top.iseason.bukkit.sakurabind

import org.bukkit.configuration.ConfigurationSection
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.Comment
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key

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

    override val onLoaded: ConfigurationSection.() -> Unit = {

    }

}