package top.iseason.bukkit.sakurabind.config.matcher

import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.getDisplayName
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.noColor
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import java.util.regex.Pattern

class NameMatcher : BaseMatcher() {
    private lateinit var pattern: Pattern
    private var isStripColor = false
    override fun getKeys(): Array<String> = arrayOf("name", "name-without-color")

    override fun fromSetting(key: String, any: Any): BaseMatcher? {
        if (any !is String) return null
        val nameMatcher = NameMatcher()
        if (key == "name-without-color") nameMatcher.isStripColor = true
        nameMatcher.pattern = Pattern.compile(any)
        return nameMatcher
    }

    override fun tryMatch(item: ItemStack): Boolean {
        var displayName = item.getDisplayName() ?: return false
        if (isStripColor) displayName = displayName.noColor()
        return pattern.matcher(displayName).find()
    }

    override fun onDebug(item: ItemStack, debugHolder: CommandSender) {
        val message = if (!isStripColor) Lang.command__test__try_match_name
        else Lang.command__test__try_match_name_strip
        debugHolder.sendColorMessage(message.formatBy(pattern, item.getDisplayName() ?: "", tryMatch(item)))
    }
}