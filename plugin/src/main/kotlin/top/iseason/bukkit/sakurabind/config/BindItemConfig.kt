package top.iseason.bukkit.sakurabind.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key

@FilePath("modules/bind-item.yml")
object BindItemConfig : SimpleYAMLConfig() {

    @Key
    @Comment(
        "",
        "绑定物品，使用一个物品来绑定/解绑物品",
        "将绑定物品放在指针上,点击需要绑定的物品完成绑定",
        "你需要给物品设置特殊的NBT才能将物品标记为绑定物品, 键是下面配置的 bind.nbt ,值是绑定配置, 无匹配将自动识别",
        "",
        "给物品下面的配置 unbind.nbt 的nbt 标记物品为解绑物品",
        "",
        "你可以可以使用命令 '/sakurabind autoBind 键 值' 的方式来快速给物品加上nbt",
        "如果是默认配置就是 '/sakurabind autoBind sakura_bind_bind-item' 标记物品为绑定物品, 绑定配置自动识别",
    )
    var readme = ""

    @Key
    @Comment("", "功能总开关，重启生效")
    var enable = false

    @Key
    @Comment("", "绑定物品")
    var bind: MemorySection? = null

    @Key("bind.nbt")
    @Comment("", "识别配置的NBT路径,'.'为分隔符")
    var bindNbt = "sakura_bind_bind-item"
    var bindPath = arrayOf("sakura_bind_bind-item")
        private set

    @Key("bind.chance")
    @Comment("", "成功率, 每次绑定都会消耗一个物品")
    var bindChance = 100.0

    @Key
    @Comment("", "解绑物品")
    var unbind: MemorySection? = null

    @Key("unbind.nbt")
    @Comment("", "识别配置的NBT路径,'.'为分隔符")
    var unbindNbt = "sakura_bind_unbind-item"
    var unBindPath = arrayOf("sakura_bind_unbind-item")
        private set

    @Key("unbind.chance")
    @Comment("", "成功率, 每次解绑定都会消耗一个物品")
    var unbindChance = 100.0

    @Key
    @Comment("", "如果绑定/解绑的是可堆叠的物品，那么需要消耗相同数量的物品，否则每次只消耗一个")
    var syncAmount = true

    override fun onLoaded(section: ConfigurationSection) {
        bindPath = bindNbt.split('.').toTypedArray()
    }


}