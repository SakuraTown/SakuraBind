package top.iseason.bukkit.sakurabind.config

import org.bukkit.configuration.MemorySection
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key

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
    @Comment("", "", "物品设置")
    var item: MemorySection? = null

    @Key
    @Comment("", "显示的lore,玩家名称占位符为 %player%")
    var item__lore = listOf("&a灵魂绑定: &6%player%")

    @Key
    @Comment("", "显示的lore位置")
    var item__lore_index = 0

    @Key
    @Comment("", "如果删除匹配的lore则以绑定lore替换之(覆盖lore-index)")
    var item__lore_replace_matched = true

    @Key
    @Comment("", "当物品丢失时(掉虚空、消失等)归还物主(在线则发背包，否则发邮件)")
    var item__send_when_lost = true

    @Key
    @Comment("", "当容器被破坏时将绑定物品归还物主(在线则发背包，否则发邮件)")
    var item__send_when_container_break = true

    @Key
    @Comment("", "当捡到不属于你的绑定物品时是否送回原物主, 前提是 item-deny.pickup 开启")
    var item__send_back_on_pickup = true

    @Key
    @Comment("", "当物品作为掉落物时延迟多少tick还物主(在线则发背包，否则发邮件), 0表示立马返回，-1关闭")
    var item__send_back_delay = -1L

    @Key
    @Comment("", "当扫描器扫描物品时如果发现物品不是该玩家的就返还物主")
    var item__send_back_scanner = true

    @Key
    @Comment("", "", "物品禁用设置")
    var item_deny: MemorySection? = null

    @Key("item-deny.interact-left@")
    @Comment("", "手上拿着绑定物品时禁止左键交互")
    var item_deny__interact_left = true

    @Key("item-deny.interact-right@")
    @Comment("", "手上拿着绑定物品时禁止右键交互")
    var item_deny__interact_right = true

    @Key("item-deny.interact-entity@")
    @Comment("", "禁止实体交互(攻击或右键)")
    var item_deny__interact_entity = true

    @Key("item-deny.armor-stand@")
    @Comment("", "禁止盔甲架交互")
    var item_deny__armor_stand = true

    @Key
    @Comment("", "禁止丢弃")
    var item_deny__drop = false

    @Key
    @Comment("", "禁止含有绑定物品的容器被玩家破坏")
    var item_deny__container_break = false

    @Key()
    @Comment("", "禁止捡起")
    var item_deny__pickup = false

    @Key()
    @Comment("", "禁止拿走绑定物品")
    var item_deny__click = false

    @Key
    @Comment("", "禁止铁砧(1.9以上)")
    var item_deny__anvil = false

    @Key
    @Comment("", "禁止合成")
    var item_deny__craft = false

    @Key
    @Comment("", "禁止发射器射出")
    var item_deny__dispense = false

    @Key
    @Comment("", "掉落物禁止被漏斗或漏斗矿车吸入")
    var item_deny__hopper = false

    @Key
    @Comment("", "容器里的绑定物品不被漏斗或漏斗矿车吸走")
    var item_deny__container_move = false

    @Key
    @Comment("", "禁止放入展示框")
    var item_deny__item_frame = false

    @Key
    @Comment("", "禁止弹射物射出(药水、箭、雪球等)")
    var item_deny__throw = false

    @Key
    @Comment("", "禁止消耗(吃)")
    var item_deny__consume = false

    @Key
    @Comment("", "手上拿着绑定物品时禁止输入一下匹配命令")
    var item_deny__command = false

    @Key
    @Comment("", "匹配的命令正则表达式: '.*' 表示全部。测试: https://www.bejson.com/othertools/regex/")
    var item_deny__command_pattern = listOf(".*")

    @Key
    @Comment("", "禁止绑定物品放入特定标题的容器里,为了防止各种操作绕过将同时也会禁止点击")
    var item_deny__inventory = false

    @Key
    @Comment("", "禁止绑定物品放入特定标题的容器里,正则表达式")
    var item_deny__inventory_pattern = listOf("^垃圾桶$")

    @Key
    @Comment(
        "",
        "禁止绑定物品放入特定类型的容器里, https://bukkit.windit.net/javadoc/org/bukkit/event/inventory/InventoryType.html"
    )
    var item_deny__inventory_types = listOf("ANVIL", "DISPENSER", "DROPPER", "FURNACE", "GRINDSTONE", "SMITHING")

    @Key
    @Comment("", "禁止绑定物品死亡掉落(只在死亡不掉落游戏规则未开启时有效)")
    var item_deny__drop_on_death = false

    @Key
    @Comment("", "", "方块禁用设置", "由于监听方块物品需要较多的资源，如果不绑定方块物品关闭以节省性能")
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
    var block_deny__explode = false

    @Key
    @Comment("", "禁止方块物品被活塞推动/拉动. 建议禁止, 如果不禁止，被活塞推/拉动后的方块也会绑定(实验性功能)")
    var block_deny__piston = false

    @Key
    @Comment("", "禁止流水/岩浆破坏, 被岩浆破坏的物品将不会有掉落物,故而设置为false时将会把物品直接送回物主")
    var block_deny__flow = false

    @Key
    @Comment("", "禁止绑定的方块转化为实体，比如重力方块变成下落方块，tnt被点燃")
    var block_deny__change_to_entity = false

    @Key
    @Comment("", "", "实体设置")
    var entity: MemorySection? = null

    @Key("entity.bind-name")
    @Comment("", "绑定的生物的名字, {0} 为玩家名 {1} 为实体名")
    var entity__bind_name = "&a{0} &f的 &7{1}"

    @Key()
    @Comment("", "绑定实体死亡掉落物也绑定")
    var entity__bind_drops = false

    @Key("entity.hostility@")
    @Comment("", "是否对敌对目标")
    var entity__hostility = true

    @Key()
    @Comment("", "是否守护非敌对目标(吸引仇恨)")
    var entity__defend = false

    @Key()
    @Comment("", "绑定的实体对友好目标的守护距离")
    var entity__defend_distance = 10.0

    @Key()
    @Comment("", "是否启用刷怪蛋检测, 启用之后绑定的刷怪蛋生成的生物会绑定")
    var entity__spawn_egg_check = false

    @Key
    @Comment("", "", "由绑定物品生成的实体的监听器", "一般指刷怪蛋")
    var entity_deny: MemorySection? = null

    @Key()
    @Comment("", "是否禁止该实体被除了玩家之外的实体攻击掉血")
    var entity_deny_damage_by_entity = false

    @Key("entity-deny.damage-by-player@")
    @Comment("", "是否禁止该实体被玩家攻击掉血")
    var entity_deny_damage_by_player = true

    @Key()
    @Comment("", "是否禁止该实体受到任何伤害")
    var entity_deny_damage = false

    @Key("entity-deny.interact@")
    @Comment("", "是否禁止与该实体交互(右键)")
    var entity_deny_interact = true

    @Key()
    @Comment("", "是否禁用实体AI (1.9+)")
    var entity_deny_ai = false

    @Key()
    @Comment("", "是否禁用实体重力 (1.10+)")
    var entity_deny_gravity = false

    @Key()
    @Comment("", "禁止绑定实体死亡掉落物")
    var entity_deny_drops = false


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
    @Comment("", "使用物品消耗耐久时绑定(包括工具、武器、盔甲等有耐久的物品)")
    var auto_bind__onUse = false

    @Key
    @Comment("", "手拿物品左键时绑定")
    var auto_bind__onLeft = false

    @Key
    @Comment("", "手拿物品右键时绑定")
    var auto_bind__onRight = false

    @Key
    @Comment("", "扫描器扫描时绑定(在config.yml中配置扫描器)")
    var auto_bind__onScanner = true

    @Key
    @Comment("", "", "自动解绑设置(前提是已经绑定)")
    var auto_unbind: MemorySection? = null

    @Key
    @Comment("", "是否开启自动解绑,如果全局开启将会解绑所有物品，请在setting.yml中配置开启以解绑特殊物品")
    var auto_unbind__enable = false

    @Key
    @Comment("", "点击物品时解绑")
    var auto_unbind__onClick = false

    @Key
    @Comment("", "捡起物品时解绑")
    var auto_unbind__onPickup = false

    @Key
    @Comment("", "丢弃物品时解绑")
    var auto_unbind__onDrop = false

    @Key
    @Comment("", "使用物品消耗耐久时解绑(包括工具、武器、盔甲等有耐久的物品)")
    var auto_unbind__onUse = false

    @Key
    @Comment("", "手拿物品左键时解绑")
    var auto_unbind__onLeft = false

    @Key
    @Comment("", "手拿物品右键时解绑")
    var auto_unbind__onRight = false

    @Key
    @Comment("", "扫描器扫描时解绑(在config.yml中配置扫描器)")
    var auto_unbind__onScanner = false


}