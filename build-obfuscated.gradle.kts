// Build target: Minecraft 1.21.4 (obfuscated). Uses Architectury Loom's remap
// pipeline with official Mojang mappings (Yarn is dead). Java 21.
plugins {
    id("dev.architectury.loom") version "1.13-SNAPSHOT"
    id("com.modrinth.minotaur") version "2.+"
    `maven-publish`
}

val minecraftVersion = property("minecraft_version").toString()
val javaVersion = property("java_dep").toString().toInt()

version = property("mod_version").toString()
group = property("maven_group").toString()

base {
    // Jar name: <archives_base_name>-<minecraft_version>-<mod_version>
    archivesName.set("${property("archives_base_name")}-$minecraftVersion")
}

repositories {
    maven("https://maven.terraformersmc.com/releases")
    // DevAuth: real Microsoft login in the dev client so runClient can join online servers.
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    // Obfuscated pipeline: remap against official Mojang mappings, mod deps in the
    // intermediary namespace via the mod* configurations.
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    modCompileOnly("com.terraformersmc:modmenu:${property("modmenu_version")}")
    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
}

loom {
    runs {
        named("client") {
            property("devauth.enabled", "true")
        }
    }
}

val resourceProps = mapOf(
    "version" to version,
    "mc_dep" to property("mc_dep").toString(),
    "java_dep" to property("java_dep").toString(),
)

tasks.processResources {
    inputs.properties(resourceProps)
    filesMatching(listOf("fabric.mod.json", "zombiezcompanion.mixins.json")) { expand(resourceProps) }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaVersion)
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${base.archivesName.get()}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = property("archives_base_name").toString()
            from(components["java"])
        }
    }
}

// Modrinth publishing (per MC version = one Modrinth version). Lazy config: no token / no
// `modrinth_id` set => normal builds are unaffected; only running `:<ver>-fabric:modrinth`
// needs MODRINTH_TOKEN (env) and the project id (`-Pmodrinth_id=...` or gradle.properties).
// Run:  MODRINTH_TOKEN=xxx ./gradlew :1.21.4-fabric:modrinth -Pmodrinth_id=<slug>
modrinth {
    // Token from ~/.gradle/gradle.properties (`modrinth_token=…`, outside the repo, never committed),
    // falling back to the MODRINTH_TOKEN env var. Never hardcode it in a tracked file.
    token.set(providers.gradleProperty("modrinth_token").orElse(providers.environmentVariable("MODRINTH_TOKEN")))
    projectId.set(providers.gradleProperty("modrinth_id").orElse("REPLACE_ME"))
    versionNumber.set("${project.version}+mc$minecraftVersion")
    versionName.set("${project.version} (MC $minecraftVersion)")
    versionType.set("release")
    uploadFile.set(tasks.named("remapJar"))
    gameVersions.add(minecraftVersion)
    loaders.add("fabric")
    dependencies {
        required.project("fabric-api")
    }
}
