package io.github.algorandecosystem.example.coinflipper

import androidx.room.Room
import io.github.algorandecosystem.example.coinflipper.data.local.DB_FILE_NAME
import io.github.algorandecosystem.example.coinflipper.data.local.ExampleDatabase
import io.github.algorandecosystem.example.coinflipper.data.local.buildExampleDatabase
import io.github.algorandecosystem.example.coinflipper.data.local.createDataStore
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun providePlatformModules() =
    module {
        single<ExampleDatabase> {
            Room
                .databaseBuilder<ExampleDatabase>(name = "${documentDirectory()}/$DB_FILE_NAME")
                .buildExampleDatabase()
        }
        single { createDataStore { "${documentDirectory()}/example_app.preferences_pb" } }
    }

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return requireNotNull(url?.path)
}
