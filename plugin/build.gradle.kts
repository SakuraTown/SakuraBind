plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
    id("org.jetbrains.dokka") version "1.9.20"
}

buildscript {
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.5.0")
    }
}
repositories {
    mavenCentral()
    maven {
        name = "MMOItems"
        url = uri("https://nexus.phoenixdevt.fr/repository/maven-public/")
    }
    maven {
        name = "McMMO"
        url = uri("https://nexus.neetgames.com/repository/maven-releases/")
    }
    maven {
        name = "jitpack"
        url = uri("https://www.jitpack.io")
    }
    maven {
        name = "PlaceholderAPI"
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}

dependencies {
// 依赖core模块
    api(project(":core"))
//    反射库
//    compileOnly(kotlin("reflect"))

//    协程库
//    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.9.20")

// 本地依赖放在libs文件夹内
    implementation("org.bstats:bstats-bukkit:3.0.2")
    compileOnly(fileTree("libs") { include("*.jar") })
    compileOnly("org.ehcache:ehcache:3.10.8") { isTransitive = false }
    compileOnly("me.clip:placeholderapi:2.11.6") { isTransitive = false }
    compileOnly("fr.xephi:authme:5.6.0-SNAPSHOT") { isTransitive = false }
    compileOnly("net.Indyuce:MMOItems-API:6.9.4-SNAPSHOT") { isTransitive = false }
    compileOnly("com.github.LoneDev6:api-itemsadder:3.4.1-r4") { isTransitive = false }
    compileOnly("com.github.oraxen:oraxen:1.155.3") { isTransitive = false }
    compileOnly("com.gmail.nossr50.mcMMO:mcMMO:2.2.012") { isTransitive = false }
    compileOnly("com.github.MrXiaoM:SweetMail:cef8a6d031") { isTransitive = false }
}

// 插件名称，请在gradle.properties 修改
val pluginName: String by rootProject
//包名，请在gradle.properties 修改
val groupS = project.group as String
// 作者，请在gradle.properties 修改
val author: String by rootProject
// jar包输出路径，请在gradle.properties 修改
val jarOutputFile: String by rootProject
//插件版本，请在gradle.properties 修改

val obfuscated: String by rootProject
val obfuscatedDictionary: String by rootProject
val obfuscationDictionaryFile: File? = if (obfuscatedDictionary.isEmpty()) null
else
    File(obfuscatedDictionary).absoluteFile
val obfuscatedMainClass =
    if (obfuscationDictionaryFile?.exists() == true) {
        obfuscationDictionaryFile.readLines().firstOrNull() ?: "a"
    } else "a"
val isObfuscated = obfuscated == "true"
val shrink: String by rootProject
//val defaultFile = File("../build", "${rootProject.name}-${rootProject.version}.jar")
val formatJarOutput = jarOutputFile.replace("\${root}", rootProject.projectDir.absolutePath)
val output: File =
    if (isObfuscated)
        File(formatJarOutput, "${rootProject.name}-${rootProject.version}-obfuscated.jar").absoluteFile
    else
        File(formatJarOutput, "${rootProject.name}-${rootProject.version}.jar").absoluteFile

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }
    kotlin {
        jvmToolchain(8)
    }

    shadowJar {
        if (isObfuscated) {
            relocate("top.iseason.bukkittemplate.BukkitTemplate", obfuscatedMainClass)
        }
        relocate("top.iseason.bukkittemplate", "$groupS.libs.core")
        relocate("org.bstats", "$groupS.libs.bstats")
        relocate("io.github.bananapuncher714.nbteditor", "$groupS.libs.nbteditor")
//        relocate("net.cinnom:nano-cuckoo", "$groupS.libs.nanocuckoo")
    }
    build {
        dependsOn("buildPlugin")
    }
    processResources {
        filesMatching("plugin.yml") {
            // 删除注释,你可以返回null以删除整行，但是IDEA有bug会报错，故而返回了""
            filter {
                if (it.trim().startsWith("#")) null else it
            }
            expand(
                "main" to if (isObfuscated) obfuscatedMainClass else "$groupS.libs.core.BukkitTemplate",
                "name" to pluginName,
                "version" to project.version,
                "author" to author,
                "kotlinVersion" to getProperties("kotlinVersion"),
                "exposedVersion" to getProperties("exposedVersion"),
                "nbtEditorVersion" to getProperties("nbtEditorVersion")
            )
        }
    }
    dokkaHtml.configure {
        dokkaSourceSets {
            named("main") {
                moduleName.set("SakuraBind")
            }
        }
    }
}
tasks.register<proguard.gradle.ProGuardTask>("buildPlugin") {
    group = "minecraft"
    verbose()
    injars(tasks.named("shadowJar"))
    if (!isObfuscated) {
        dontobfuscate()
    } else if (obfuscationDictionaryFile?.exists() == true) {
        //混淆词典
        classobfuscationdictionary(obfuscationDictionaryFile)
        obfuscationdictionary(obfuscationDictionaryFile)
    }
    if (shrink != "true") {
        dontshrink()
    }
    allowaccessmodification() //优化时允许访问并修改有修饰符的类和类的成员
    dontusemixedcaseclassnames() // 混淆时不要大小写混合
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
    if (isObfuscated) keep(allowObf, "class $obfuscatedMainClass {}")
    else keep("class $groupS.libs.core.BukkitTemplate {}")
    keepkotlinmetadata()
    keep(allowObf, "class * implements $groupS.libs.core.BukkitPlugin {*;}")
    keep("class top.iseason.bukkit.sakurabind.SakuraBindAPI {*;}")
    keepclassmembers("class * extends $groupS.libs.core.config.SimpleYAMLConfig {*;}")
    keepclassmembers("class * implements $groupS.libs.core.ui.container.BaseUI {*;}")
    keepclassmembers(allowObf, "class * implements org.bukkit.event.Listener {*;}")
    keepclassmembers(allowObf, "class * extends org.bukkit.event.Event {*;}")
    keepclassmembers(allowObf, "class * extends org.jetbrains.exposed.dao.id.IdTable {*;}")
    keepclassmembers(allowObf, "class * extends org.jetbrains.exposed.dao.Entity {*;}")
    keepattributes("Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*")
    keepclassmembers("enum * {public static **[] values();public static ** valueOf(java.lang.String);}")
    repackageclasses()
    outjars(output)
}

fun getProperties(properties: String) = rootProject.properties[properties].toString()
