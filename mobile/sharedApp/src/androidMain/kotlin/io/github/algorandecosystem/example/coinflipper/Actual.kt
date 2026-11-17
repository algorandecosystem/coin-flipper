package io.github.algorandecosystem.example.coinflipper

import android.content.Context
import androidx.room.Room
import io.github.algorandecosystem.example.coinflipper.data.local.DB_FILE_NAME
import io.github.algorandecosystem.example.coinflipper.data.local.ExampleDatabase
import io.github.algorandecosystem.example.coinflipper.data.local.buildExampleDatabase
import io.github.algorandecosystem.example.coinflipper.data.local.createDataStore
import org.koin.dsl.module

actual fun providePlatformModules() =
    module {
        single<ExampleDatabase> {
            val context: Context = get()
            Room
                .databaseBuilder<ExampleDatabase>(context, context.getDatabasePath(DB_FILE_NAME).absolutePath)
                .buildExampleDatabase()
        }
        single {
            val context: Context = get()
            createDataStore { context.filesDir.resolve("example_app.preferences_pb").absolutePath }
        }
    }
