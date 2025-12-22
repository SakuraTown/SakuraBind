package top.iseason.bukkit.sakurabind.config.module

import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key

@FilePath("modules/retrieve.yml")
object RetrieveConfig : SimpleYAMLConfig() {
    @Key
    @Comment(
        "",
        "找回/取回物品模块",
        "为绑定物品添加UUID标记并储存到数据库，玩家可以通过ui取回绑定物品(重新生成UUID)",
        "找回物品之后原物品将标记为废弃，当发现、使用原物品时将自动删除，从而实现任意位置找回的功能",
        "",

        )
    var readme = ""

    @Key
    @Comment("", "是否启用，重启生效")
    var enable = false

    @Key("nbt-path")
    @Comment("", "识别/储存UUID的NBT路径, 如果修改，现有的物品将失效, 但仍可以找回")
    var nbtPath = "sakura_bind_retrieve"

}