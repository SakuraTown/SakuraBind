package top.iseason.bukkit.sakurabind.cache

import top.iseason.bukkit.sakurabind.config.BaseSetting
import top.iseason.bukkit.sakurabind.config.ItemSettings
import java.util.*

data class BlockInfo(
    /**
     * 物主
     */
    val owner: String,
    /**
     * 绑定设置
     */
    val setting: BaseSetting,
    /**
     * 额外数据
     */
    val extraData: List<String> = emptyList()
) {
    val ownerUUID: UUID by lazy { UUID.fromString(owner) }

    fun serialize(): String {
        val value = if (setting.keyPath != "global-setting")
            "$owner,$setting"
        else owner
        if (extraData.isEmpty()) {
            return value
        } else {
            return extraData.joinToString(prefix = "${value}\t", separator = "\t")
        }
    }

    companion object {

        @JvmStatic
        fun deserialize(str: String): BlockInfo {
            val strings = str.split('\t')
            val split = strings.first().split(',')
            return if (strings.size > 1) {
                BlockInfo(split[0], ItemSettings.getSetting(split.getOrNull(1)), strings.drop(1))
            } else {
                BlockInfo(split[0], ItemSettings.getSetting(split.getOrNull(1)))
            }
        }
    }

}