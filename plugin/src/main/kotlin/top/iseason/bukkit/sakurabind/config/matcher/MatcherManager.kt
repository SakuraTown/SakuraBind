package top.iseason.bukkit.sakurabind.config.matcher

import org.bukkit.configuration.ConfigurationSection
import java.util.concurrent.ConcurrentHashMap

object MatcherManager {
    private val matchers = ConcurrentHashMap<String, BaseMatcher>()

    init {
        //注册默认匹配器
        addMatcher(NameMatcher())
        addMatcher(TypeMatcher())
        addMatcher(LoreMatcher())
        addMatcher(NBTMatcher())
    }

    fun addMatcher(matcher: BaseMatcher) {
        for (key in matcher.getKeys()) {
            matchers[key] = matcher
        }
    }

    fun parseSection(section: ConfigurationSection): List<BaseMatcher> {
        return section
            .getKeys(false)
            .mapNotNull { matchers[it]?.fromSetting(it, section.get(it)!!) }
    }
}