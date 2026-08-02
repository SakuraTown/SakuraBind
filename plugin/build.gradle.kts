plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
    id("org.jetbrains.dokka") version "2.2.0"
}

buildscript {
    dependencies {
        val proguardVersion = rootProject.providers.gradleProperty("proguardVersion").get()
        classpath("com.guardsquare:proguard-gradle:$proguardVersion")
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
    maven {
        name = "Oraxen"
        url = uri("https://repo.oraxen.com/releases")
    }
    maven {
        name = "NeigeItems"
        url = uri("https://r.irepo.space/maven/")
    }
    maven {
        name = "MythicMobs"
        url = uri("https://mvn.lumine.io/repository/maven-public/")
    }

}

dependencies {
// 依赖core模块
    api(project(":core"))
//    反射库
//    compileOnly(kotlin("reflect"))

//    协程库
//    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:2.2.0")

// 本地依赖放在libs文件夹内
    implementation("org.bstats:bstats-bukkit:3.1.0")
    compileOnly(fileTree("libs") { include("*.jar") })
    compileOnly("org.ehcache:ehcache:3.11.1") { isTransitive = false }
    compileOnly("me.clip:placeholderapi:2.11.6") { isTransitive = false }
    compileOnly("fr.xephi:authme:5.6.0-SNAPSHOT") { isTransitive = false }
    compileOnly("net.Indyuce:MMOItems-API:6.9.4-SNAPSHOT") { isTransitive = false }
    compileOnly("com.github.LoneDev6:api-itemsadder:3.4.1-r4") { isTransitive = false }
    compileOnly("io.th0rgal:oraxen:1.189.0") { isTransitive = false }
    compileOnly("com.gmail.nossr50.mcMMO:mcMMO:2.2.012") { isTransitive = false }
    compileOnly("com.github.MrXiaoM:SweetMail:1.0.3") { isTransitive = false }
    compileOnly("net.william278:husksync:3.2.1") { isTransitive = false }
    compileOnly("pers.neige.neigeitems:NeigeItems:1.21.160") { isTransitive = false }
    compileOnly("io.lumine:Mythic-Dist:5.1.2") { isTransitive = false }
}

// 插件名称，请在gradle.properties 修改
val pluginName = rootProject.providers.gradleProperty("pluginName").get()
val version = rootProject.providers.gradleProperty("version").get()
//包名，请在gradle.properties 修改
val groupS = project.group as String
// 作者，请在gradle.properties 修改
val author = rootProject.providers.gradleProperty("author").get()
// jar包输出路径，请在gradle.properties 修改
val jarOutputFile = rootProject.providers.gradleProperty("jarOutputFile").get()
//插件版本，请在gradle.properties 修改

val obfuscated = rootProject.providers.gradleProperty("obfuscated").get()
val obfuscatedDictionary = rootProject.providers.gradleProperty("obfuscatedDictionary").get()
val obfuscationDictionaryFile: File? = if (obfuscatedDictionary.isEmpty()) null
else
    File(obfuscatedDictionary).absoluteFile
val obfuscatedMainClass =
    if (obfuscationDictionaryFile?.exists() == true) {
        obfuscationDictionaryFile.readLines().firstOrNull() ?: "a"
    } else "a"
val isObfuscated = obfuscated == "true"
val shrink = rootProject.providers.gradleProperty("shrink").get()
val java8Launcher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(8))
}
//val defaultFile = File("../build", "${rootProject.name}-${rootProject.version}.jar")
val formatJarOutput = jarOutputFile.replace($$"${root}", rootProject.projectDir.absolutePath)
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
        inputs.property("obfuscated", isObfuscated)
        filesMatching("META-INF/*.kotlin_module") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        if (isObfuscated) {
            relocate("top.iseason.bukkittemplate.BukkitTemplate", obfuscatedMainClass)
        }
        relocate("top.iseason.bukkittemplate", "$groupS.libs.core")
        relocate("org.bstats", "$groupS.libs.bstats")
//        relocate("io.github.bananapuncher714.nbteditor", "$groupS.libs.nbteditor")
        relocate("com.github.mgunlogson.cuckoofilter4j", "$groupS.libs.cuckoofilter")
//        relocate("net.cinnom:nano-cuckoo", "$groupS.libs.nanocuckoo")
    }
    processResources {
        val resourceProperties = mapOf(
            "main" to if (isObfuscated) obfuscatedMainClass else "$groupS.libs.core.BukkitTemplate",
            "name" to pluginName,
            "version" to version,
            "author" to author,
            "kotlinVersion" to getProperties("kotlinVersion"),
            "exposedVersion" to getProperties("exposedVersion")
        )
        inputs.properties(resourceProperties)
        filesMatching("plugin.yml") {
            // 删除注释,你可以返回null以删除整行，但是IDEA有bug会报错，故而返回了""
            filter {
                if (it.trim().startsWith("#")) null else it
            }
            expand(resourceProperties)
        }
    }
}
dokka {
    moduleName.set("SakuraBind")
}
tasks.named("build") {
    dependsOn("buildPlugin")
}
tasks.register<proguard.gradle.ProGuardTask>("buildPlugin") {
    group = "minecraft"
    description = "Shrinks and optionally obfuscates the shadow plugin jar"
    inputs.file(layout.projectDirectory.file("build.gradle.kts"))
    inputs.property("obfuscated", isObfuscated)
    inputs.property("shrink", shrink)
    obfuscationDictionaryFile?.takeIf(File::isFile)?.let(inputs::file)
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
    dontusemixedcaseclassnames() // 混淆时不要大小写混合
    optimizationpasses(3)

    // 使用与编译目标一致的 Java 8 完整运行库。
    val java8Home = java8Launcher.get().metadata.installationPath.asFile
    val java8Runtime = listOf(
        java8Home.resolve("jre/lib/rt.jar"),
        java8Home.resolve("lib/rt.jar")
    ).firstOrNull(File::isFile)
        ?: throw GradleException("Java 8 runtime library rt.jar was not found under $java8Home")
    libraryjars(java8Runtime)
    // Shadow JAR 包含 core 代码，合并并去重两个模块的 compile-only API。
    val libraryClasspath = (
            configurations.compileClasspath.get().files +
                    project(":core").configurations.getByName("compileClasspath").files
            ).distinct()
    libraryjars(libraryClasspath)

    // Bukkit 服务端及其扩展均为 compile-only 的可选运行时依赖，
    // ProGuard 无法在构建期完整解析这些 API。
    dontwarn()

    val reportVariant = if (isObfuscated) "obfuscated" else "shrunk"
    val reportsDirectory = layout.buildDirectory.dir("reports/proguard/$reportVariant").get().asFile
    reportsDirectory.mkdirs()
    outputs.dir(reportsDirectory)
    printmapping(reportsDirectory.resolve("mapping.txt"))
    printseeds(reportsDirectory.resolve("seeds.txt"))
    printusage(reportsDirectory.resolve("usage.txt"))
    //启用混淆的选项
    val allowObf = mapOf("allowobfuscation" to true)
    //class规则
    if (isObfuscated) keep("class $obfuscatedMainClass { *; }")
    else keep("class $groupS.libs.core.BukkitTemplate {}")
    keepkotlinmetadata()
    keep(allowObf, "class * implements $groupS.libs.core.BukkitPlugin {*;}")
    keepclassmembers("class * implements $groupS.libs.core.BukkitPlugin { public static final ** INSTANCE; }")
    // 保持所有对外 API 的类名和公开成员。
    keep("public class top.iseason.bukkit.sakurabind.SakuraBindAPI { public protected *; }")
    keep("public class top.iseason.bukkit.sakurabind.event.** { public protected *; }")
    keep("public interface top.iseason.bukkit.sakurabind.config.BaseSetting { public protected *; }")
    keep("public class top.iseason.bukkit.sakurabind.cache.BlockInfo { public protected *; }")
    keep("public abstract class top.iseason.bukkit.sakurabind.pickers.BasePicker { public protected *; }")
    keep("public enum top.iseason.bukkit.sakurabind.utils.BindType { *; }")
    keep("public enum top.iseason.bukkit.sakurabind.utils.SendBackType { *; }")
    // CuckooFilter 使用 Java 默认序列化持久化到磁盘，类名和字段名必须跨版本稳定。
    keep("class $groupS.libs.cuckoofilter.** { *; }")
    keepclassmembers(
        allowObf, """class * implements java.io.Serializable{
        |static final long serialVersionUID;
        |private void writeObject(java.io.ObjectOutputStream);
        |private void readObject(java.io.ObjectInputStream);
        |}
    """.trimMargin()
    )
    keepclassmembers("class * extends $groupS.libs.core.config.SimpleYAMLConfig {*;}")
    keepclassmembers("class * implements $groupS.libs.core.ui.container.BaseUI {*;}")
    keepclassmembers(allowObf, "class * implements org.bukkit.event.Listener {*;}")
    keepclassmembers(allowObf, "class * extends org.bukkit.event.Event {*;}")
    keepclassmembers(allowObf, "class * extends org.jetbrains.exposed.v1.core.dao.id.IdTable {*;}")
    keepattributes("Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*")
    keepclassmembers("enum * {public static **[] values();public static ** valueOf(java.lang.String);}")
    if (isObfuscated) repackageclasses("$groupS.internal")
    outjars(output)
}

fun getProperties(propertyName: String) = rootProject.providers.gradleProperty(propertyName).get()
