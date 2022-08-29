package top.iseason.bukkit.bukkittemplate.utils.bukkit

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


/**
 * 修改ItemMeta
 */
inline fun <T : ItemStack> T.applyMeta(block: ItemMeta.() -> Unit): T {
    val itemMeta = itemMeta ?: return this
    block(itemMeta)
    this.itemMeta = itemMeta
    return this
}

/**
 * 减少物品数量，如果小于0则物品变为空气
 */
fun ItemStack.subtract(count: Int) {
    val i = amount - count
    if (i <= 0) type = Material.AIR
    else amount = i
}

/**
 * 增加物品数量，返回溢出的数量
 */
fun ItemStack.add(count: Int): Int {
    val i = amount + count
    return if (i >= maxStackSize) {
        amount = maxStackSize
        i - maxStackSize
    } else {
        amount = i
        0
    }
}

/**
 * 检查材质是否是空气
 */
fun Material.checkAir(): Boolean = when (this.name) {
    "VOID_AIR",
    "CAVE_AIR",
    "AIR",
    "LEGACY_AIR" -> true

    else -> false
}


object ItemUtils {

    /**
     * 物品转化为字节
     */
    fun toByteArray(item: ItemStack): ByteArray {
        val outputStream = ByteArrayOutputStream()
        BukkitObjectOutputStream(outputStream).use {
            it.writeObject(item)
        }
        val gzipStream = ByteArrayOutputStream()
        GZIPOutputStream(gzipStream).use { it.write(outputStream.toByteArray()) }
        return gzipStream.toByteArray()
    }

    /**
     * 字节转换为ItemStack
     */
    fun fromByteArray(bytes: ByteArray): ItemStack {
        GZIPInputStream(ByteArrayInputStream(bytes)).use { it1 ->
            BukkitObjectInputStream(it1).use { return it.readObject() as ItemStack }
        }
    }

    /**
     * 一组物品转化为字节
     */
    fun toByteArrays(items: Collection<ItemStack>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        BukkitObjectOutputStream(outputStream).use {
            it.writeInt(items.size)
            for (item in items) {
                it.writeObject(item)
            }
        }
        val gzipStream = ByteArrayOutputStream()
        GZIPOutputStream(gzipStream).use { it.write(outputStream.toByteArray()) }
        return gzipStream.toByteArray()
    }

    /**
     * 字节转换为一组ItemStack
     */
    fun fromByteArrays(bytes: ByteArray): List<ItemStack> {
        GZIPInputStream(ByteArrayInputStream(bytes)).use { it1 ->
            BukkitObjectInputStream(it1).use {
                val mutableListOf = mutableListOf<ItemStack>()
                val size = it.readInt()
                for (i in 0 until size) {
                    mutableListOf.add(it.readObject() as ItemStack)
                }
                return mutableListOf
            }
        }

    }


    /**
     * 物品转为BASE64字符串
     */
    fun toBase64(item: ItemStack) = Base64.getEncoder().encodeToString(toByteArray(item))

    /**
     * BASE64字符串转为物品
     */
    fun fromBase64(base64: String) = fromByteArray(Base64.getDecoder().decode(base64))

}