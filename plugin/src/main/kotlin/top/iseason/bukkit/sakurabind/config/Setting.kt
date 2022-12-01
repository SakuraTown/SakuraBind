package top.iseason.bukkit.sakurabind.config

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.ItemStack
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.noColor
import java.security.InvalidParameterException
import java.util.regex.Pattern

open class Setting(section: ConfigurationSection) {
    private var namePattern: Pattern? = null
    private var nameWithoutColorPattern: Pattern? = null
    private var materialPattern: Pattern? = null
    private var materials: HashSet<Material>? = null
    private var ids: List<Pair<Int, Byte?>>? = null
    private var materialIds: List<Pair<Material, Byte?>>? = null
    private var lorePatterns: List<Pattern>? = null
    private var nbt: ConfigurationSection? = null
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
            val subId = split.getOrNull(1)?.toByteOrNull()
            mainId to subId
        }
        val mIds = matcher.getStringList("materialIds")
        materialIds = if (mIds.isEmpty()) null else mIds.mapNotNull {
            val split = it.split(':')
            val first = split.getOrNull(0) ?: return@mapNotNull null
            val m = Material.matchMaterial(first) ?: return@mapNotNull null
            val subId = split.getOrNull(1)?.toByteOrNull()
            m to subId
        }
        val patterns = matcher.getStringList("lore").map { it.toPattern() }
        if (patterns.isNotEmpty())
            lorePatterns = patterns
        val ms = matcher.getStringList("materials").mapNotNull { Material.matchMaterial(it) }.toHashSet()
        if (ms.isNotEmpty()) materials = ms
        nbt = matcher.getConfigurationSection("nbt")
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
//                println("${it.first}:${it.second} -> ${id}:${mData.data}")
                if (it.second == null)
                    it.first == id
                else
                    it.first == id && mData.data == it.second
            }
            if (!matchId) return false
        }
        if (materialIds != null) {
            val matchMaterialIds = materialIds!!.any {
                val mData = item.data ?: return@any false
                val material = mData.itemType
//                println("${it.first}:${it.second} -> ${id}:${mData.data}")
                if (it.second == null)
                    it.first == material
                else
                    it.first == material && mData.data == it.second
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
                        val l = loreIter.next()
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
            val matchNbt = nbt!!.getKeys(true)
                .filter {
                    val value = nbt!!.get(it)
                    value != null && value !is ConfigurationSection
                }.all {
                    val string = NBTEditor.getString(item, *it.split('.').toTypedArray()) ?: return@all false
                    nbt!!.get(it).toString().matches(Regex(string))
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
            if (player.hasPermission("sakurabind.settings.$key.true")) {
                return true
            } else if (player.hasPermission("sakurabind.settings.$key.false")) {
                return false
            }
        }
        //是物主或者拥有物主的权限
        var isOwner = false
        if (owner != null) {
            isOwner =
                (owner == player?.uniqueId.toString()) || player?.hasPermission("sakurabind.bypass.$owner") == true
            if (isOwner && setting.contains("$key@")) {
                return !setting.getBoolean("$key@")
            }
        }
        if (setting.contains(key)) return setting.getBoolean(key)
        if (isOwner && GlobalSettings.config.contains("$key@")) {
            return !GlobalSettings.config.getBoolean("$key@")
        }
        return GlobalSettings.config.getBoolean(key)
    }

}