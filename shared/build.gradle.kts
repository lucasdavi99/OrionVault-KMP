import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

// --- Configuração do Supabase gerada em build time (plano, fase 6.2) ---
// A URL e a chave `anon` NUNCA ficam literais em código versionado. Elas vêm de
// `secrets.properties` na raiz do repositório (git-ignorado) ou das variáveis de ambiente
// SUPABASE_URL / SUPABASE_ANON_KEY (CI). O arquivo Kotlin resultante é escrito em
// build/generated/, portanto nunca existe dentro de uma source set versionada.
val supabaseSecrets = Properties().apply {
    val file = rootProject.file("secrets.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val supabaseUrl: String =
    (supabaseSecrets.getProperty("SUPABASE_URL") ?: System.getenv("SUPABASE_URL"))?.trim().orEmpty()
val supabaseAnonKey: String =
    (supabaseSecrets.getProperty("SUPABASE_ANON_KEY") ?: System.getenv("SUPABASE_ANON_KEY"))?.trim().orEmpty()

val generateSupabaseConfig = tasks.register("generateSupabaseConfig") {
    val outputDir = layout.buildDirectory.dir("generated/supabaseConfig/kotlin")
    outputs.dir(outputDir)
    inputs.property("supabaseUrl", supabaseUrl)
    inputs.property("supabaseAnonKey", supabaseAnonKey)
    val url = supabaseUrl
    val anonKey = supabaseAnonKey
    doLast {
        if (url.isBlank() || anonKey.isBlank()) {
            error(
                """
                Configuração do Supabase ausente.

                Defina SUPABASE_URL e SUPABASE_ANON_KEY em `secrets.properties` na raiz do
                repositório (git-ignorado) ou como variáveis de ambiente. Exemplo:

                    SUPABASE_URL=https://<project-ref>.supabase.co
                    SUPABASE_ANON_KEY=<chave anon publica do projeto>

                Passo a passo completo em backend/supabase/README.md.
                NUNCA use a chave `service_role` aqui - ela ignora RLS.
                """.trimIndent()
            )
        }
        val packageDir = outputDir.get().asFile.resolve("com/cuboidestudio/orionvault/network")
        packageDir.mkdirs()
        packageDir.resolve("GeneratedSupabaseConfig.kt").writeText(
            """
            // GERADO AUTOMATICAMENTE por :shared:generateSupabaseConfig - não edite, não versione.
            package com.cuboidestudio.orionvault.network

            internal object GeneratedSupabaseConfig {
                const val URL: String = "$url"
                const val ANON_KEY: String = "$anonKey"
            }
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    targets.configureEach {
        compilations.configureEach {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    jvm()
    
    android {
       namespace = "com.cuboidestudio.orionvault.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        commonMain {
            kotlin.srcDir(generateSupabaseConfig)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.libsodium.bindings)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.jna.platform)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
            implementation(libs.ktor.client.darwin)
        }
    }
}

sqldelight {
    databases {
        create("OrionVaultDatabase") {
            packageName.set("com.cuboidestudio.orionvault.storage.db")
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}