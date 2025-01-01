package ru.kainlight.lightcutter.DATA

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import ru.kainlight.lightcutter.Main
import java.io.File
import java.io.IOException


class Database(
    val plugin: Main,
    val storage: String = plugin.config.getString("database-settings.storage", "sqlite")!!.lowercase(),
    val host: String = plugin.config.getString("database-settings.host", "localhost") !!,
    val port: Int = plugin.config.getInt("database-settings.port", 3306),
    val base: String = plugin.config.getString("database-settings.base", "lightcutter") !!,
    val user: String = plugin.config.getString("database-settings.user", "root") !!,
    val password: String = plugin.config.getString("database-settings.password", "") !!,
    val poolSize: Int = plugin.config.getInt("database-settings.pool-size", 2),
) {

    var dataSource: HikariDataSource? = null
    private val config: HikariConfig = HikariConfig()

    private fun configureDataSource(driverClassName: String, jdbcUrl: String, sqlite: Boolean = false) {
        config.driverClassName = driverClassName

        if (sqlite) config.jdbcUrl = "jdbc:sqlite://$jdbcUrl"
        else config.jdbcUrl = "$jdbcUrl$host:$port/$base"

        config.username = user
        config.password = password
        config.maximumPoolSize = poolSize

        dataSource = HikariDataSource(config)
    }

    fun connect() {
        when (storage) {
            "mariadb" -> configureDataSource("org.mariadb.jdbc.Driver", "jdbc:mariadb://", false)
            "mysql" -> configureDataSource("com.mysql.cj.jdbc.Driver", "jdbc:mysql://", false)
            "postgresql" -> configureDataSource("org.postgresql.Driver", "jdbc:postgresql://", false)
            "sqlite" -> {
                val dbFile = File(plugin.dataFolder, "$base.db")
                if (!dbFile.exists()) {
                    try {
                        dbFile.createNewFile()
                    } catch (e: IOException) {
                        e.printStackTrace()
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
            plugin.logger.info("DATABASE: ${e.message}")
        }
    }

    private fun isConnected(): Boolean {
        return dataSource != null && !dataSource?.isClosed!!
    }

    fun createTables() {
        dataSource?.connection.use { connection ->
            connection?.createStatement().use { statement ->
                statement?.execute("CREATE TABLE IF NOT EXISTS lightcutter_regions (region_name VARCHAR(64) PRIMARY KEY, earn VARCHAR(16), need_break INT UNSIGNED, cooldown INT UNSIGNED)")
            }
        }
    }

    fun insertRegion(region: Region) {
        if (isRegion(region.name)) return

        dataSource!!.connection.use { connection ->
            connection!!.prepareStatement("INSERT INTO lightcutter_regions VALUES (?, ?, ?, ?)").use { statement ->
                statement.setString(1, region.name)
                statement.setString(2, region.earn)
                statement.setInt(3, region.needBreak)
                statement.setInt(4, region.cooldown)
                statement.executeUpdate()
            }
        }
    }

    fun removeRegion(name: String) {
        if (!isRegion(name)) return

        dataSource!!.connection.use { connection ->
            connection.prepareStatement("DELETE FROM lightcutter_regions WHERE region_name = ?").use { statement ->
                statement.setString(1, name)
                statement.executeUpdate()
            }
        }
    }

    fun updateRegion(region: Region) {
        if (! isRegion(region.name)) return

        dataSource!!.connection.use { connection ->
            connection.prepareStatement("UPDATE lightcutter_regions SET earn = ?, need_break = ?, cooldown = ? WHERE region_name = ?")
                .use { statement ->
                    statement.setString(1, region.earn)
                    statement.setInt(2, region.needBreak)
                    statement.setInt(3, region.cooldown)
                    statement.setString(4, region.name)
                    statement.executeUpdate()
                }
        }
    }

    fun isRegion(name: String): Boolean {
        dataSource?.connection.use { connection ->
            connection?.prepareStatement("SELECT * FROM lightcutter_regions WHERE region_name = ?").use { statement ->
                statement?.setString(1, name)
                statement?.executeQuery().use { resultSet ->
                    if (resultSet != null) {
                        return resultSet.next()
                    } else return false
                }
            }
        }
    }

    fun getRegions(): MutableList<Region> {
        val regionNames: MutableList<Region> = mutableListOf()
        dataSource?.connection.use { connection ->
            connection?.prepareStatement("SELECT * FROM lightcutter_regions").use { statement ->
                statement?.executeQuery().use { resultSet ->
                    while (resultSet?.next() == true) {
                        val name = resultSet.getString("region_name")
                        val earn = resultSet.getString("earn")
                        val needBreak = resultSet.getInt("need_break")
                        val cooldown = resultSet.getInt("cooldown")

                        val region = Region(name, earn, needBreak, cooldown)
                        regionNames.add(region)
                    }
                }
            }
        }
        return regionNames
    }

    fun getRegion(name: String): Region? {
        dataSource?.connection.use { connection ->
            connection?.prepareStatement("SELECT * FROM lightcutter_regions WHERE region_name = ?").use { statement ->
                    statement?.setString(1, name)
                    statement?.executeQuery().use { resultSet ->
                        if (resultSet?.next() == true) {
                            val earn = resultSet.getString("earn")
                            val needBreak = resultSet.getInt("need_break")
                            val cooldown = resultSet.getInt("cooldown")
                            return Region(name, earn, needBreak, cooldown)
                        } else {
                            return null
                        }
                    }
                }
        }
    }

}