pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev")
        // Architectury Loom's remap pipeline pulls Forge tooling (mcinjector, DiffPatch).
        maven("https://maven.minecraftforge.net")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/snapshots")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9"
    // Auto-provision the Java 25 toolchain required by Minecraft 26.1.2.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

stonecutter {
    kotlinController = true
    shared {
        // Two build targets: 26.1.2 (unobfuscated, official names, Java 25) and
        // 1.21.4 (obfuscated -> Mojmap, Java 21). Each picks its own buildscript.
        fun mc(vararg versions: String) {
            for (version in versions) {
                val buildscript =
                    if (version == "26.1.2") "build-unobfuscated.gradle.kts"
                    else "build-obfuscated.gradle.kts"
                version("$version-fabric", version).buildscript(buildscript)
            }
        }
        // 26.1.2 first => it is the default "vcs"/commit version, so the committed
        // source stays in canonical 26.1 form (see stonecutter.gradle.kts).
        mc("26.1.2", "1.21.4")
    }
    create(rootProject)
}

rootProject.name = "zombiez-companion"
