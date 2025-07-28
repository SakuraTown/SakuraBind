package top.iseason.bukkit.sakurabind.config.module

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheStats
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.ItemSetting
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.warn
import java.util.concurrent.TimeUnit

@FilePath("modules/auto-unbind.yml")
object AutoUnBindConfig : SimpleYAMLConfig() {

    @Suppress("unused")
    @Key
    @Comment(
        "",
        "自动解绑模块",
        "识别已绑定的物品，并在玩家进行特定行为时解绑。",
        "此功能会对每一个绑定物品进行检测，建议只在需要时开启",
        "xxMatcher 的格式与 setting.yml 的格式一致"
    )
    var readme = ""

    @Key
    @Comment("", "功能总开关")
    var enable = false

    @Key
    @Comment("", "是否点击物品时解绑")
    var onClick = false
        get() = enable && field

    @Key("onClickMatcher")
    var onClickSection: MemorySection? = null
    var onClickMatcher = HashMap<String, ItemSetting>()

    @Key
    @Comment("", "捡起物品时解绑")
    var onPickup = false
        get() = enable && field

    @Key("onPickupMatcher")
    var onPickupSection: MemorySection? = null
    var onPickupMatcher = HashMap<String, ItemSetting>()

    @Key
    @Comment("", "丢弃物品时解绑")
    var onDrop = false
        get() = enable && field

    @Key("onDropMatcher")
    var onDropSection: MemorySection? = null
    var onDropMatcher = HashMap<String, ItemSetting>()

    @Key
    @Comment("", "使用物品消耗耐久时解绑(包括工具、武器、盔甲等有耐久的物品)")
    var onUse = false
        get() = enable && field

    @Key("onUseMatcher")
    var onUseSection: MemorySection? = null
    var onUseMatcher = HashMap<String, ItemSetting>()

    @Key
    @Comment("", "手拿物品左键时解绑")
    var onLeft = false
        get() = enable && field

    @Key("onLeftMatcher")
    var onLeftSection: MemorySection? = null
    var onLeftMatcher = HashMap<String, ItemSetting>()

    @Key
    @Comment("", "手拿物品右键时解绑")
    var onRight = false
        get() = enable && field

    @Key("onRightMatcher")
    var onRightSection: MemorySection? = null
    var onRightMatcher = HashMap<String, ItemSetting>()

    @Key
    @Comment("", "装备物品穿戴时解绑(仅限Paper及其下游服务端核心)")
    var onEquipWear = false
        get() = enable && field

    @Key("onEquipWearMatcher")
    var onEquipWearSection: MemorySection? = null
    var onEquipWearMatcher = HashMap<String, ItemSetting>()

    @Key
    @Comment("", "扫描器扫描时解绑(在config.yml中配置扫描器)")
    var onScanner = false
        get() = enable && field

    @Key("onScannerMatcher")
    var onScannerSection: MemorySection? = null
    var onScannerMatcher = HashMap<String, ItemSetting>()

    @Key
    @Comment("", "匹配完的物品会缓存结果, 此处设置最大缓存的数量, 重启生效")
    var cacheSize = 800L

    private var cache: Cache<ItemStack, Boolean>? = null
    fun getCacheStat(): CacheStats? = cache?.stats()

    fun setupMatcher(keyName: String, section: MemorySection?, matcherMap: MutableMap<String, ItemSetting>) {
        matcherMap.clear()
        section?.getKeys(false)?.forEach {
            val s = section.getConfigurationSection(it)!!
            try {
                matcherMap[it] = ItemSetting(it, s)
            } catch (_: Exception) {
                warn("配置 modules/auto-unbind.yml $keyName.$it 格式错误，请检查!")
            }
        }

    }

    override fun onLoaded(section: ConfigurationSection) {
        if (cache == null) {
            cache = CacheBuilder.newBuilder()
                .recordStats()
                .maximumSize(cacheSize)
                .weakKeys()
                .expireAfterAccess(30000L, TimeUnit.MILLISECONDS)
                .build()
        } else {
            cache!!.invalidateAll()
            cache!!.cleanUp()
        }
        if (onClick) setupMatcher("onClick", onClickSection, onClickMatcher)
        if (onPickup) setupMatcher("onPickup", onPickupSection, onPickupMatcher)
        if (onDrop) setupMatcher("onDrop", onDropSection, onDropMatcher)
        if (onUse) setupMatcher("onUse", onUseSection, onUseMatcher)
        if (onLeft) setupMatcher("onLeft", onLeftSection, onLeftMatcher)
        if (onRight) setupMatcher("onRight", onRightSection, onRightMatcher)
        if (onEquipWear) setupMatcher("onEquipWear", onEquipWearSection, onEquipWearMatcher)
        if (onScanner) setupMatcher("onScanner", onScannerSection, onScannerMatcher)
    }

    fun check(itemStack: ItemStack, matcherMap: MutableMap<String, ItemSetting>): Boolean {
        return cache?.get(itemStack) {
            matcherMap.any { (_, matcher) -> matcher.match(itemStack) }
        } ?: matcherMap.any { (_, matcher) -> matcher.match(itemStack) }
    }
}