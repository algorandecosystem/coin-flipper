import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
}

kotlin {
    android {
        namespace = "io.github.algorandecosystem.example.coinflipper.shared"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        androidResources.enable = true
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        withHostTest {}
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "SharedApp"
            isStatic = true
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("androidx.compose.ui.ExperimentalComposeUiApi")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.animation)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.navigation.compose)
            implementation(libs.bundles.koin)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.datastore.preferences)
            implementation(libs.kotlincrypto.sha2)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.napier)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.wallet.sdk.core)
            implementation(libs.wallet.sdk.ui)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activityCompose)
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }

        getByName("androidHostTest").dependencies {
            implementation(kotlin("test-junit"))
            implementation(libs.algosdk)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

compose.resources {
    packageOfResClass = "io.github.algorandecosystem.example.coinflipper.generated.resources"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

ktlint {
    version.set(libs.versions.ktlint)
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}

// CI runs `./gradlew test`, which the KMP plugin does not define for this module. Run the shared
// tests (commonTest + androidHostTest) on the JVM so they are covered on any CI runner.
tasks.register("test") {
    group = "verification"
    description = "Runs the shared module's unit tests on the JVM."
    dependsOn("testAndroidHostTest")
}
