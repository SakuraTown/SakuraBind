package top.iseason.bukkit.sakurabind.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import java.util.regex.Pattern

@FilePath("global-setting.yml")
object GlobalSettings : SimpleYAMLConfig() {

    @Key
    @Comment(
        "", "本配置为绑定的全局权限设置，优先级最低",
        "在布尔类型的选项之后加上@则表示对于物主采取相反的结果,部分没有提示消息的无效",
        "如 item-deny.click@: true 表示仅允许物主拿走容器内的物品",
        "如 item-deny.drop: true 表示禁止所有人丢弃绑定物品",
        "如 block-deny.place@: true 表示禁止所有人放置方块，但允许物主放置"
    )
    var global_setting = ""

    @Key
    @Comment("", "显示的lore,玩家名称占位符为 %player%")
    var lore = listOf("&a灵魂绑定: &6%player%")

    @Key
    @Comment("", "显示的lore位置")
    var lore_index = 0

    @Key
    @Comment("", "当物品丢失时(掉虚空、消失等)归还物主(在线则发背包，否则发邮件)")
    var send_when_lost = true

    @Key
    @Comment("", "当容器被破坏时将绑定物品归还物主(在线则发背包，否则发邮件)")
    var send_when_container_break = true

    @Key
    @Comment("", "当物品作为掉落物时立刻归还物主(在线则发背包，否则发邮件)")
    var send_immediately = false

    @Key
    @Comment("", "当物品作为掉落物时延迟多少tick还物主(在线则发背包，否则发邮件), 0则关闭")
    var send_back_delay = 0L

    @Key
    @Comment("", "", "物品禁用设置")
    var item_deny: MemorySection? = null

    @Key("item-deny.interact@")
    @Comment("", "手上拿着绑定物品时禁止交互(点击方块)")
    var item_deny__interact = true

    @Key("item-deny.interact-entity@")
    @Comment("", "禁止实体交互(攻击或右键)")
    var item_deny__interact_entity = true

    @Key
    @Comment("", "禁止丢弃")
    var item_deny__drop = true

    @Key
    @Comment("", "禁止含有绑定物品的容器被玩家破坏")
    var item_deny__container_break = false

    @Key("item-deny.pickup@")
    @Comment("", "禁止捡起")
    var item_deny__pickup = true

    @Key("item-deny.click@")
    @Comment("", "禁止拿走绑定物品")
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
    @Comment("", "掉落物禁止被漏斗或漏斗矿车吸入")
    var item_deny__hopper = true

    @Key
    @Comment("", "容器里的绑定物品不被漏斗或漏斗矿车吸走")
    var item_deny__container_move = true

    @Key
    @Comment("", "禁止放入展示框")
    var item_deny__item_frame = true

    @Key
    @Comment("", "禁止右键丢出(药水、雪球等投掷物)")
    var item_deny__throw = true

    @Key
    @Comment("", "禁止消耗(吃)")
    var item_deny__consume = true

    @Key
    @Comment("", "手上拿着绑定物品时禁止输入一下匹配命令")
    var item_deny__command = false

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
    var block_deny: MemorySection? = null

    @Key("block-deny.break@")
    @Comment("", "禁止方块物品被破坏")
    var block_deny__break = true

    @Key("block-deny.place@")
    @Comment("", "禁止方块物品被放置")
    var block_deny__place = true

    @Key("block-deny.interact@")
    @Comment("", "禁止方块物品被互动(左右键)")
    var block_deny__interact = true

    @Key
    @Comment("", "禁止方块物品被爆炸损坏")
    var block_deny__explode = true

    @Key
    @Comment("", "禁止方块物品被活塞推动/拉动")
    var block_deny__piston = true

    @Key
    @Comment("", "禁止流水/岩浆破坏,如关闭被冲走的绑定物品将送回玩家或发邮件")
    var block_deny__flow = true

    @Key
    @Comment("", "", "自动绑定设置")
    var auto_bind: MemorySection? = null

    @Key
    @Comment("", "是否开启自动绑定,如果全局开启将会绑定所有物品，请在setting.yml中配置开启以绑定特殊物品")
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
    @Comment("", "扫描玩家时如果发现不属于这个玩家的物品则送回去")
    var scanner_send_back = true

    override fun onLoaded(section: ConfigurationSection) {
        itemDenyCommands =
            item_deny__command_pattern.mapNotNull { kotlin.runCatching { Pattern.compile(it) }.getOrNull() }
        itemDenyInventories =
            item_deny__inventory_pattern.mapNotNull { kotlin.runCatching { Pattern.compile(it) }.getOrNull() }

    }
}