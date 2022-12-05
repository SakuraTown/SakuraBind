package top.iseason.bukkit.sakurabind.config

import com.google.gson.Gson
import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.ItemStack
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.noColor
import java.security.InvalidParameterException
import java.util.regex.Pattern

open class Setting(val keyPath: String, section: ConfigurationSection) {
    private var namePattern: Pattern? = null
    private var nameWithoutColorPattern: Pattern? = null
    private var materialPattern: Pattern? = null
    private var materials: HashSet<Material>? = null
    private var ids: List<Pair<Int, Int?>>? = null
    private var materialIds: List<Pair<Material, Int?>>? = null
    private var lorePatterns: List<Pattern>? = null
    private var stripColor = false
    private var nbt: List<Pair<Array<String>, Pattern>>? = null
    private var setting: ConfigurationSection

    init {
        val matcher =
            section.getConfigurationSection("match") ?: throw InvalidParameterException("需要声明 'match' 选项")
        if (matcher.getKeys(false).isEmpty()) {
            throw InvalidParameterException("'match'选项不能为空!")
        }
        setting =
            section.getConfigurationSection("settings") ?: YamlConfiguration()
        namePattern = matcher.getString("name")?.toPattern()
        nameWithoutColorPattern = matcher.getString("name-without-color")?.toPattern()
        materialPattern = matcher.getString("material")?.toPattern()
        namePattern = matcher.getString("name")?.toPattern()
        val idList = matcher.getStringList("ids")
        ids = if (idList.isEmpty()) null else idList.mapNotNull {
            val split = it.split(':')
            val mainId = split.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val subId = split.getOrNull(1)?.toIntOrNull()
            mainId to subId
        }
        val mIds = matcher.getStringList("materialIds")
        materialIds = if (mIds.isEmpty()) null else mIds.mapNotNull {
            val split = it.split(':')
            val first = split.getOrNull(0) ?: return@mapNotNull null
            val m = Material.matchMaterial(first) ?: return@mapNotNull null
            val subId = split.getOrNull(1)?.toIntOrNull()
            m to subId
        }
        val list = if (matcher.contains("lore"))
            matcher.getStringList("lore")
        else if (matcher.contains("lore-without-color")) {
            stripColor = true
            matcher.getStringList("lore-without-color")
        } else emptyList()
        if (list.isNotEmpty())
            lorePatterns = list.map { it.toPattern() }
        val ms = matcher.getStringList("materials").mapNotNull { Material.matchMaterial(it) }.toHashSet()
        if (ms.isNotEmpty()) materials = ms
        val nbtSection = matcher.getConfigurationSection("nbt")
        if (nbtSection != null) {
            nbt = nbtSection.getKeys(true)
                .mapNotNull {
                    val value = nbtSection.get(it)
                    if (value == null || value is ConfigurationSection) return@mapNotNull null
                    it.split('.').toTypedArray() to Pattern.compile(value.toString())
                }
        }
    }

    open fun match(item: ItemStack): Boolean {
        val meta = item.itemMeta
        if (namePattern != null) {
            val matchName = with(namePattern!!) {
                meta ?: return@with false
                if (meta.hasDisplayName() || meta.displayName == null) return@with false
                this.matcher(meta.displayName).find()
            }
            if (!matchName) return false
        }
        if (nameWithoutColorPattern != null) {
            val matchNameNoColor = with(nameWithoutColorPattern!!) {
                meta ?: return@with false
                if (meta.hasDisplayName()) return@with false
                this.matcher(meta.displayName.noColor()!!).find()
            }
            if (!matchNameNoColor) return false
        }
        if (materialPattern != null) {
            val matchMaterial = materialPattern!!.matcher(item.type.toString()).find()
            if (!matchMaterial) return false
        }
        if (materials != null) {
            val matchMaterials = materials!!.contains(item.type)
            if (!matchMaterials) return false
        }
        if (ids != null) {
            val matchId = ids!!.any {
                val mData = item.data ?: return@any false
                val id = mData.itemType.id
                var subId = mData.data.toInt()
                if (subId < 0) subId += 256
//                debug("尝试匹配物品id: ${it.first}:${it.second} 实际的id ${id}:${subId}")
                if (it.second == null)
                    it.first == id
                else
                    it.first == id && subId == it.second
            }
            if (!matchId) return false
        }
        if (materialIds != null) {
            val matchMaterialIds = materialIds!!.any {
                val mData = item.data ?: return@any false
                val material = mData.itemType
                var subId = mData.data.toInt()
                if (subId < 0) subId += 256
//                println("${it.first}:${it.second} -> ${id}:${mData.data}")
                if (it.second == null)
                    it.first == material
                else
                    it.first == material && subId == it.second
            }
            if (!matchMaterialIds) return false
        }
        if (lorePatterns != null) {
            val matchLore = with(lorePatterns!!) {
                meta ?: return@with false
                if (meta.hasLore()) return@with false
                val lore = meta.lore ?: return@with false
                var mLore = false
                val loreIter = lore.iterator()
                val patternIter = this.iterator()
                var start = false
                label1@ while (patternIter.hasNext()) {
                    val next = patternIter.next()
                    label2@ while (loreIter.hasNext()) {
                        var l = loreIter.next()
                        if (stripColor) l = l.noColor()
                        val find = next.matcher(l).find()
                        if (find) {
                            if (!start) {
                                start = true
                                mLore = true
                            }
                            break@label2
                        } else if (start) {
                            mLore = false
                            break@label1
                        }
                    }
                }
                mLore
            }
            if (!matchLore) return false
        }
        if (nbt != null) {
            val json = Gson().fromJson(NBTEditor.getNBTCompound(item).toJson(), Map::class.java)
            val matchNbt = nbt!!.any {
                val path = it.first
                val pattern = it.second
                var temp = json
                val size = path.size - 1
                var value: String? = null
                for (i in 0..size) {
                    val node = path[i]
                    val v = temp[node] ?: break
                    if (i == size) {
                        value = v.toString()
                        break
                    } else {
                        temp = v as? Map<Any?, Any?> ?: break
                    }
                }
//                println(path.joinToString())
//                println(pattern)
//                println(value)
                if (value == null) return@any false
                pattern.matcher(value).find()
            }
            if (!matchNbt) return false
        }
        return true
    }

    fun getString(key: String): String {
        return setting.getString(key, GlobalSettings.config.getString(key)) ?: ""
    }

    fun getStringList(key: String): List<String> {
        return if (setting.contains(key))
            setting.getStringList(key)
        else GlobalSettings.config.getStringList(key)
    }

    fun getInt(key: String): Int = setting.getInt(key, GlobalSettings.config.getInt(key))

    fun getLong(key: String): Long = setting.getLong(key, GlobalSettings.config.getLong(key))

    /**
     * 返回是否禁止操作
     */
    fun getBoolean(key: String, owner: String? = null, player: HumanEntity? = null): Boolean {
        //权限检查
        if (player != null) {
            if (player.hasPermission("sakurabind.setting.$keyPath.$key.true")) {
                return true
            } else if (player.hasPermission("sakurabind.setting.$keyPath.$key.false")) {
                return false
            } else if (player.hasPermission("sakurabind.settings.$key.true")) {
                return true
            } else if (player.hasPermission("sakurabind.settings.$key.false")) {
                return false
            }
        }
//        println(0)
        //是物主或者拥有物主的权限
        var isOwner = false
        if (owner != null) {
            isOwner =
                (owner == player?.uniqueId.toString()) || player?.hasPermission("sakurabind.bypass.$owner") == true
//            println("isOwner ${isOwner}")
            if (isOwner && setting.contains("$key@")) {
                return !setting.getBoolean("$key@")
            }
        }
        if (setting.contains(key)) return setting.getBoolean(key)
        if (GlobalSettings.config.contains("$key@")) {
            return if (isOwner)
                !GlobalSettings.config.getBoolean("$key@")
            else
                GlobalSettings.config.getBoolean("$key@")
        }
        return GlobalSettings.config.getBoolean(key)
    }

}