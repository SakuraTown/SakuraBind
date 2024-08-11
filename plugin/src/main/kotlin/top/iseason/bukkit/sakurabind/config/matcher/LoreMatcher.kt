package top.iseason.bukkit.sakurabind.config.matcher

import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.containList
import top.iseason.bukkit.sakurabind.utils.indexOfList
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.noColor
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import java.util.regex.Pattern

class LoreMatcher : BaseMatcher() {
    var stripLoreColor = false
        private set
    var removeMatched = false
        private set
    lateinit var lorePatterns: List<Pattern>
        private set

    override fun getKeys(): Array<String> =
        arrayOf("lore", "lore!", "lore-without-color", "lore-without-color!")

    override fun fromSetting(key: String, any: Any): BaseMatcher? {
        val loreMatcher = LoreMatcher()
        if (key.endsWith('!')) loreMatcher.removeMatched = true
        if (key.length > 5) loreMatcher.stripLoreColor = true
        val lore = if (any is String) listOf(any) else any as? Collection<String> ?: return null
        loreMatcher.lorePatterns = lore.map { Pattern.compile(it) }
        return loreMatcher
    }

    override fun tryMatch(item: ItemStack): Boolean {
        val meta = item.itemMeta
        return with(lorePatterns) {
            meta ?: return@with false
            if (!meta.hasLore()) return@with false
            var lore = meta.lore ?: return@with false
            if (stripLoreColor) lore = lore.map { it.noColor() }
            containList(lore, lorePatterns) { raw, pattern ->
                pattern.matcher(raw).find()
            }
        }
    }

    override fun onDebug(item: ItemStack, debugHolder: CommandSender) {
        val meta = item.itemMeta ?: return
        with(lorePatterns) {
            if (!meta.hasLore()) return@with
            var lore = meta.lore ?: return@with
            if (stripLoreColor) lore = lore.map { it.noColor() }
            val lang =
                if (stripLoreColor) Lang.command__test__try_match_lore_strip else Lang.command__test__try_match_lore
            val indexOfFirst = indexOfList(lore, lorePatterns) { raw, pattern ->
                pattern.matcher(raw).find()
            }
            val patternIter = lorePatterns.iterator()
            var pattern = patternIter.next()
            debugHolder.sendColorMessage(
                lang.formatBy(
                    indexOfFirst,
                    pattern,
                    if (indexOfFirst < 0) "null" else lore[indexOfFirst],
                    indexOfFirst >= 0
                )
            )
            if (indexOfFirst < 0) return
            for (i in (indexOfFirst + 1) until (indexOfFirst + this.size)) {
                pattern = patternIter.next()
                val result = pattern.matcher(lore[i]).find()
                debugHolder.sendColorMessage(lang.formatBy(i, pattern, lore[i], result))
                if (!result) {
                    break
                }
            }
        }
    }

}