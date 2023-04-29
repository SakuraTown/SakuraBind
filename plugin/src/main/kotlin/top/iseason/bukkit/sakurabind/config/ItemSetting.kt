package top.iseason.bukkit.sakurabind.config

import com.google.gson.Gson
import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.ItemStack
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.getDisplayName
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.noColor
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import java.util.regex.Pattern

open class ItemSetting(override val keyPath: String, section: ConfigurationSection) : BaseSetting {
    private var namePattern: Pattern? = null
    //    private var nameWithoutColorPattern: Pattern? = null
    private var materialPattern: Pattern? = null
    private var materials: HashSet<Material>? = null
    private var ids: Set<String>? = null
    private var materialIds: Set<String>? = null
    var lorePatterns: List<Pattern>? = null
        private set
    var stripNameColor = false
        private set
    var stripLoreColor = false
        private set
    var removeLore = false
        private set
    private var nbt: List<Pair<Array<String>, Pattern>>? = null
    private var setting: ConfigurationSection

    init {
        val matcher =
            section.getConfigurationSection("match") ?: section.createSection("match")
        setting =
            section.getConfigurationSection("settings") ?: YamlConfiguration()
        var name = matcher.getString("name")
        if (name == null) {
            name = matcher.getString("name-without-color")?.also { stripNameColor = true }
        }
        if (name != null)
            namePattern = name.toPattern()
        materialPattern = matcher.getString("material")?.toPattern()
        namePattern = matcher.getString("name")?.toPattern()
        val idList = matcher.getStringList("ids")
        ids = if (idList.isEmpty()) null else idList.toHashSet()
        val mIds = matcher.getStringList("materialIds")
        materialIds = if (mIds.isEmpty()) null else mIds.toHashSet()
        val list = if (matcher.contains("lore"))
            matcher.getStringList("lore")
        else if (matcher.contains("lore!")) {
            removeLore = true
            matcher.getStringList("lore!")
        } else if (matcher.contains("lore-without-color")) {
            stripLoreColor = true
            matcher.getStringList("lore-without-color")
        } else if (matcher.contains("lore-without-color!")) {
            stripLoreColor = true
            removeLore = true
            matcher.getStringList("lore-without-color!")
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

    override fun match(item: ItemStack): Boolean {
        return match(item, null)
    }

    override fun match(item: ItemStack, sender: CommandSender?): Boolean {
        val meta = item.itemMeta
        if (namePattern != null) {
            val matchName = with(namePattern!!) {
                var displayName = item.getDisplayName() ?: return@with false
                if (stripNameColor) displayName = displayName.noColor()
                this.matcher(displayName).find()
            }
            if (sender != null) {
                val message =
                    if (!stripNameColor)
                        Lang.command__test__try_match_name
                    else
                        Lang.command__test__try_match_name_strip
                sender.sendColorMessage(message.formatBy(namePattern, item.getDisplayName() ?: "", matchName))
            }
            if (!matchName) return false
        }
        if (materialPattern != null) {
            val matchMaterial = materialPattern!!.matcher(item.type.toString()).find()
            sender?.sendColorMessage(
                Lang.command__test__try_match_material_pattern.formatBy(
                    materialPattern,
                    item.type,
                    matchMaterial
                )
            )
            if (!matchMaterial) return false
        }
        if (materials != null) {
            val matchMaterials = materials!!.contains(item.type)
            sender?.sendColorMessage(
                Lang.command__test__try_match_material_set.formatBy(
                    if (materials!!.size > 3) "..." else materials!!.joinToString(),
                    item.type,
                    matchMaterials
                )
            )
            if (!matchMaterials) return false
        }
        if (ids != null) {
            var matchId = false
            var idStr = item.type.id.toString()
            //主ID识别
            if (ids!!.contains(idStr)) matchId = true
            else {
                //子ID识别
                val mData = item.data
                if (mData != null) {
                    var subId = mData.data.toInt()
                    if (subId < 0) subId += 256
                    idStr = "$idStr:$subId"
                    matchId = ids!!.contains(idStr)
                }
            }
            sender?.sendColorMessage(
                Lang.command__test__try_match_ids.formatBy(
                    if (ids!!.size > 5) "..." else ids!!.joinToString(),
                    idStr,
                    matchId
                )
            )
            if (!matchId) return false
        }
        if (materialIds != null) {
            var str = item.type.toString()
            val mData = item.data
            if (mData != null) {
                var subId = mData.data.toInt()
                if (subId < 0) subId += 256
                str = "$str:$subId"
            }
            val matchMaterialIds = materialIds!!.contains(str)
            sender?.sendColorMessage(
                Lang.command__test__try_match_material_id.formatBy(
                    if (materialIds!!.size > 5) "..." else materialIds!!.joinToString(),
                    str,
                    matchMaterialIds
                )
            )
            if (!matchMaterialIds) return false
        }
        if (lorePatterns != null) {
            val matchLore = with(lorePatterns!!) {
                meta ?: return@with false
                if (!meta.hasLore()) return@with false
                val lore = meta.lore ?: return@with false
                val patternIter = this.iterator()
                var mLore = true
                var pattern = patternIter.next()
                val lang =
                    if (stripLoreColor) Lang.command__test__try_match_lore_strip else Lang.command__test__try_match_lore
                val indexOfFirst = lore.indexOfFirst {
                    pattern.matcher(if (stripLoreColor) it else it.noColor()).find()
                }
                sender?.sendColorMessage(
                    lang.formatBy(
                        if (indexOfFirst < 0) 0 else indexOfFirst,
                        pattern,
                        lore[if (indexOfFirst < 0) 0 else indexOfFirst],
                        indexOfFirst >= 0
                    )
                )
                if (indexOfFirst < 0 || lore.size < indexOfFirst + this.size) {
                    mLore = false
                } else {
                    for (i in (indexOfFirst + 1) until (indexOfFirst + this.size)) {
                        pattern = patternIter.next()
                        val s = lore[i]
                        val result = pattern.matcher(if (stripLoreColor) s else s.noColor()).find()
                        sender?.sendColorMessage(
                            lang.formatBy(
                                indexOfFirst,
                                pattern,
                                s,
                                result
                            )
                        )
                        if (!result) {
                            mLore = false
                            break
                        }
                    }
                }
                mLore
            }
            if (!matchLore) return false
        }
        if (nbt != null) {
            val json = Gson().fromJson(NBTEditor.getNBTCompound(item).toJson(), Map::class.java)
            val matchNbt = nbt!!.all {
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
                        temp = v as? Map<*, *> ?: break
                    }
                }
                var nbtResult = false
                if (value != null && pattern.matcher(value).find()) {
                    nbtResult = true
                }
                sender?.sendColorMessage(
                    Lang.command__test__try_match_nbt.formatBy(
                        pattern,
                        value,
                        nbtResult,
                        path.joinToString(".")
                    )
                )
                nbtResult
            }

            if (!matchNbt) return false
        }
        return true
    }

    override fun getString(key: String): String {
        return setting.getString(key, GlobalSettings.config.getString(key)) ?: ""
    }

    override fun getStringList(key: String): List<String> {
        return if (setting.contains(key))
            setting.getStringList(key)
        else GlobalSettings.config.getStringList(key)
    }

    override fun getInt(key: String): Int = setting.getInt(key, GlobalSettings.config.getInt(key))

    override fun getLong(key: String): Long = setting.getLong(key, GlobalSettings.config.getLong(key))
    override fun getDouble(key: String): Double = setting.getDouble(key, GlobalSettings.config.getDouble(key))

    override fun getBoolean(key: String, owner: String?, player: HumanEntity?): Boolean {
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
        //是物主或者拥有物主的权限
        var isOwner = false
        if (owner != null && player != null) {
            isOwner =
                (owner == player.uniqueId.toString()) || player.hasPermission("sakurabind.bypass.$owner") == true
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