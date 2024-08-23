package top.iseason.bukkit.sakurabind.config

import com.google.common.cache.CacheBuilder
import io.github.bananapuncher714.nbteditor.NBTEditor
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
        "为提高性能，匹配过一次的物品在绑定之后将会把匹配到的设置键存入物品NBT，此为NBT的路径(由tag路径开始) '.' 为路径分隔符",
        "注意，缓存会导致不同配置的nbt不一致，以至于不同配置的相同物品无法堆叠",
        "留空不使用缓存"
    )
    var nbt_cache_path: String = "sakura_bind_setting_cache"
    var nbtPath: Array<Any> = arrayOf("sakura_bind_setting_cache")
    var isCacheInNbt = true

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

//    val settingCache: UserManagedCache<ItemStack, BaseSetting> = UserManagedCacheBuilder
//        .newUserManagedCacheBuilder(ItemStack::class.java, BaseSetting::class.java)
//        .identifier("SakuraBind-Setting-Cache")
//        .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofSeconds(3)))
//        .withKeyCopier(IdentityCopier.identityCopier())
//        .withValueCopier(IdentityCopier.identityCopier())
//        .withDispatcherConcurrency(2)
//        .build(true)

    private val settingCache2 = CacheBuilder.newBuilder()
        .maximumSize(Config.setting_cache_size)
        .weakValues()
        .recordStats()
        .build<ItemStack, BaseSetting>()

    fun getCacheStats() = settingCache2.stats()

    private var settings = LinkedHashMap<String, BaseSetting>()

    override fun onLoaded(section: ConfigurationSection) {
        settings.clear()
        settingCache2.invalidateAll()
        settingCache2.cleanUp()
        nbtPath = if (nbt_cache_path.isBlank()) {
            emptyArray()
        } else {
            if (NBTEditor.getMinecraftVersion().ordinal > NBTEditor.MinecraftVersion.v1_20_R4.ordinal) {
                var list1 = ArrayList<Any>()
                list1.add(NBTEditor.CUSTOM_DATA)
                list1.addAll(nbt_cache_path.split('.'))
                list1.toTypedArray()
            } else {
                nbt_cache_path.split('.').toTypedArray()
            }
        }
        isCacheInNbt = nbtPath.isNotEmpty()
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
    fun getSetting(item: ItemStack, setInCache: Boolean = true): BaseSetting {
        // 一级缓存, 由EhCache实现
        // 仅给未绑定物品添加一级缓存
        var setting: BaseSetting? = if (setInCache && isCacheInNbt) null else settingCache2.getIfPresent(item)
//        // 存在缓存，立即返回
        if (setting != null) {
            return setting
        }
        // 二级缓存,从NBT中读取设置的ID,再根据ID获取对应的对象
        val key = if (isCacheInNbt) NBTEditor.getString(item, *nbtPath) else null
        // 持久化的ID可能过期，先判断
        if (key != null && setInCache) {
            // 存在缓存，立即返回
            setting = settings[key]
            if (setting != null) return setting
        }
        // 物品匹配,顺序查找,找到了立即结束循环并加入缓存
        for (s in settings.values) {
            if (s.match(item)) {
                val itemMatchedEvent = ItemMatchedEvent(item, s)
                Bukkit.getPluginManager().callEvent(itemMatchedEvent)
                setting = itemMatchedEvent.matchSetting ?: DefaultItemSetting
                if (setInCache && isCacheInNbt) item.itemMeta =
                    NBTEditor.set(item, setting.keyPath, *nbtPath).itemMeta
                break
            }
        }
        //到这就没有合适的键了，但又有nbt，说明被删了，清除旧的缓存
        if (key != null) item.itemMeta = NBTEditor.set(item, null, *nbtPath).itemMeta
        val s = setting ?: DefaultItemSetting
        if (!setInCache) settingCache2.put(item, s)
        return s
    }

    fun setSettingCache(item: ItemStack, setting: BaseSetting) {
        if (nbtPath.isEmpty()) return
        item.itemMeta = NBTEditor.set(item, setting.keyPath, *nbtPath).itemMeta
    }

    fun getMatchedSetting(item: ItemStack): BaseSetting {
        for ((_, s) in settings) {
            if (s.match(item)) {
                return s
            }
        }
        return DefaultItemSetting
    }

    fun getSetting(key: String?) = settings[key ?: "global-setting"] ?: DefaultItemSetting

    fun getSettingNullable(key: String) = settings[key]

    fun getSettingKeys() = settings.keys

    fun putSetting(key: String, setting: BaseSetting) {
        settings[key] = setting
    }

}