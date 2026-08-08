import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.File

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.cuboidestudio.orionvault.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.cuboidestudio.orionvault"
            packageVersion = "1.0.0"

            val iconsDir = File(projectDir, "src/main/resources/icons")
            macOS {
                iconFile.set(File(iconsDir, "app_icon.icns"))
            }
            windows {
                iconFile.set(File(iconsDir, "app_icon.ico"))
            }
            linux {
                iconFile.set(File(iconsDir, "app_icon.png"))
            }
        }
    }
}