package top.iseason.bukkittemplate.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.ConfigurationSection
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.DisableHook
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.debug
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.dependency.DependencyDownloader
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
    var dbName = "database_${BukkitTemplate.getPlugin().name}"

    @Comment("", "数据库用户名，如果有的话")
    @Key
    var user = "user"

    @Comment("", "数据库密码，如果有的话")
    @Key
    var password = "password"

    // table缓存
    private var tables: Array<out Table> = emptyArray()
    var isConnected = false
        private set
    private var isConnecting = false
    lateinit var connection: Database
        private set
    private var ds: HikariDataSource? = null

    init {
        DisableHook.addTask { closeDB() }
    }

    override fun onLoaded(section: ConfigurationSection) {
        isAutoUpdate = autoReload
        reConnected()
        if (tables.isNotEmpty()) {
            initTables(*tables)
        }
    }

    /**
     * 链接数据库
     */
    fun reConnected() {
        if (isConnecting) return
        info("&6数据库链接中...")
        isConnecting = true
        closeDB()
        runCatching {
            val dd = DependencyDownloader()
                .addRepository("https://maven.aliyun.com/repository/public")
                .addRepository("https://repo.maven.apache.org/maven2/")

            val config = when (database) {
                "MySQL" -> HikariConfig().apply {
                    dd.downloadDependency("mysql:mysql-connector-java:8.0.30")
                    jdbcUrl = "jdbc:mysql://$url/$dbName?createDatabaseIfNotExist=true"
                    driverClassName = "com.mysql.cj.jdbc.Driver"
                }

                "MariaDB" -> HikariConfig().apply {
                    dd.downloadDependency("org.mariadb.jdbc:mariadb-java-client:3.0.7")
                    jdbcUrl = "jdbc:mariadb://$url/$dbName?createDatabaseIfNotExist=true"
                    driverClassName = "org.mariadb.jdbc.Driver"
                }

                "SQLite" -> HikariConfig().apply {
                    dd.downloadDependency("org.xerial:sqlite-jdbc:3.36.0.3")
                    jdbcUrl = "jdbc:sqlite:$url"
                    driverClassName = "org.sqlite.JDBC"
                }

                "H2" -> HikariConfig().apply {
                    dd.downloadDependency("com.h2database:h2:2.1.214")
                    jdbcUrl = "jdbc:h2:$url/$dbName;TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0"
                    driverClassName = "org.h2.Driver"
                }

                "PostgreSQL" -> HikariConfig().apply {
                    dd.downloadDependency("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.9")
                    jdbcUrl = "jdbc:pgsql://$url/$dbName"
                    driverClassName = "com.impossibl.postgres.jdbc.PGDriver"
                }

                "Oracle" -> HikariConfig().apply {
                    dd.downloadDependency("com.oracle.database.jdbc:ojdbc8:21.6.0.0.1")
                    jdbcUrl = "dbc:oracle:thin:@//$url/$dbName"
                    driverClassName = "oracle.jdbc.OracleDriver"
                }

                "SQLServer" -> HikariConfig().apply {
                    dd.downloadDependency("com.microsoft.sqlserver:mssql-jdbc:10.2.1.jre8")
                    jdbcUrl = "jdbc:sqlserver://$url/$dbName"
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
            connection = Database.connect(ds!!, databaseConfig = DatabaseConfig.invoke {
                sqlLogger = MySqlLogger
            })
            isConnected = true
            info("&a数据库链接成功: &6$database")
        }.getOrElse {
            isConnected = false
            it.printStackTrace()
            info("&c数据库链接失败!")
        }
        isConnecting = false
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
        this.tables = tables
        runCatching {
            transaction {
                SchemaUtils.create(*tables)
            }
        }.getOrElse { it.printStackTrace() }
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

object MySqlLogger : SqlLogger {
    override fun log(context: StatementContext, transaction: Transaction) {
        debug("&6DEBUG SQL: &7${context.expandArgs(transaction)}")
    }
}

/**
 * 使用本插件数据库的事务
 */
fun <T> dbTransaction(statement: Transaction.() -> T) =
    transaction(top.iseason.bukkittemplate.config.DatabaseConfig.connection, statement)
