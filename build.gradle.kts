plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

subprojects {
    group = rootProject.group
    version = rootProject.version
    apply {
        plugin<JavaPlugin>()
        plugin<JavaLibraryPlugin>()
    }
    repositories {
        mavenCentral()
        maven {
            name = "aliyun"
            url = uri("https://maven.aliyun.com/repository/public")
        }
        maven {
            name = "aliyun-google"
            url = uri("https://maven.aliyun.com/repository/google")
        }
//        google()
        maven {
            name = "spigot"
            url = uri("https://hub.spigotmc.org/nexus/content/repositories/public/")
        }
        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            name = "jitpack"
            url = uri("https://jitpack.io")
        }
        maven {
            name = "CodeMC"
            url = uri("https://repo.codemc.org/repository/maven-public")
        }
        maven {
            name = "PlaceholderAPI"
            url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        }
        mavenLocal()
    }
    dependencies {
        val kotlinVersion: String by rootProject
        val exposedVersion: String by rootProject
        val nbtEditorVersion: String by rootProject

        compileOnly(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
        //基础库
        compileOnly(kotlin("stdlib"))
        compileOnly("org.spigotmc", "spigot-api", "1.20.3-R0.1-SNAPSHOT", "compile")
        compileOnly(
            "io.papermc.paper", "paper-api", "1.20.3-R0.1-SNAPSHOT", "compile"
        ) {
            isTransitive = false
            exclude("org.bukkit")
        }
        compileOnly("me.clip:placeholderapi:2.11.3")
        implementation("io.github.bananapuncher714:nbteditor:$nbtEditorVersion")

        // 数据库
        compileOnly("org.jetbrains.exposed", "exposed-core", exposedVersion)
        compileOnly("org.jetbrains.exposed", "exposed-dao", exposedVersion)
        compileOnly("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
        compileOnly("org.jetbrains.exposed", "exposed-java-time", exposedVersion)

        compileOnly("com.zaxxer:HikariCP:4.0.3")
    }


}

repositories {
//    阿里的服务器速度快一点
    maven {
        name = "aliyun"
        url = uri("https://maven.aliyun.com/repository/public/")
    }
    google()
    mavenCentral()
}
dependencies {
    //基础库
    compileOnly(kotlin("stdlib"))
}
