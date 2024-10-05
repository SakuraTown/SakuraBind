package top.iseason.bukkit.sakurabind.config

import de.tr7zw.nbtapi.NBT
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.BindLogger.Formatter
import top.iseason.bukkit.sakurabind.dto.UniqueLog
import top.iseason.bukkit.sakurabind.module.UniqueItem
import top.iseason.bukkit.sakurabind.module.UniqueItem.getUniqueKey
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.DisableHook
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.hook.PlaceHolderHook
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.noColor
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.runAsync
import java.io.File
import java.time.LocalDateTime
import java.util.logging.FileHandler
import java.util.logging.Logger

@FilePath("modules/unique-item.yml")
object UniqueItemConfig : SimpleYAMLConfig() {

    @Key
    @Comment(
        "",
        "请注意!!! 该功能还在实验性阶段，不推荐在生产环境使用!!!",
        "请注意!!! 该功能还在实验性阶段，不推荐在生产环境使用!!!",
        "请注意!!! 该功能还在实验性阶段，不推荐在生产环境使用!!!",
        "",
        "唯一物品, 物品防刷功能。这是一个独立的功能，关闭不会产生任何性能消耗",
        "在物品第一次绑定时记录其数量和生成一个唯一的UUID，并检查所有玩家的物品栏、所有世界掉落物、方块中的数量, 超过的将会删除",
        "开启功能后请在 settings.yml 中配置 unique-item 选项以启用",
    )
    var readme = ""

    @Key
    @Comment("", "功能总开关，重启生效")
    var enable = false

    @Key
    @Comment("", "扫描频率，单位 tick")
    var scanner_period = 10L
    private var scanner: BukkitTask? = null

    @Key
    @Comment("", "唯一Id的NBT路径,'.'为分隔符")
    var unique_nbt_path = "SakuraBind_Unique"

    @Key
    @Comment(
        "", "",
        "默认绑定新物品时使用随机的UUID标识",
        "可以自定义随机标识以减少空间占用，但是请不要太短以免重复"
    )
    var random_template: MemorySection? = null

    @Key
    @Comment("", "自定义随机标识功能开关")
    var random_template__enable = false

    @Key
    @Comment("", "随机的字符库, 请不要出现英文逗号 ','")
    var random_template__chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

    @Key
    @Comment("", "被随机字符替换的字符")
    var random_template__char = 'X'

    @Key
    @Comment("", "模板，由 char-replaced 组成")
    var random_template__template = "XXXX-XXXXXX"

    @Key
    @Comment("", "日志系统，用于记录删除日志")
    var logger = ""

    @Key
    @Comment("", "总开关")
    var logger__enable = false

    @Key
    @Comment("", "日志格式")
    var logger__formatter =
        "&6[唯一物品] 物主: {0} 唯一键: {1} 最大数量 {2} ID: {3} 子ID: {4} 减少数量: {5}；类型: {6} 玩家: {7}"

    @Key
    @Comment("", "将日志输出到控制台")
    var logger__console = true

    @Key
    @Comment("", "将日志输出到数据库中，可跨服")
    var logger__database = false

    @Key
    @Comment("", "将日志输出到独立的文件中")
    var logger__file = false

    @Key
    @Comment("", "独立的文件的位置,修改需重启生效")
    var logger__file_path =
        File(BukkitTemplate.getPlugin().dataFolder, "log${File.separatorChar}unique-log-%g-%u.log").toString()

    @Key
    @Comment("", "独立的文件的最大数量，每个1M,修改需重启生效")
    var file_max_count = 10


    private val file_logger = Logger.getLogger("SakuraBind-UniqueLogger")
        .apply {
            useParentHandlers = false
            File(logger__file_path).parentFile.mkdirs()
            val handler = FileHandler(logger__file_path, 1024000, file_max_count, true)
            handler.formatter = Formatter()
            handler.encoding = "UTF-8"
            addHandler(handler)
        }

    init {
        DisableHook.addTask {
            file_logger.handlers.forEach { it.close() }
        }
    }

    override fun onLoaded(section: ConfigurationSection) {
        if (!logger__enable) setUpdate(false)

        scanner?.cancel()
        scanner = null
        if (enable && scanner_period > 0L)
            scanner = UniqueItem.Scanner()
                .runTaskTimerAsynchronously(BukkitTemplate.getPlugin(), scanner_period, scanner_period)
    }

    fun getUniqueId(item: ItemStack): String? = NBT.get<String>(item) {
        val string = it.getString(unique_nbt_path)
        if (string == "") null
        else
            string
    }

    fun isUnique(item: ItemStack): Boolean = NBT.get<Boolean>(item) { it.hasTag(unique_nbt_path) }

    fun setUnique(item: ItemStack, amount: Int) {
        NBT.modify(item) {
            it.setString(unique_nbt_path, getUniqueKey(amount))
        }
    }

    fun setUnique(item: ItemStack, key: String) {
        NBT.modify(item) {
            it.setString(unique_nbt_path, key)
        }
    }

    fun log(unique: String, max: Int, item: ItemStack, reduce: Int, type: String, attach: String = "") {
        if (!logger__enable) return
        runAsync {
            val owner by lazy { SakuraBindAPI.getOwner(item)!!.toString() }
            val message by lazy {
                val name = if (item.hasItemMeta() && item.itemMeta!!.hasDisplayName()) {
                    "${item.type}(${item.itemMeta!!.displayName})"
                } else item.type
                val subId = item.data?.data ?: 0
                PlaceHolderHook.setPlaceHolder(
                    logger__formatter.formatBy(
                        owner,
                        unique,
                        max,
                        name,
                        subId,
                        reduce,
                        type,
                        attach
                    ), null
                )
            }
            if (logger__console) {
                Bukkit.getConsoleSender().sendColorMessage(message, SimpleLogger.prefix)
            }
            val noColor by lazy { message.noColor() }
            if (logger__file) {
                file_logger.info(noColor)
            }
            if (logger__database) {
                dbTransaction {
                    UniqueLog.new {
                        this.uuid = owner
                        this.unique = unique
                        this.log = noColor
                        this.type = type
                        this.time = LocalDateTime.now()
                    }
                }
            }
        }
    }
}