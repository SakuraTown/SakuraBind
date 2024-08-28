package top.iseason.bukkittemplate.utils

object JavaVersion {
    val version: Int

    init {
        val versionString = System.getProperty("java.version")
        val indexOf = versionString.indexOf('.')
        version = try {
            if (indexOf > 0) {
                val substring = versionString.substring(0, indexOf)
                if (substring == "1") {
                    val indexOf1 = versionString.indexOf('.', indexOf + 1)
                    versionString.substring(indexOf + 1, indexOf1).toInt()
                } else {
                    substring.toInt()
                }
            } else {
                versionString.toInt()
            }
        } catch (_: Exception) {
            8
        }
    }

    fun isGreaterOrEqual(version: Int): Boolean = this.version >= version

}