package top.iseason.bukkit.bukkittemplate.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.ConfigurationSection
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.AutoDisable
import top.iseason.bukkit.bukkittemplate.BukkitTemplate
import top.iseason.bukkit.bukkittemplate.config.annotations.Comment
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key
import top.iseason.bukkit.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkit.bukkittemplate.debug.info
import top.iseason.bukkit.bukkittemplate.dependency.DependencyDownloader
import java.io.File

@FilePath("database.yml")
object DatabaseConfig : SimpleYAMLConfig() {
    @Key
    @Comment("", "是否自动重连数据库")
    var autoReload = true

    @Comment("", "数据库类型: 支持 MySQL、MariaDB、SQLite、H2、Oracle、PostgreSQL、SQLServer")
    @Key
    var database = "H2"

    @Comment("", "数据库地址")
    @Key
    var url = File(BukkitTemplate.getPlugin().dataFolder, "database").absoluteFile.toString()

    @Comment("", "数据库名")
    @Key
    var dbName = "database-${BukkitTemplate.getPlugin().name}"

    @Comment("", "数据库用户名，如果有的话")
    @Key
    var user = "user"

    @Comment("", "数据库密码，如果有的话")
    @Key
    var password = "password"

    var isConnected = false
    private lateinit var connection: Database
    private var ds: HikariDataSource? = null

    override val onLoaded: (ConfigurationSection.() -> Unit) = {
        isAutoUpdate = autoReload
        reConnected()
    }

    /**
     * 链接数据库
     */
    fun reConnected() {
        info("&6数据库链接中...")
        AutoClose
        closeDB()
        runCatching {
            val dd = DependencyDownloader().apply {
                repositories.clear()
                addRepository("https://maven.aliyun.com/repository/public")
                addRepository("https://repo.maven.apache.org/maven2/")
            }
            val config = when (database) {
                "MySQL" -> HikariConfig().apply {
                    dd.downloadDependency("mysql:mysql-connector-java:8.0.30")
                    jdbcUrl = "jdbc:mysql://$url"
                    driverClassName = "com.mysql.cj.jdbc.Driver"
                }

                "MariaDB" -> HikariConfig().apply {
                    dd.downloadDependency("org.mariadb.jdbc:mariadb-java-client:3.0.7")
                    jdbcUrl = "jdbc:mariadb://$url"
                    driverClassName = "org.mariadb.jdbc.Driver"
                }

                "SQLite" -> HikariConfig().apply {
                    dd.downloadDependency("org.xerial:sqlite-jdbc:3.36.0.3")
                    jdbcUrl = "jdbc:sqlite:$url"
                    driverClassName = "org.sqlite.JDBC"
                }

                "H2" -> HikariConfig().apply {
                    dd.downloadDependency("com.h2database:h2:2.1.214")
                    jdbcUrl = "jdbc:h2:$url;TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0"
                    driverClassName = "org.h2.Driver"
                }

                "PostgreSQL" -> HikariConfig().apply {
                    dd.downloadDependency("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.9")
                    jdbcUrl = "jdbc:postgresql://$url"
                    driverClassName = "com.impossibl.postgres.jdbc.PGDriver"
                }

                "Oracle" -> HikariConfig().apply {
                    dd.downloadDependency("com.oracle.database.jdbc:ojdbc8:21.6.0.0.1")
                    jdbcUrl = "dbc:oracle:thin:@//$url"
                    driverClassName = "oracle.jdbc.OracleDriver"
                }

                "SQLServer" -> HikariConfig().apply {
                    dd.downloadDependency("com.microsoft.sqlserver:mssql-jdbc:10.2.1.jre8")
                    jdbcUrl = "jdbc:sqlserver://$url"
                    driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
                }

                else -> throw Exception("错误的数据库类型!")
            }
            with(config) {
                username = this@DatabaseConfig.user
                password = this@DatabaseConfig.password
                isAutoCommit = true
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                poolName = BukkitTemplate.getPlugin().name
            }
            ds = HikariDataSource(config)
            connection = Database.connect(ds!!)
            isConnected = true
            info("&a数据库链接成功!")
        }.getOrElse {
            isConnected = false
            it.printStackTrace()
            info("&c数据库链接失败!")
        }
    }

    /**
     * 关闭数据库
     */
    fun closeDB() {
        if (!isConnected) return
        runCatching {
            ds?.close()
            TransactionManager.closeAndUnregister(connection)
            isConnected = false
        }.getOrElse { it.printStackTrace() }
    }

    /**
     * 初始化表
     */
    fun initTables(vararg tables: Table) {
        if (!isConnected) return
        runCatching {
            transaction {
                if (SimpleLogger.isDebug) addLogger(StdOutSqlLogger)
                if (!database.equals("sqlite", true)) {
                    val schema = Schema(dbName)
                    SchemaUtils.createSchema(schema)
                    SchemaUtils.setSchema(schema)
                }
                SchemaUtils.create(*tables)
            }
        }.getOrElse { it.printStackTrace() }
    }

    object AutoClose : AutoDisable() {
        override fun onDisable() {
            closeDB()
        }
    }
}

/**
 * varchar(255) 作为主键的table
 */
open class StringIdTable(name: String = "", columnName: String = "id") : IdTable<String>(name) {
    final override val id: Column<EntityID<String>> = varchar(columnName, 255).entityId()
    final override val primaryKey = PrimaryKey(id)
}

abstract class StringEntity(id: EntityID<String>) : Entity<String>(id)

abstract class StringEntityClass<out E : Entity<String>> constructor(
    table: IdTable<String>,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<String>) -> E)? = null
) : EntityClass<String, E>(table, entityType, entityCtor)