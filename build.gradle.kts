import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.0.2")
        classpath("com.guardsquare:proguard-gradle:7.2.2")
    }
}

// 插件名称，请在gradle.properties 修改
val pluginName: String by project
//包名，请在gradle.properties 修改
val group: String by project
val groupS = group
// 作者，请在gradle.properties 修改
val author: String by project
// jar包输出路径，请在gradle.properties 修改
val jarOutputFile: String by project
//插件版本，请在gradle.properties 修改
val version: String by project
// shadowJar 版本 ，请在gradle.properties 修改
val shadowJar: ShadowJar by tasks
// exposed 数据库框架版本，请在gradle.properties 修改

val exposedVersion: String by project
val obfuscated: String by project
val shrink: String by project

repositories {
//    阿里的服务器速度快一点
    maven {
        name = "aliyun"
        url = uri("https://maven.aliyun.com/repository/public/")
    }
    google()
    mavenCentral()
    maven {
        name = "spigot"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/public/")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }

    mavenLocal()
}

dependencies {
    //基础库
    compileOnly(kotlin("stdlib-jdk8"))
//    反射库
//    compileOnly(kotlin("reflect"))

//    协程库
//    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")

    // 数据库
    compileOnly("org.jetbrains.exposed:exposed-core:$exposedVersion")
    compileOnly("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    compileOnly("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    compileOnly("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    compileOnly("com.zaxxer:HikariCP:4.0.3")

    implementation("org.bstats:bstats-bukkit:3.0.0")
    compileOnly("org.spigotmc:spigot-api:1.13.2-R0.1-SNAPSHOT")

}

tasks {
    shadowJar {
        relocate("top.iseason.bukkit.bukkittemplate", "$groupS.libs.core")
    }
    build {
        dependsOn("buildPlugin")
    }
    compileJava {
        options.encoding = "UTF-8"
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    processResources {
        filesMatching("plugin.yml") {
            expand(
                "main" to "$groupS.libs.core.BukkitTemplate",
                "name" to pluginName,
                "version" to project.version,
                "author" to author,
                "kotlinVersion" to getProperties("kotlinVersion"),
                "exposedVersion" to exposedVersion
            )
        }
    }
}
task<com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation>("relocateShadowJar") {
    target = tasks.shadowJar.get()
    prefix = "$groupS.libs"
    shadowJar.minimize()
}
tasks.shadowJar.get().dependsOn(tasks.getByName("relocateShadowJar"))

tasks.register<proguard.gradle.ProGuardTask>("buildPlugin") {
    group = "minecraft"
    verbose()
    injars(tasks.named("shadowJar"))
    if (obfuscated != "true") {
        dontobfuscate()
    }
    if (shrink != "true") {
        dontshrink()
    }
    optimizationpasses(5)
    dontwarn()
    //添加运行环境
    val javaHome = System.getProperty("java.home")
    if (JavaVersion.current() < JavaVersion.toVersion(9)) {
        libraryjars("$javaHome/lib/rt.jar")
    } else {
        libraryjars(
            mapOf(
                "jarfilter" to "!**.jar",
                "filter" to "!module-info.class"
            ),
            "$javaHome/jmods/java.base.jmod"
        )
    }
    libraryjars(configurations.compileClasspath.get().files)
    //启用混淆的选项
    val allowObf = mapOf("allowobfuscation" to true)
    //class规则
    keep("class $groupS.libs.core.BukkitTemplate {}")
    keep(allowObf, "class $groupS.libs.core.utils.MessageUtilsKt {*;}")
    keep(allowObf, "class * implements $groupS.libs.core.KotlinPlugin {*;}")
    keepclassmembers("class * extends $groupS.libs.core.config.SimpleYAMLConfig {*;}")
    keepclassmembers("class * implements $groupS.libs.core.ui.container.BaseUI {*;}")
    keepclassmembers(allowObf, "class * implements org.bukkit.event.Listener {*;}")
    keepclassmembers(allowObf, "class * implements org.jetbrains.exposed.dao.id.IdTable {*;}")
    keepclassmembers(
        allowObf,
        "class * implements org.bukkit.configuration.serialization.ConfigurationSerializable {*;}"
    )
    keepattributes("Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod")
    keepkotlinmetadata()
    repackageclasses()
    if (obfuscated == "true")
        outjars(File(jarOutputFile, "${project.name}-${project.version}-obfuscated.jar"))
    else
        outjars(File(jarOutputFile, "${project.name}-${project.version}.jar"))
}

fun getProperties(properties: String) = rootProject.properties[properties].toString()