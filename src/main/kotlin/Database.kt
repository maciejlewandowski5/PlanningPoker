package com.example

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

fun Application.configureDatabase() {
    val url = System.getenv("DATABASE_URL")
        ?: environment.config.propertyOrNull("database.url")?.getString()
        ?: "jdbc:sqlite:./data/planningpoker.db"

    if (url.startsWith("jdbc:sqlite:") && !url.contains(":memory:")) {
        val path = url.removePrefix("jdbc:sqlite:")
        File(path).parentFile?.mkdirs()
    }

    Database.connect(url, driver = "org.sqlite.JDBC")
    transaction {
        SchemaUtils.createMissingTablesAndColumns(Rooms, Participants, Votes)
    }
}
