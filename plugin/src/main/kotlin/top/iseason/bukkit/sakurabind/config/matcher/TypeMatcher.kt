package top.iseason.bukkit.sakurabind.config.matcher

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import java.util.regex.Pattern

/**
 * 物品材质匹配器
 */
class TypeMatcher : BaseMatcher() {
    private var pattern: Pattern? = null
    private var materials: Set<Material>? = null
    private var ids: Set<String>? = null
    private var materialIds: Set<String>? = null

    override fun getKeys(): Array<String> = arrayOf("material", "materials", "ids", "materialIds")

    override fun fromSetting(key: String, any: Any): BaseMatcher? {
        val typeMatcher = TypeMatcher()
        if (key == "material" && any is String) {
            typeMatcher.pattern = Pattern.compile(any)
            return typeMatcher
        }
        val strings = any as? Collection<*> ?: return null
        if (key == "materials") {
            typeMatcher.materials = strings.mapNotNull { Material.matchMaterial(it.toString().trim()) }.toHashSet()
            return typeMatcher
        }
        // 6578 或 6578:2
        if (key == "ids") {
            typeMatcher.ids = strings.map { it.toString().trim() }.toHashSet()
            return typeMatcher
        }
        // STONE 或 STONE:2
        if (key == "materialIds") {
            typeMatcher.materialIds = strings.mapNotNull {
                val split = it.toString().trim().split(':')
                val matchMaterial = Material.matchMaterial(split[0])
                if (matchMaterial == null) return@mapNotNull null
                else if (split.size > 1)
                    "${matchMaterial.name}:${split[1]}"
                else matchMaterial.name
            }.toHashSet()
            return typeMatcher
        }
        return null
    }

    override fun tryMatch(item: ItemStack): Boolean {
        if (pattern != null) {
            return pattern!!.matcher(item.type.toString()).find()
        }
        if (materials != null) {
            return materials!!.contains(item.type)
        }
        if (ids != null) {
            var idStr = item.type.id.toString()
            //主ID识别
            if (ids!!.contains(idStr)) return true
            else {
                //子ID识别
                val mData = item.data
                if (mData != null) {
                    var subId = mData.data.toInt()
                    if (subId < 0) subId += 256
                    idStr = "$idStr:$subId"
                    return ids!!.contains(idStr)
                }
            }
        }
        if (materialIds != null) {
            var str = item.type.name
            val mData = item.data
            if (mData != null) {
                var subId = mData.data.toInt()
                if (subId < 0) subId += 256
                str = "$str:$subId"
            }
            return materialIds!!.contains(str)
        }
        return false
    }

    override fun onDebug(item: ItemStack, debugHolder: CommandSender) {
        if (pattern != null) debugHolder.sendColorMessage(
            Lang.command__test__try_match_material_pattern.formatBy(
                pattern,
                item.type,
                tryMatch(item)
            )
        )
        if (materials != null) debugHolder.sendColorMessage(
            Lang.command__test__try_match_material_set.formatBy(
                if (materials!!.size > 3) "..." else materials!!.joinToString(),
                item.type, tryMatch(item)
            )
        )
        if (ids != null) {
            var idStr = item.type.id.toString()
            val mData = item.data
            if (mData != null && mData.data != 0.toByte()) {
                var subId = mData.data.toInt()
                if (subId < 0) subId += 256
                idStr = "$idStr:$subId"
            }
            debugHolder.sendColorMessage(
                Lang.command__test__try_match_ids.formatBy(
                    if (ids!!.size > 5) "..." else ids!!.joinToString(),
                    idStr, tryMatch(item)
                )
            )
        }
        if (materialIds != null) {
            var idStr = item.type.name
            val mData = item.data
            if (mData != null && mData.data != 0.toByte()) {
                var subId = mData.data.toInt()
                if (subId < 0) subId += 256
                idStr = "$idStr:$subId"
            }
            debugHolder.sendColorMessage(
                Lang.command__test__try_match_material_id.formatBy(
                    if (materialIds!!.size > 5) "..." else materialIds!!.joinToString(),
                    idStr, tryMatch(item)
                )
            )
        }

    }

}