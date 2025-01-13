package ru.kainlight.lightcutter.DATA

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.UTILS.Debug
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

@Suppress("UNUSED")
class Database(private val plugin: Main) {

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
                        Debug.log(e.message.toString(), Level.SEVERE)
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
            Debug.log(e.message.toString(), Level.SEVERE)
        }
    }

    private fun isConnected(): Boolean = dataSource != null && ! (dataSource?.isClosed ?: true)

    fun createTables() {
        executeUpdate(
            "CREATE TABLE IF NOT EXISTS $columnName (region_name VARCHAR(64) PRIMARY KEY, earn VARCHAR(16), need_break INT UNSIGNED, cooldown INT UNSIGNED)"
        )
    }

    fun initializeCache() {
        if (caching) {
            fetchAllRegionsFromDatabase().forEach { region ->
                cache[region.name] = region
            }
        }
    }

    fun insertRegion(region: Region): Int {
        val rowsAffected = executeUpdate(
            """
            INSERT INTO $columnName (region_name, earn, need_break, cooldown) 
            SELECT ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM $columnName WHERE region_name = ?)
            """
        ) {
            it.setString(1, region.name)
            it.setString(2, region.earn)
            it.setInt(3, region.needBreak)
            it.setInt(4, region.cooldown)

            it.setString(5, region.name)
        }
        if (caching && rowsAffected > 0) cache.put(region.name, region)
        return rowsAffected
    }

    fun removeRegion(name: String): Int {
        val rowsAffected = executeUpdate("DELETE FROM $columnName WHERE region_name = ?") {
            it.setString(1, name)
        }
        if (caching && rowsAffected > 0) cache.remove(name)
        return rowsAffected
    }

    fun updateRegion(region: Region): Int {
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

    fun hasRegion(name: String): Boolean {
        return if (caching) {
            cache.containsKey(name) || getRegion(name) != null
        } else {
            executeQuery(
                "SELECT 1 FROM $columnName WHERE region_name = ?",
                { it.setString(1, name) }
            ) { true } != null
        }
    }

    fun getRegion(name: String): Region? {
        return if (caching) {
            cache[name] ?: fetchRegionFromDatabase(name)?.also { cache[name] = it }
        } else {
            fetchRegionFromDatabase(name)
        }
    }

    fun getRegions(): List<Region> {
        return if (caching) cache.values.toList() else fetchAllRegionsFromDatabase()
    }

    private fun fetchRegionFromDatabase(name: String): Region? {
        return executeQuery(
            "SELECT * FROM $columnName WHERE region_name = ?",
            { it.setString(1, name) }
        ) { resultSet ->
            Region(
                name = resultSet.getString("region_name"),
                earn = resultSet.getString("earn"),
                needBreak = resultSet.getInt("need_break"),
                cooldown = resultSet.getInt("cooldown")
            )
        }
    }

    private fun fetchAllRegionsFromDatabase(): List<Region> {
        return executeQuery("SELECT * FROM $columnName", mapper = { resultSet ->
            mutableListOf<Region>().apply {
                do {
                    add(
                        Region(
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

    private fun <T> executeQuery(
        sql: String,
        setter: (java.sql.PreparedStatement) -> Unit = {},
        mapper: (java.sql.ResultSet) -> T?
    ): T? {
        dataSource?.connection.use { connection ->
            connection?.prepareStatement(sql).use { statement ->
                setter(statement !!)
                statement.executeQuery().use { resultSet ->
                    return if (resultSet.next()) mapper(resultSet) else null
                }
            }
        }
        return null
    }

    private fun executeUpdate(sql: String, setter: (java.sql.PreparedStatement) -> Unit = {}): Int {
        dataSource?.connection.use { connection ->
            connection?.prepareStatement(sql).use { statement ->
                setter(statement !!)
                return statement.executeUpdate()
            }
        }
        return 0
    }
}

