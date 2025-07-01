## Architectury 1.21.1 NeoForge Port: `IllegalAccessError` / `NoClassDefFoundError` with KotlinForForge Dependency**

Hey peeps,

Got a persistent dependency issue.

**Setup:**
*   **Minecraft:** 1.21.1
*   **Toolchain:** Architectury
*   **Loaders:** Fabric / NeoForge
*   **Key Dependencies:** Fzzy Config (which requires KotlinForForge), GeckoLib, Jade.

### **The Problem:** The Fabric client builds and runs perfectly. The NeoForge client consistently fails to launch, cycling between `IllegalAccessError` and `NoClassDefFoundError` related to the Kotlin standard library.

I have tried numerous configurations in my `neoforge/build.gradle` file. Between each of these attempts, I have refreshed my Gradle dependencies to ensure a clean state. Below is a complete history of my attempts and the resulting errors.

---

### **Attempt #1: Initial Configuration**

This was the starting `neoforge/build.gradle` based on the Architectury template and adding the necessary dependencies.

<details>
<summary>neoforge/build.gradle</summary>

```gradle
plugins {
   id "com.github.johnrengelman.shadow" version "7.1.2"
}
evaluationDependsOn ':common'

architectury {
   platformSetupLoomIde()
   neoForge()
}

loom {
   accessWidenerPath = project(":common").loom.accessWidenerPath
}

configurations {
   common
   shadowCommon
   compileClasspath.extendsFrom common
   runtimeClasspath.extendsFrom common
   developmentNeoForge.extendsFrom common
}

repositories {
   maven {
       url = "https://maven.neoforged.net/releases/"
       content {
           includeGroupAndSubgroups "net.neoforged"
           includeGroupAndSubgroups "cpw.mods"
       }
   }
   maven {
       url = "https://thedarkcolour.github.io/KotlinForForge/"
   }
}

dependencies {
   neoForge "net.neoforged:neoforge:${rootProject.neoforge_version}"
   modImplementation "dev.architectury:architectury-neoforge:$rootProject.architectury_api_version"

   // GeckoLib
   modImplementation "software.bernie.geckolib:geckolib-neoforge-$rootProject.minecraft_version:$rootProject.geckolib_version"

   // Jade
   modImplementation "curse.maven:jade-324717:${rootProject.jade_version_neoforge}"

   // Fzzy Config and its runtime dependencies
   modRuntimeOnly "me.fzzyhmstrs:fzzy_config:${rootProject.fzzy_config_version}+neoforge"
   modRuntimeOnly "maven.modrinth:kotlin-for-forge:${rootProject.kff_version}" // kff_version was 5.5.0

   common(project(path: ":common", configuration: "namedElements")) { transitive false }
   shadowCommon(project(path: ":common", configuration: "transformProductionNeoForge")) { transitive = false }
}

processResources {
   inputs.property "version", project.version

   filesMatching("META-INF/neoforge.mods.toml") {
       expand "version": project.version
   }
}

shadowJar {
   exclude "fabric.mod.json"
   exclude "architectury.common.json"

   configurations = [project.configurations.shadowCommon]
   archiveClassifier = "dev-shadow"
}

remapJar {
   input.set shadowJar.archiveFile
   dependsOn shadowJar
}

sourcesJar {
   def commonSources = project(":common").sourcesJar
   dependsOn commonSources
   from commonSources.archiveFile.map { zipTree(it) }
}

components.java {
   withVariantsFromConfiguration(project.configurations.shadowRuntimeElements) {
       skip()
   }
}
```
</details>

**Resulting Error:**
```
java.lang.module.ResolutionException: Module kfflang.neoforge reads more than one module named fml_loader
```
A dependency tree analysis showed that Fzzy Config was pulling in KotlinForForge `5.4.0` while I was explicitly requesting `5.5.0`, causing a conflict.

---

### **Attempt #2: Standardizing on KFF 5.8.0**

To resolve the duplicate module issue, I updated to `kff_version=5.8.0` in `gradle.properties` and changed the dependency to `modImplementation`.

<details>
<summary>neoforge/build.gradle dependencies block</summary>

```gradle
dependencies {
    neoForge "net.neoforged:neoforge:${rootProject.neoforge_version}"
    modImplementation "dev.architectury:architectury-neoforge:$rootProject.architectury_api_version"

    modImplementation "software.bernie.geckolib:geckolib-neoforge-$rootProject.minecraft_version:$rootProject.geckolib_version"
    modImplementation "curse.maven:jade-324717:${rootProject.jade_version_neoforge}"
    modImplementation "me.fzzyhmstrs:fzzy_config:${rootProject.fzzy_config_version}+neoforge"
    modImplementation "thedarkcolour:kotlinforforge-neoforge:${rootProject.kff_version}" // kff_version is now 5.8.0

    common(project(path: ':common', configuration: 'namedElements')) { transitive false }
    shadowBundle project(path: ':common', configuration: 'transformProductionNeoForge')
}
```
</details>

**Resulting Error:**
```
java.lang.NoClassDefFoundError: kotlin/jvm/internal/Intrinsics
```

---

### **Attempt #3: Manually Adding Kotlin Stdlib**

To fix the `NoClassDefFoundError`, I tried manually adding the Kotlin standard library JARs to the development classpath.

<details>
<summary>neoforge/build.gradle dependencies block</summary>

```gradle
dependencies {
    neoForge "net.neoforged:neoforge:${rootProject.neoforge_version}"
    modImplementation "dev.architectury:architectury-neoforge:$rootProject.architectury_api_version"

    modImplementation "software.bernie.geckolib:geckolib-neoforge-$rootProject.minecraft_version:$rootProject.geckolib_version"
    modImplementation "curse.maven:jade-324717:${rootProject.jade_version_neoforge}"
    modImplementation "me.fzzyhmstrs:fzzy_config:${rootProject.fzzy_config_version}+neoforge"
    modImplementation "thedarkcolour:kotlinforforge-neoforge:${rootProject.kff_version}"

    // Manually added stdlib
    developmentNeoForge "org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0"
    developmentNeoForge "org.jetbrains.kotlin:kotlin-reflect:2.0.0"

    common(project(path: ':common', configuration: 'namedElements')) { transitive false }
    shadowBundle project(path: ':common', configuration: 'transformProductionNeoForge')
}
```
</details>

**Resulting Error:**
```
java.lang.IllegalAccessError: class thedarkcolour.kotlinforforge.neoforge.KotlinLanguageLoader (in module kfflang.neoforge) cannot access class kotlin.jvm.internal.Intrinsics (in module generated_47e16b7) because module kfflang.neoforge does not read module generated_47e16b7
```

---

### **Attempt #4: Adding JVM Arguments for Module Access**

To fix the `IllegalAccessError`, I added `--add-reads` arguments to the `loom` configuration to grant the Kotlin language provider access to the generated automatic module for the stdlib.

<details>
<summary>neoforge/build.gradle loom block</summary>

```gradle
loom {
    accessWidenerPath = project(":common").loom.accessWidenerPath

    runs {
        client {
            property "dev.architectury.loom.args.jvm",
                    "--add-reads kfflang.neoforge=ALL-UNNAMED,generated_b965cb4 " +
                    "--add-reads kffmod.neoforge=ALL-UNNAMED,generated_b965cb4"
        }
    }
}
```
</details>

**Resulting Error:** The same `IllegalAccessError`, but the generated module name changed, proving it was unstable.
```
java.lang.IllegalAccessError: class thedarkcolour.kotlinforforge.neoforge.KotlinLanguageLoader (in module kfflang.neoforge) cannot access class kotlin.jvm.internal.Intrinsics (in module generated_531f20f) because module kfflang.neoforge does not read module generated_531f20f
```

---

### **Attempt #5: Final (Current) State**

Based on the unstable module name and a `duplicate input class` warning from Architectury Transformer, the last attempt was to simplify the build script again, removing the manual stdlib additions and the JVM arguments, trusting KFF 5.8.0 to handle its dependencies.

<details>
<summary>neoforge/build.gradle (Current Failing State)</summary>

```gradle
plugins {
    id "com.github.johnrengelman.shadow" version "7.1.2"
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    accessWidenerPath = project(":common").loom.accessWidenerPath
}

configurations {
    common
    shadowCommon
    compileClasspath.extendsFrom common
    runtimeClasspath.extendsFrom common
    developmentNeoForge.extendsFrom common
    shadowBundle {
        canBeResolved = true
        canBeConsumed = false
    }
}

repositories {
    maven {
        name = 'NeoForged'
        url = 'https://maven.neoforged.net/releases'
    }
    maven {
        url = "https://thedarkcolour.github.io/KotlinForForge/"
    }
}

dependencies {
    neoForge "net.neoforged:neoforge:${rootProject.neoforge_version}"
    modImplementation "dev.architectury:architectury-neoforge:$rootProject.architectury_api_version"

    modImplementation "software.bernie.geckolib:geckolib-neoforge-$rootProject.minecraft_version:$rootProject.geckolib_version"
    modImplementation "curse.maven:jade-324717:${rootProject.jade_version_neoforge}"
    modImplementation "me.fzzyhmstrs:fzzy_config:${rootProject.fzzy_config_version}+neoforge"
    modImplementation "thedarkcolour:kotlinforforge-neoforge:${rootProject.kff_version}"

    common(project(path: ':common', configuration: 'namedElements')) { transitive false }
    shadowBundle project(path: ':common', configuration: 'transformProductionNeoForge')
}

processResources {
    inputs.property 'version', project.version

    filesMatching('META-INF/neoforge.mods.toml') {
        expand version: project.version
    }
}

shadowJar {
    configurations = [project.configurations.shadowBundle]
    archiveClassifier = 'dev-shadow'
    exclude "fabric.mod.json"
}

remapJar {
    inputFile.set shadowJar.archiveFile
    dependsOn shadowJar
}

sourcesJar {
    def commonSources = project(":common").sourcesJar
    dependsOn commonSources
    from commonSources.archiveFile.map { zipTree(it) }
}
```
</details>

**Resulting Error:** This brought us back to the `NoClassDefFoundError`.
```
java.lang.NoClassDefFoundError: kotlin/jvm/internal/Intrinsics
```

## I seem to be stuck in a loop between `IllegalAccessError` and `NoClassDefFoundError`. I've run out of ideas so any insight into the correct way to configure this would be immensely appreciated!


---

### **Other Relevant Gradle Files**

<details>
<summary>Root build.gradle</summary>

```gradle
plugins {
  id "architectury-plugin" version "3.4.161"
  id "dev.architectury.loom" version "1.9-SNAPSHOT" apply false
}

architectury {
  minecraft = rootProject.minecraft_version
}

subprojects {
  apply plugin: "dev.architectury.loom"

  dependencies {
  minecraft "com.mojang:minecraft:${rootProject.minecraft_version}"
  mappings loom.layered {
  it.mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
  it.mappings "dev.architectury:yarn-mappings-patch-neoforge:${project.neoforge_yarn_patch}"
  }
 }}

allprojects {
  apply plugin: "java"
  apply plugin: "architectury-plugin"

  base {
  archivesName = rootProject.archives_base_name
  }

  version = "${rootProject.mod_version}+${project.name}"
  group = rootProject.maven_group

  repositories {
  maven { name = "TerraformersMC"; url = "https://maven.terraformersmc.com/" }
  maven { name = "FzzyMaven"; url = "https://maven.fzzyhmstrs.me/" }
  maven { name = "CurseMaven"; url = "https://cursemaven.com" }
  maven { name = "Modrinth"; url = "https://api.modrinth.com/maven" }
  maven { name = "GeckoLib"; url = 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/' }
 }
  tasks.withType(JavaCompile).configureEach {
  it.options.encoding = "UTF-8"
  it.options.release = 21
  it.options.compilerArgs.add("-Xlint:unchecked")
    }

  java {
  withSourcesJar()
    }
}
```
</details>

<details>
<summary>gradle.properties</summary>

```properties
# Gradle Properties
org.gradle.jvmargs=-Xmx2G

# Architectury Properties
minecraft_version=1.21.1
yarn_mappings=1.21.1+build.3
neoforge_yarn_patch=1.21+build.4
enabled_platforms=fabric,neoforge

# Loader Versions
fabric_loader_version=0.16.14
neoforge_version=21.1.172

# Mod Properties
mod_version=2.0.0
maven_group=net.dawson.adorablehamsterpets
archives_base_name=adorablehamsterpets

# API Dependencies
fabric_api_version=0.116.2+1.21.1
architectury_api_version=13.0.8

# For Data Gen
fabric.datagen.modid=adorablehamsterpets

# Mod Dependencies
geckolib_version=4.7.3
modmenu_version=11.0.1
fzzy_config_version=0.7.0+1.21
jade_version_fabric=6291536
jade_version_neoforge=6155158

# Runtime Dependencies (for Fzzy Config)
flk_version=1.12.1+kotlin.2.0.20
kff_version=5.8.0
```
</details>

<details>
<summary>settings.gradle</summary>

```gradle
pluginManagement {
  repositories {
  maven { url = "https://maven.architectury.dev/" }
  maven { url = "https://maven.fabricmc.net/" }
  maven { url = "https://maven.minecraftforge.net/" }
  gradlePluginPortal()
        mavenCentral()
    }
}

include("common")
include("fabric")
include("neoforge")

rootProject.name = "adorablehamsterpets"
```
</details>

<details>
<summary>common/build.gradle</summary>

```gradle
architectury {
  common(rootProject.enabled_platforms.split(","))
}

loom {
  accessWidenerPath = file("src/main/resources/adorablehamsterpets.accesswidener")
}

dependencies {
  // Depend on Fabric Loader here to use the Fabric @Environment annotations
  modImplementation "net.fabricmc:fabric-loader:${rootProject.fabric_loader_version}"

  // Architectury API
  modCompileOnly "dev.architectury:architectury:$rootProject.architectury_api_version"

  // ── ExpectPlatform / injectables  (NEEDED FOR @ExpectPlatform) ─────
  def injectablesVer = "1.0.13"
  compileOnly         "dev.architectury:architectury-injectables:$injectablesVer"
  annotationProcessor "dev.architectury:architectury-injectables:$injectablesVer"

  // GeckoLib API
  modCompileOnly "software.bernie.geckolib:geckolib-fabric-$rootProject.minecraft_version:$rootProject.geckolib_version"

  // Fzzy Config API
  modCompileOnly "me.fzzyhmstrs:fzzy_config:${rootProject.fzzy_config_version}"

  // Jade API
  modCompileOnly "curse.maven:jade-324717:${rootProject.jade_version_fabric}"
}
```
</details>

<details>
<summary>fabric/build.gradle</summary>

```gradle
plugins {
  id "com.github.johnrengelman.shadow" version "7.1.2"
}
evaluationDependsOn ':common'

architectury {
  platformSetupLoomIde()
    fabric()
}

fabricApi {
  configureDataGeneration {
  client = true
  }
}

loom {
  accessWidenerPath = project(":common").loom.accessWidenerPath

    mods {
  main {
  sourceSet sourceSets.main
  sourceSet project(':common').sourceSets.main
        }
 }}

configurations {
  common
    shadowCommon
    compileClasspath.extendsFrom common
    runtimeClasspath.extendsFrom common
    developmentFabric.extendsFrom common
}

dependencies {
  modImplementation "net.fabricmc:fabric-loader:${rootProject.fabric_loader_version}"
  modImplementation "net.fabricmc.fabric-api:fabric-api:${rootProject.fabric_api_version}"
  modImplementation "dev.architectury:architectury-fabric:$rootProject.architectury_api_version"

  // GeckoLib
  modImplementation "software.bernie.geckolib:geckolib-fabric-$rootProject.minecraft_version:$rootProject.geckolib_version"

  // Mod Menu
  modImplementation "com.terraformersmc:modmenu:${rootProject.modmenu_version}"

  // Jade
  modImplementation "curse.maven:jade-324717:${rootProject.jade_version_fabric}"

  // Fzzy Config and its runtime dependencies
  modImplementation "me.fzzyhmstrs:fzzy_config:${rootProject.fzzy_config_version}"
  modRuntimeOnly "maven.modrinth:fabric-language-kotlin:${rootProject.flk_version}"

  // Placeholder API (for bundling)
  modImplementation include("maven.modrinth:placeholder-api:2.4.2+1.21")

    common(project(path: ":common", configuration: "namedElements")) { transitive false }
  shadowCommon(project(path: ":common", configuration: "transformProductionFabric")) { transitive false }
}

processResources {
  inputs.property "version", project.version

  filesMatching("fabric.mod.json") {
  expand "version": project.version
  }
}

shadowJar {
  exclude "architectury.common.json" // keep
  configurations = [project.configurations.shadowCommon]  // transformed common code
  from(sourceSets.main.output)             // ADD → include Fabric classes
  archiveClassifier = "dev-shadow"
  dependsOn project(':common').tasks.named('transformProductionFabric')
}

remapJar {
  injectAccessWidener = true
  input.set shadowJar.archiveFile
  dependsOn shadowJar
}

sourcesJar {
  def commonSources = project(":common").sourcesJar
  dependsOn commonSources
    from commonSources.archiveFile.map { zipTree(it) }
}

components.java {
  withVariantsFromConfiguration(project.configurations.shadowRuntimeElements) {
  skip()
    }
}
```
</details>