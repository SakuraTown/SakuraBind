val pluginName = providers.gradleProperty("pluginName").get()
rootProject.name = pluginName

pluginManagement {
    //kotlin 版本
    val kotlinVersion = providers.gradleProperty("kotlinVersion").get()
    //shadowJar 版本
    val shadowJarVersion = providers.gradleProperty("shadowJarVersion").get()
    plugins {
        kotlin("jvm") version kotlinVersion
        id("com.gradleup.shadow") version shadowJarVersion
    }
}
include("core", "plugin")
