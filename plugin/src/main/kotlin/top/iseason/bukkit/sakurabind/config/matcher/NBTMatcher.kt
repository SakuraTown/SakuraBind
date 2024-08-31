package top.iseason.bukkit.sakurabind.config.matcher

import com.google.gson.Gson

import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toJson
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import java.util.regex.Pattern

class NBTMatcher : BaseMatcher() {
    private lateinit var nbt: List<Pair<Array<String>, Pattern>>

    override fun getKeys(): Array<String> = arrayOf("nbt")

    override fun fromSetting(key: String, any: Any): BaseMatcher? {
        if (any !is ConfigurationSection) return null
        val nbtMatcher = NBTMatcher()
        nbtMatcher.nbt = any.getKeys(true)
            .mapNotNull {
                val value = any.get(it)
                if (value == null || value is ConfigurationSection) return@mapNotNull null
                it.split('.').toTypedArray() to Pattern.compile(value.toString())
            }
        return nbtMatcher
    }

    override fun tryMatch(item: ItemStack): Boolean {
        val json = Gson().fromJson(item.toJson(), Map::class.java)
        return nbt.all {
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
            nbtResult
        }

    }

    override fun onDebug(item: ItemStack, debugHolder: CommandSender) {
        val json = Gson().fromJson(item.toJson(), Map::class.java)
        nbt.all {
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
            debugHolder.sendColorMessage(
                Lang.command__test__try_match_nbt.formatBy(
                    pattern,
                    value,
                    nbtResult,
                    path.joinToString(".")
                )
            )
            nbtResult
        }
    }

}