package io.github.algorandecosystem.example.coinflipper.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.algorandecosystem.example.coinflipper.data.local.dao.ExampleDao
import io.github.algorandecosystem.example.coinflipper.data.local.entities.ValidatorEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [ValidatorEntity::class], version = 5, exportSchema = false)
@ConstructedBy(ExampleDatabaseConstructor::class)
abstract class ExampleDatabase : RoomDatabase() {
    abstract fun getDao(): ExampleDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object ExampleDatabaseConstructor : RoomDatabaseConstructor<ExampleDatabase> {
    override fun initialize(): ExampleDatabase
}

internal const val DB_FILE_NAME = "exampledatabase.db"

/** Finishes a platform-specific [RoomDatabase.Builder] with the shared driver and settings. */
fun RoomDatabase.Builder<ExampleDatabase>.buildExampleDatabase(): ExampleDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
