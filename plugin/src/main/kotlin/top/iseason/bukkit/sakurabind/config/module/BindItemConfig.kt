package top.iseason.bukkit.sakurabind.config.module


import de.tr7zw.nbtapi.NBT
import org.bukkit.configuration.MemorySection
import org.bukkit.inventory.ItemStack
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
    @Comment("", "识别配置的NBT路径, 值是指定 settings.yml的匹配器键，不存在将自动识别")
    var bindNbt = "sakura_bind_bind-item"


    @Key("bind.chance")
    @Comment("", "成功率, 每次绑定都会消耗一个物品")
    var bindChance = 100.0

    @Key("bind.chance-nbt")
    @Comment("", "成功率，储存在nbt中，不读配置")
    var bindChanceNbt = "sakura_bind_bind-item-chance"

    @Key
    @Comment("", "解绑物品")
    var unbind: MemorySection? = null

    @Key("unbind.nbt")
    @Comment("", "识别配置的NBT路径")
    var unbindNbt = "sakura_bind_unbind-item"

    @Key("unbind.chance")
    @Comment("", "成功率, 每次解绑定都会消耗一个物品")
    var unbindChance = 100.0

    @Key("unbind.chance-nbt")
    @Comment("", "成功率，储存在nbt中，不读配置")
    var unBindChanceNbt = "sakura_bind_unbind-item-chance"

    @Key
    @Comment("", "如果绑定/解绑的是可堆叠的物品，那么需要消耗相同数量的物品，否则每次只消耗一个")
    var syncAmount = true


    fun getBind(item: ItemStack): Pair<String, Double>? {
        val (settingKey, rate) = NBT.get<Pair<String?, Double?>>(item) {
            if (it.hasTag(bindNbt)) {
                val chanceStr = it.getString(bindChanceNbt)
                val chance = if (chanceStr == "") bindChance
                else runCatching { chanceStr.toDouble() }.getOrElse { bindChance }
                it.getString(bindNbt) to chance
            } else Pair(null, null)
        }
        if (settingKey == null) return null
        return settingKey to rate!!
    }

    fun getUnBind(item: ItemStack): Pair<String, Double>? {
        val (settingKey, rate) = NBT.get<Pair<String?, Double?>>(item) {
            if (it.hasTag(unbindNbt)) {
                val chanceStr = it.getString(unBindChanceNbt)
                val chance = if (chanceStr == "") unbindChance
                else runCatching { chanceStr.toDouble() }.getOrElse { unbindChance }
                it.getString(unbindNbt) to chance
            } else Pair(null, null)
        }
        if (settingKey == null) return null
        return settingKey to rate!!
    }

}