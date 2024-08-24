package top.iseason.bukkittemplate.utils

object JavaVersion {
    val version: Int

    init {
        val versionString = System.getProperty("java.version")
        val indexOf = versionString.indexOf('.')
        val substring = versionString.substring(0, indexOf)
        if (substring == "1") {
            val indexOf1 = versionString.indexOf('.', indexOf + 1)
            version = versionString.substring(indexOf + 1, indexOf1).toInt()
        } else {
            version = substring.toInt()
        }
    }

    fun isGreaterOrEqual(version: Int): Boolean = this.version >= version

}