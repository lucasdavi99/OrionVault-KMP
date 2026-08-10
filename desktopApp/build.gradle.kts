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
    implementation(compose.material3)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.cuboidestudio.orionvault.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            modules("java.sql", "java.naming", "java.desktop", "java.instrument", "jdk.unsupported")
            packageName = "OrionVault"
            packageVersion = "0.1.0"

            val iconsDir = File(projectDir, "src/main/resources/icons")
            macOS {
                iconFile.set(File(iconsDir, "app_icon.icns"))
            }
            windows {
                iconFile.set(File(iconsDir, "app_icon.ico"))
                menuGroup = "OrionVault"
                perUserInstall = true
                shortcut = true          // atalho na área de trabalho
                menu = true               // entrada no Menu Iniciar (essencial pra aparecer na busca)
                upgradeUuid = "51e2faa2-8c92-475a-a89b-059bed416d5c"
            }
            linux {
                iconFile.set(File(iconsDir, "app_icon.png"))
            }
        }
    }
}