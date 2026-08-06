plugins {
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "com.notpatch.NoOrder"
version = "1.4.2"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    maven { url = uri("https://jitpack.io") } // Vault, NLib
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") } // PaperMC
    maven { url = uri("https://repo.extendedclip.com/releases/") } // PlaceholderAPI
    maven { url = uri("https://maven.devs.beer/") } // ItemsAdder
    maven { url = uri("https://repo.nexomc.com/releases") } // Nexo
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven { url = uri("https://nexus.phoenixdevt.fr/repository/maven-public/") }
    mavenCentral()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    compileOnly("com.github.notpatch:NLib:1.5.2")

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    compileOnly("com.zaxxer:HikariCP:6.3.0")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude("org.bukkit", "bukkit") // The Bukkit dependency conflicts with paper, so we must remove it
    }
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("org.json:json:20250517")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("dev.lone:api-itemsadder:4.0.10")
    compileOnly("io.lumine:MythicLib-dist:1.6.2-SNAPSHOT")
    compileOnly("net.Indyuce:MMOItems-API:6.9.5-SNAPSHOT")
    compileOnly("com.nexomc:nexo:1.15.0") {
        exclude("dev.triumphteam", "triumph-gui")
    }
}

tasks {
    // Helps run faster tests
    runServer {
        downloadPlugins {
            github("MilkBowl", "Vault", "1.7.3", "Vault.jar") // Vault (The economy)
            modrinth("hXiIvTyT", "2.22.0") // EssentialsX (Vault Economy Provider)
            modrinth("lKEzGugV", "2.12.3") // PlaceholderAPI
            modrinth("Vebnzrzj", "v5.5.53-bukkit") // Luckperms
            // I couldn't find or the rest of the dependencies are paid
        }

        minecraftVersion("26.2")
    }
    runPaper.folia.registerTask()

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"

        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    withType<JavaCompile>() {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:deprecation")
    }
    withType<Javadoc>() {
        options.encoding = "UTF-8"
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}