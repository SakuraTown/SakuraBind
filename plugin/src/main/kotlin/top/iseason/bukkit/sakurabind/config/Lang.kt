package top.iseason.bukkit.sakurabind.config

import org.bukkit.configuration.ConfigurationSection
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils

@Key
@FilePath("lang.yml")
object Lang : SimpleYAMLConfig() {
    @Comment(
        "",
        "消息留空将不会显示，使用 '\\n' 可以换行",
        "支持 & 颜色符号，1.17以上支持16进制颜色代码，如 #66ccff",
        "{0}、{1}、{2}、{3} 等格式为该消息独有的变量占位符",
        "所有消息支持PlaceHolderAPI"
    )
    var readme = ""

    var prefix = "&a[&6${BukkitTemplate.getPlugin().description.name}&a] &f"
    var send_back_all = "&7你的遗失物品已全部放入你的背包"
    var send_back_inventory = "&7你的部分遗失物品已放入你的背包"
    var send_back_ender_chest = "&7你的部分遗失物品已放入你的末影箱"
    var item_bind_hand = "&7你手上的物品已绑定"
    var block_bind = "&7你前面的方块已绑定"
    var entity_bind = "&7你前面的实体已绑定"
    var item_bind_all = "&7你的背包物品已绑定"
    var item_unbind_hand = "&7你手上的物品已解绑"
    var block_unbind = "&7你手上的物品已解绑"
    var entity_unbind = "&7你手上的物品已解绑"
    var item_unbind_all = "&7你的背包物品已解绑"
    var entity_bind_on_spawner_egg = "&6你的生物已绑定!"

    var item__deny_command = "&6你不能拿着此物品输入该命令!"
    var item__deny_drop = "&6该物品不能丢出!"
    var item__deny_pickup = "&6该物品属于 &a{0},&6已归还"
    var item__deny_itemFrame = "&6该物品禁止放入展示框!"
    var item__deny_interact = "&6该物品禁止交互!"
    var item__deny_armor_stand_set = "&6该物品禁止放入盔甲架!"
    var item__deny_armor_stand_get = "&6该物品禁止从盔甲架取出!"
    var item__deny_entity_interact = "&6该物品禁止与实体交互!"
    var item__deny_click = "&6你不能拿走这个物品!"
    var item__deny_inventory = "&6此物品禁止放入这个容器!"
    var item__deny_throw = "&6该物品禁止投掷!"
    var item__deny_consume = "&6该物品禁止消耗!"
    var item__deny_craft = "&6该物品禁止用于合成!"
    var item__deny_anvil = "&6该物品禁止用于铁砧!"
    var item__deny_container_break = "&6该容器含有绑定物品，禁止破坏!"

    var auto_bind__onClick = "&a此物品已绑定你的灵魂!"
    var auto_bind__onPickup = "&a你捡到了与你相性最好的物品!"
    var auto_bind__onDrop = "&a你刚刚丢弃的物品将永远属于你!"
    var auto_bind__onScanner = "&a你背包有适合你的物品，已绑定你的灵魂!"

    var block__deny_break = "&6你不能破坏这个方块,这个方块属于 &b{0}"
    var block__deny_place = "&6你不能放置这个方块，此物物品已绑定"
    var block__deny_interact = "&6你没有这个方块的使用权限,这个方块属于 &b{0}"

    var entity__deny_damage = "&6你不能伤害这个实体，这个实体属于 &b{0}"
    var entity__deny_interact = "&6你没有这个实体的交互,这个实体属于 &b{0}"

    var scanner_item_send_back = "&6检测到你的背包存在别人的物品，已归还物主!"

    @Comment("", "命令消息")
    var command = ""
    var command__bind_item = "&a已绑定 &b{0} &a手上的物品"
    var command__bind_block = "&a已绑定 &b{0} &a前面的方块"
    var command__bind_entity = "&a已绑定 &b{0} &a前面的实体"
    var command__bindTo_item = "&a已将手上的物品绑定至 &b{0}"
    var command__bindTo_block = "&a已将前方的方块绑定至 &b{0}"
    var command__bindTo_entity = "&a已将前方的实体绑定至 &b{0}"
    var command__unbind_item = "&a已将 &b{0} 手上的物品解绑"
    var command__unbind_block = "&a已将 &b{0} 前方的方块解绑"
    var command__unbind_entity = "&a已将 &b{0} 前方的实体解绑"
    var command__bindAll = "&a已绑定 &b{0} 整个背包的物品"
    var command__unbindAll = "&a已解绑 &b{0} 整个背包的物品"
    var command__getLost_item = "&a你领取了 &b{0} &a个遗失物品,请腾出空间领取剩余物品!"
    var command__getLost_empty = "&6没有遗失物品!"
    var command__getLost_full = "&6背包空间不足!"
    var command__getLost_all = "&a你领取了所有的遗失物品! 共 &b{0} &a个物品"
    var command__getLost_coolDown = "&6你输入得太快了!"
    var command__openLost_empty = "&6该玩家没有遗失的物品!"
    var command__openLost_page_not_exist = "&6没有这么多页,最多{0}页!"
    var command__openLost_open = "&6你打开了玩家 {0} 的暂存箱第 {1} 页"
    var command__openLost_closed = "&6玩家 {0} 暂存箱已更新"
    var command__select_on =
        "&a已经开启目标选择模式,右键选择 物品|方块|实体 \n&6输入/sakurabind select bind 确认绑定 \n&6再次输入命令取消选择"
    var command__select_off = "&6已经开启目标选择模式已关闭"
    var command__select_select_item = "&6你选择了手上的物品 {0} x {1}"
    var command__select_select_block = "&6你选择了点击的方块 {0} ({1},{2},{3})"
    var command__select_select_entity = "&6你选择了点击的实体 {0} ({1},{2},{3})"
    var command__select_has_bind = "&6该目标已经绑定，无法选择"
    var command__select_no_selected = "&6你没有选择目标!"
    var command__select_bind = "&6已绑定目标!"
    var command__select_timeout = "&6长时间未确认绑定，已退出选择模式!"
    var command__autoBind = "&a已添加 &b{0}"
    var command__debug = "&aDebug模式: &b{0}"
    var has_lost_item = "&a你有遗失的物品,请输入 &6'/sakurabind getLost' &a领取"
    var lost_item_send_when_online = "&a你有遗失的物品,请输入 &6'/sakurabind getLost' &a领取"

    override fun onLoaded(section: ConfigurationSection) {
        MessageUtils.defaultPrefix = prefix
        SimpleLogger.prefix = prefix
    }
}