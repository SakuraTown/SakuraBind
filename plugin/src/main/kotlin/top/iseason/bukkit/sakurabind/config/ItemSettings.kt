package top.iseason.bukkit.sakurabind.config

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import de.tr7zw.nbtapi.NBT
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.event.ItemMatchedEvent
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.warn
import java.util.concurrent.TimeUnit
import kotlin.math.max

@FilePath("settings.yml")
object ItemSettings : SimpleYAMLConfig() {

    @Key
    @Comment(
        "<--------------------matcher教程-------------------->",
        "matcher可以匹配某类特殊的物品以应用不同的设置",
        "请勿声明名为 'global-setting '的matcher,否则会与公共设置冲突",
        "match 项为需要匹配的物品特征，采用正则表达式 https://www.bejson.com/othertools/regex/",
        "所有 match 项都不是必须的，你可以自由组合, 但至少需要有一个子项, 只有匹配所有子项才算最终匹配到",
        "name 为 物品名字, 必须为非原版翻译名(也就是从创造物品栏拿出来的'圆石'的name为空)",
        "name-without-color 为 除去颜色代码的物品名字, 必须为非原版翻译名(也就是从创造物品栏拿出来的'圆石'的name为空)",
        "material 为 物品材质,使用正则匹配",
        "materials 为 物品材质,使用全名匹配 https://bukkit.windit.net/javadoc/org/bukkit/Material.html",
        "ids 为 物品id:子id 匹配方式 如 6578 或 6578:2",
        "materialIds 为 物品材质:子ID 匹配方式 如 STONE 或 STONE:2 ; 如果只需要匹配材质请使用效率更高的 materials 方式",
        "lore 为 物品lore正则匹配 如有多行则需全匹配, lore! 表示删除匹配到的lore",
        "lore-without-color 为 物品lore除去颜色代码正则匹配 如有多行则需全匹配 与 lore 互斥, lore-without-color! 表示删除匹配到的lore",
        "nbt 为 物品NBT，注意：常规的nbt储存在tag下",
        "注：以上的 name 和 name-without-color 互斥，material、materials、ids、materialIds 互斥，",
        "注：lore、lore-without-color 及其!后缀互斥。 互斥就是只能同时存在其中一个",
        "",
        "<<--------------------matcher插件兼容-------------------->>",
        "以下的项只有在插件存在时有效",
        "MMOItems 有2个项：mmoitems 和 mmoitems-type, 前者匹配 type:id 的物品id，后者只匹配 type , 请使用 List类型",
        "MMOItems 有2个特殊的值 mmoitems: all 或 mmoitems-type: all 表示匹配所有MMOItems物品",
        "ItemsAdder 有2个项：itemsadder 和 itemsadder-namespace, 前者匹配 namespace:id 的物品id，后者只匹配 namespace , 请使用 List类型",
        "ItemsAdder 有2个特殊的值 itemsadder: all 或 itemsadder-namespace: all 表示匹配所有ItemsAdder物品",
        "Oraxen 有1个项：oraxen, 匹配 Oraxen 物品ID, 请使用 List类型",
        "Oraxen 有1个特殊的值 oraxen: all 表示匹配所有 Oraxen 物品",
        "注：以上的 mmoitems 和 mmoitems-type 互斥，itemsadder 和 itemsadder-namespace 互斥。互斥就是只能同时存在其中一个",
        "",
        "<--------------------settings教程-------------------->",
        "settings 项为此matcher独立的设置，完全兼容global-setting中的选项",
        "settings 项中以 '@' 结尾的布尔类型的项，其物主将使用与他人相反的设置",
        "         如 block-deny 下的 break: true 表示所有人都不能破坏此方块物品",
        "         但如果是 break@: true 表示所有人都不能破坏,但物主可以破坏此方块物品",
        "settings 中不存在的项将继承 global-setting.yml 中的同名项",
    )
    var readme = ""

    @Key
    @Comment(
        "",
        "为提高性能，匹配过一次的物品在绑定之后将会把匹配到的设置键存入物品NBT，此为NBT的路径",
        "注意，缓存会导致不同配置的nbt不一致，以至于不同配置的相同物品无法堆叠",
        "留空会使用默认的缓存键"
    )
    var nbt_cache_path: String = "sakura_bind_setting_cache"

    @Key
    @Comment(
        "",
        "在此配置匹配器，example 是一个默认的演示匹配器，不需要请删除",
    )
    private var matchers: ConfigurationSection = YamlConfiguration().apply {
        val example = createSection("example")
        example.createSection("match").apply {
            set("name", "^这是.*的物品$")
            set("material", "BOW|BOOKSHELF")
            set("materials", listOf("DIAMOND_SWORD"))
            set("materialId", listOf("SPECIAL:2"))
            set("ids", listOf("6578", "2233:2"))
            set("lore", listOf("绑定物品", "属于"))
            set("mmoitems", listOf("LONG_SWORD:KNIFE", "LONG_SWORD:SHARP_SWORD"))
            set("nbt.tag.testnbt", ".*")
        }
        example.createSection("settings").apply {
            set("item.lore", listOf("&a灵魂绑定2: &6%player%"))
            set("item-deny.interact", false)
            set("block-deny.interact@", false)
        }
    }

    private var settingCache: Cache<ItemStack, BaseSetting>? = null

    fun getCacheStats() = settingCache?.stats()

    private var settings = LinkedHashMap<String, BaseSetting>()

    override fun onLoaded(section: ConfigurationSection) {
        if (settingCache == null) {
            settingCache = CacheBuilder.newBuilder()
                .initialCapacity(max((Config.setting_cache_size / 8L).toInt(), 200))
                .maximumSize(Config.setting_cache_size)
                .expireAfterAccess(Config.setting_cache_time, TimeUnit.MILLISECONDS)
                .concurrencyLevel(1)
                .weakKeys()
//        .weakValues()
//        .softValues()
                .recordStats()
                .build<ItemStack, BaseSetting>()
        } else {
            settingCache!!.invalidateAll()
            settingCache!!.cleanUp()
        }
        settings.clear()
        if (nbt_cache_path.isBlank()) {
            nbt_cache_path = "sakura_bind_setting_cache"
        }
        matchers.getKeys(false).forEach {
            val s = matchers.getConfigurationSection(it)!!
            try {
                settings[it] = ItemSetting(it, s)
            } catch (_: Exception) {
                warn("配置 $it 格式错误，请检查!")
            }
        }
        settings["global-setting"] = DefaultItemSetting
    }

    fun getSettingsName() = settings.keys.toSet()

    /**
     * 获取物品对应的设置,具有三级缓存
     */
    fun getSetting(item: ItemStack): BaseSetting {
        var setting: BaseSetting? = null
        // 绑定物品缓存, 从NBT中读取设置的ID,再根据ID获取对应的对象
        val key = NBT.get<String>(item) { it.getString(nbt_cache_path) }
        // 持久化的ID可能过期，先判断
        if (!key.isNullOrEmpty()) {
            // 存在缓存，立即返回
            setting = settings[key]
            if (setting != null) {
                return setting
            } else { // 配置删了
                setting = getMatchedSetting(item)
                NBT.modify(item) { it.setString(nbt_cache_path, setting.keyPath) }
                return setting
            }
        }
        // 非绑定物品缓存
        return settingCache?.get(item) {
            getMatchedSetting(item)
        } ?: getMatchedSetting(item)
    }

    fun setSettingCache(item: ItemStack, setting: BaseSetting) {
        NBT.modify(item) { it.setString(nbt_cache_path, setting.keyPath) }
    }

    fun getMatchedSetting(item: ItemStack): BaseSetting {
        for ((_, s) in settings) {
            if (s.match(item)) {
                val itemMatchedEvent = ItemMatchedEvent(item, s)
                Bukkit.getPluginManager().callEvent(itemMatchedEvent)
                return itemMatchedEvent.matchSetting ?: DefaultItemSetting
            }
        }
        return DefaultItemSetting
    }

    fun getSetting(key: String?): BaseSetting {
        if (key == null) return DefaultItemSetting
        return settings[key] ?: DefaultItemSetting
    }

    fun getSettingNullable(key: String?) = settings[key]

    fun getSettingKeys() = settings.keys

    fun putSetting(key: String, setting: BaseSetting) {
        settings[key] = setting
    }

}