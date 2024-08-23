package top.iseason.bukkit.sakurabind.utils

import io.github.bananapuncher714.nbteditor.NBTEditor
import java.lang.reflect.Method

object NBTUtils {
    val method: Method?

    init {
        if (NBTEditor.getMinecraftVersion().ordinal > NBTEditor.MinecraftVersion.v1_20_R4.ordinal) {
            var find = NBTEditor::class.java.declaredMethods.find { it.name == "getValue" }
            if (find != null) {
                find.isAccessible = true
                method = find
            } else
                method = null
        } else {
            method = null
        }
    }

    fun getKeys(item: Any, vararg keys: Any): Collection<String>? {
        if (method == null) {
            return NBTEditor.getKeys(item, keys)
        } else {
            var map = method.invoke(null, item, keys) as? Map<String, String> ?: return null
            return map.keys
        }
    }

}