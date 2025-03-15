package ru.kainlight.lightcutter.data

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.api.IRegion
import ru.kainlight.lightcutter.api.Region
import ru.kainlight.lightcutter.api.RegionHandler
import ru.kainlight.lightlibrary.UTILS.DebugBukkit
import ru.kainlight.lightlibrary.UTILS.useCatching
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

@Suppress("UNUSED")
internal class Database(private val plugin: Main) : RegionHandler {

    init {
        initializeCache()
    }

    private val host: String = plugin.config.getString("database-settings.host", "localhost") !!
    private val port: Int = plugin.config.getInt("database-settings.port", 3306)
    private val base: String = plugin.config.getString("database-settings.base", "lightcutter") !!

    private var dataSource: HikariDataSource? = null
    private val config: HikariConfig = HikariConfig()

    private val columnName = "lightcutter_regions"

    private val cache: ConcurrentHashMap<String, Region> = ConcurrentHashMap()
    private val caching: Boolean = plugin.config.getBoolean("database-settings.caching", false)

    private fun configureDataSource(driverClassName: String, jdbcUrl: String, sqlite: Boolean = false) {
        config.driverClassName = driverClassName
        config.jdbcUrl = if (sqlite) "jdbc:sqlite://$jdbcUrl" else "$jdbcUrl$host:$port/$base"
        config.username = plugin.config.getString("database-settings.user", "root") !!
        config.password = plugin.config.getString("database-settings.password", "") !!
        config.maximumPoolSize = plugin.config.getInt("database-settings.pool-size", 2)
        config.poolName = "LightCutter-Pool"

        dataSource = HikariDataSource(config)
    }

    fun connect() {
        when (plugin.config.getString("database-settings.storage", "sqlite")!!.lowercase()) {
            "mysql" -> configureDataSource("com.mysql.cj.jdbc.Driver", "jdbc:mysql://")
            "mariadb" -> configureDataSource("org.mariadb.jdbc.Driver", "jdbc:mariadb://")
            "postgresql" -> configureDataSource("org.postgresql.Driver", "jdbc:postgresql://")
            "sqlite" -> {
                val dbFile = File(plugin.dataFolder, "$base.db")
                if (! dbFile.exists()) {
                    try {
                        dbFile.createNewFile()
                    } catch (e: IOException) {
                        DebugBukkit.error(e.message.toString())
                    }
                }
                configureDataSource("org.sqlite.JDBC", dbFile.absolutePath, true)
            }
        }
    }

    fun disconnect() {
        try {
            if (isConnected()) dataSource?.close()
        } catch (e: Exception) {
            DebugBukkit.error(e.message.toString())
        }
    }

    private fun isConnected(): Boolean = dataSource != null && ! (dataSource?.isClosed ?: true)

    fun createTables() {
        executeUpdate(
            "CREATE TABLE IF NOT EXISTS $columnName (region_name VARCHAR(64) PRIMARY KEY, earn VARCHAR(16), need_break INT UNSIGNED, cooldown INT UNSIGNED)"
        )
    }

    override fun addRegion(name: String, earn: String, needBreak: Int, cooldown: Int): Region? {
        val rowsAffected = executeUpdate(
            """
            INSERT INTO $columnName (region_name, earn, need_break, cooldown) 
            SELECT ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM $columnName WHERE region_name = ?)
            """
        ) {
            it.setString(1, name)
            it.setString(2, earn)
            it.setInt(3, needBreak)
            it.setInt(4, cooldown)
            it.setString(5, name)
        }
        val region = IRegion(name, earn, needBreak, cooldown)

        return if(rowsAffected > 0) {
            if(caching) cache.put(name, region)
            region
        } else null
    }

    override fun removeRegion(name: String): Int {
        val rowsAffected = executeUpdate("DELETE FROM $columnName WHERE region_name = ?") {
            it.setString(1, name)
        }
        if (caching && rowsAffected > 0) cache.remove(name)
        return rowsAffected
    }

    override fun updateRegion(region: Region): Int {
        val rowsAffected = executeUpdate(
            "UPDATE $columnName SET earn = ?, need_break = ?, cooldown = ? WHERE region_name = ?"
        ) {
            it.setString(1, region.earn)
            it.setInt(2, region.needBreak)
            it.setInt(3, region.cooldown)
            it.setString(4, region.name)
        }
        if (caching && rowsAffected > 0) cache.put(region.name, region)
        return rowsAffected
    }

    override fun hasRegion(name: String): Boolean {
        return if (caching) {
            getRegion(name) != null
        } else {
            executeQuery(
                "SELECT 1 FROM $columnName WHERE region_name = ?",
                { it.setString(1, name) }
            ) { true } != null
        }
    }

    override fun getRegion(name: String): Region? {
        return if (caching) {
            cache.get(name) ?: fetchRegionFromDatabase(name)?.also { cache.put(name, it) }
        } else {
            fetchRegionFromDatabase(name)
        }
    }

    override fun getRegions(): List<Region> {
        return if (caching) cache.values.toList() else fetchAllRegionsFromDatabase()
    }

    private fun initializeCache() {
        if (caching) {
            fetchAllRegionsFromDatabase().forEach { region ->
                cache[region.name] = region
            }
        }
    }

    private fun fetchRegionFromDatabase(name: String): Region? {
        return executeQuery(
            "SELECT * FROM $columnName WHERE region_name = ?",
            { it.setString(1, name) }
        ) { resultSet ->
            IRegion(
                name = resultSet.getString("region_name"),
                earn = resultSet.getString("earn"),
                needBreak = resultSet.getInt("need_break"),
                cooldown = resultSet.getInt("cooldown")
            )
        }
    }

    private fun fetchAllRegionsFromDatabase(): List<Region> {
        return executeQuery("SELECT * FROM $columnName", mapper = { resultSet ->
            mutableListOf<IRegion>().apply {
                do {
                    add(
                        IRegion(
                            name = resultSet.getString("region_name"),
                            earn = resultSet.getString("earn"),
                            needBreak = resultSet.getInt("need_break"),
                            cooldown = resultSet.getInt("cooldown")
                        )
                    )
                } while (resultSet.next())
            }
        }) ?: emptyList()
    }

    private fun <T> executeQuery(sql: String, setter: (java.sql.PreparedStatement) -> Unit = {}, mapper: (java.sql.ResultSet) -> T?): T? {
        dataSource?.connection.useCatching { connection ->
            connection?.prepareStatement(sql).useCatching { statement ->
                setter(statement !!)
                statement.executeQuery().useCatching { resultSet ->
                    return if (resultSet.next()) mapper(resultSet) else null
                }
            }
        }
        return null
    }

    private fun executeUpdate(sql: String, setter: (java.sql.PreparedStatement) -> Unit = {}): Int {
        dataSource?.connection.useCatching { connection ->
            connection?.prepareStatement(sql).useCatching { statement ->
                setter(statement !!)
                return statement.executeUpdate()
            }
        }
        return 0
    }
}

