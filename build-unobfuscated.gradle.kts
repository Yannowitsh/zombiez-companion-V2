// Build target: Minecraft 26.1.2 (unobfuscated). MC 26.1+ ships deobfuscated with
// official names, so use the non-remap Loom variant (no mappings). Java 25.
plugins {
    id("net.fabricmc.fabric-loom") version "1.17.19"
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
    // No mappings: 26.1+ ships with official names, so the non-remap pipeline uses
    // them directly (Yarn removed). Mod deps use the standard configurations.
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    compileOnly("com.terraformersmc:modmenu:${property("modmenu_version")}")
    runtimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
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
    // MC 26.1.2 requires Java 25. Toolchain is auto-provisioned via foojay even when
    // the Gradle daemon runs on an older JVM.
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
// Run:  MODRINTH_TOKEN=xxx ./gradlew :26.1.2-fabric:modrinth -Pmodrinth_id=<slug>
modrinth {
    // Token from ~/.gradle/gradle.properties (`modrinth_token=…`, outside the repo, never committed),
    // falling back to the MODRINTH_TOKEN env var. Never hardcode it in a tracked file.
    token.set(providers.gradleProperty("modrinth_token").orElse(providers.environmentVariable("MODRINTH_TOKEN")))
    projectId.set(providers.gradleProperty("modrinth_id").orElse("REPLACE_ME"))
    versionNumber.set("${project.version}+mc$minecraftVersion")
    versionName.set("${project.version} (MC $minecraftVersion)")
    versionType.set("release")
    // Non-remap Loom (26.1+, official names) has no `remapJar`; `jar` is the final mod jar.
    uploadFile.set(tasks.named("jar"))
    gameVersions.add(minecraftVersion)
    loaders.add("fabric")
    dependencies {
        required.project("fabric-api")
    }
}
