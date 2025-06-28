I'm in the final stages of migrating my 1.21.1 Fabric mod to an Architectury project and have hit a wall with data generation. When I run the `runDatagen` task, the game crashes during initialization with an `AssertionError`.



The crash seems to be caused by my `@ExpectPlatform` method for registering entity spawn restrictions.



**Crash Log:**

```

Caused by: java.lang.AssertionError

        at knot//net.dawson.adorablehamsterpets.world.ModSpawnPlacements.register(ModSpawnPlacements.java:18)

        at knot//net.dawson.adorablehamsterpets.AdorableHamsterPets.init(AdorableHamsterPets.java:64)

        at knot//net.dawson.adorablehamsterpets.fabric.AdorableHamsterPetsFabric.onInitialize(AdorableHamsterPetsFabric.java:14)

```



Here's the [full Crash Log pastebin if you need it.](https://pastebin.com/uUTUW3eQ)



I've followed the standard `@ExpectPlatform` pattern, with an implementation for Fabric and one for NeoForge. The crash happens when the common code calls the `@ExpectPlatform` method.



I've double-checked my `build.gradle` files and the file paths for the implementation classes, but I can't spot the issue.



Here is the [link to the full project repo](https://github.com/DawsonBodenhamer/Adorable-Hamster-Pets-2.0/tree/master)



The relevant files are:

*   [`common/.../world/ModSpawnPlacements.java`](https://github.com/DawsonBodenhamer/Adorable-Hamster-Pets-2.0/blob/master/common/src/main/java/net/dawson/adorablehamsterpets/world/ModSpawnPlacements.java) (the `@ExpectPlatform` declaration)

*   [`fabric/.../world/fabric/ModSpawnPlacementsImpl.java`](https://github.com/DawsonBodenhamer/Adorable-Hamster-Pets-2.0/blob/master/fabric/src/main/java/net/dawson/adorablehamsterpets/fabric/world/ModSpawnPlacementsImpl.java) (the Fabric implementation)

*   [`common/.../AdorableHamsterPets.java`](https://github.com/DawsonBodenhamer/Adorable-Hamster-Pets-2.0/blob/master/common/src/main/java/net/dawson/adorablehamsterpets/AdorableHamsterPets.java) (where the method is called)



Then I "solved" that issue by wrapping the runtime-only initializers (like entity spawn placements) in my common `init()` method with a check (`System.getProperty("fabric-api.datagen") == null`) to prevent them from running during data generation. 



 ## That allowed datagen to complete successfully, but now I'm facing the same `AssertionError` when trying to launch the actual Fabric client.



**The Main Problem:**

The Fabric client fails to launch, crashing with an `AssertionError`. This happens when my common `init()` method calls `ModSpawnPlacements.register()`, which is a method annotated with `@ExpectPlatform`. This indicates that the common placeholder method is being executed instead of the Fabric-specific implementation.



I have already tried cleaning the Gradle cache multiple times and have added the `architectury` common entrypoint to my `fabric.mod.json`, but the issue persists.



**The Secondary Problem:**

Separately, when I run `./gradlew build`, the resulting Fabric JAR file is nearly empty (around 25 bytes) and only contains a `META-INF` folder. This suggests a problem with how the `shadowJar` task is configured in my `fabric/build.gradle`.



I'm sure it's a configuration issue I'm just not seeing. Would hugely appreciate if you took a look!



**Full Repo:**

*   [https://github.com/DawsonBodenhamer/Adorable-Hamster-Pets-2.0/tree/master](https://github.com/DawsonBodenhamer/Adorable-Hamster-Pets-2.0/tree/master)



**Direct Links to Key Files:**

*   **Build Scripts:**

*   [Root `build.gradle`](https://github.com/DawsonBodenhamer/Adorable-Hamster-Pets-2.0/blob/master/build.gradle)

*   [`fabric/build.gradle`](https://github.com/DawsonBodenhamer/Adorable-Hamster-Pets-2.0/blob/master/fabric/build.gradle)

*   **Metadata:**

*   [`fabric.mod.json`](https://github.com/DawsonBodenhamer/Adorable-Hamster-Pets-2.0/blob/master/fabric/src/main/resources/fabric.mod.json)

*   **Failing Code:**

*   [`ModSpawnPlacements.java` (Common - `@ExpectPlatform` interface)](https://github.com/DawsonBodenhamer/Adorable-Hamster-Pets-2.0/blob/master/common/src/main/java/net/dawson/adorablehamsterpets/world/ModSpawnPlacements.java)

*   [`ModSpawnPlacementsImpl.java` (Fabric - Implementation)](https://github.com/DawsonBodenhamer/Adorable-Hamster-Pets-2.0/blob/master/fabric/src/main/java/net/dawson/adorablehamsterpets/fabric/world/ModSpawnPlacementsImpl.java)

*   [`AdorableHamsterPets.java` (Common - Where the call happens)](https://github.com/DawsonBodenhamer/Adorable-Hamster-Pets-2.0/blob/master/common/src/main/java/net/dawson/adorablehamsterpets/AdorableHamsterPets.java)



A few followup clarifications:



**1. What version of the Architectury Loom plugin am I using?**



Based on my root `build.gradle` file, I am using version **`1.9-SNAPSHOT`** of the `dev.architectury.loom` plugin.



The exact line from my `build.gradle` is:

```groovy

id "dev.architectury.loom" version "1.9-SNAPSHOT" apply false

```





**2. Have I confirmed that the `ModSpawnPlacementsImpl` class is actually being compiled and included in the final Fabric jar during client runs (not just datagen)?**



No, I can't confirm that. In fact, I've confirmed the opposite.



When I run the `./gradlew build` task, the resulting Fabric JAR file in `fabric/build/libs/` is only 25 bytes. When I open this JAR, it's almost empty and **does not contain any of my mod's classes**, including `ModSpawnPlacementsImpl.java`.



This may possibly suggest that my `fabric/build.gradle` script is misconfigured and is failing to package the compiled code from the `common` and `fabric` source sets into the final JAR. It could be the root cause of both the empty JAR and the `AssertionError` at runtime, but it would take more research to confirm that.



**3. Can I confirm whether the Fabric implementation file is in the right source set (`fabric/src/main/java`) and package?**



Yes, I can confirm the file is in the correct location.



*   **File Path:** The implementation file is located at:

`fabric/src/main/java/net/dawson/adorablehamsterpets/fabric/world/ModSpawnPlacementsImpl.java`



*   **Package Declaration:** The package declaration inside the file is:

`package net.dawson.adorablehamsterpets.fabric.world;`



The file appears to be in the correct source set and package, so I don't think its location is the cause of the error.







---



For complete context, here are all of my Gradle configuration files and mod metadata files.







**// Root `build.gradle`**

```groovy

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

       // Layered mappings for better cross-loader compatibility

       mappings loom.layered {

           it.mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"

           it.mappings "dev.architectury:yarn-mappings-patch-neoforge:${project.neoforge_yarn_patch}"

       }

   }

}





allprojects {

   apply plugin: "java"

   apply plugin: "architectury-plugin"





   architectury {

       compileOnly()

   }





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

       maven { name = "GeckoLib"; url 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/' }

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



**// Root `gradle.properties`**

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





# Mod Dependencies

geckolib_version=4.7.5

modmenu_version=11.0.1

fzzy_config_version=0.7.0+1.21

jade_version_fabric=6291536

jade_version_neoforge=6155158





# Runtime Dependencies (for Fzzy Config)

flk_version=1.12.1+kotlin.2.0.20

kff_version=5.5.0

```



**// Root `settings.gradle`**

```groovy

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



**// `fabric/build.gradle`**

```groovy

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

   }

}





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

   modImplementation "me.fzzyhmstrs:fzzy_config:${root.fzzy_config_version}"

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

   exclude "architectury.common.json"

   configurations = [project.configurations.shadowCommon]

   archiveClassifier = "dev-shadow"

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



**// `fabric/src/main/resources/fabric.mod.json`**

```json

{

 "schemaVersion": 1,

 "id": "adorablehamsterpets",

 "version": "${version}",

 "name": "Adorable Hamster Pets",

 "description": "Sure, you’ve got dragons and machines, but let’s be honest— no respectable mod pack is complete without tiny fluffy hamsters running around.",

 "authors": [

   "The Scarlet Fox"

 ],

 "contact": {

   "homepage": "https://www.fortheking.design/",

   "sources": "https://github.com/DawsonBodenhamer/Adorable-Hamster-Pets-1.21/"

 },

 "license": "Custom - See LICENSE.md",

 "icon": "assets/adorablehamsterpets/icon.png",

 "environment": "*",

 "entrypoints": {

   "main": [

     "net.dawson.adorablehamsterpets.fabric.AdorableHamsterPetsFabric"

   ],

   "client": [

     "net.dawson.adorablehamsterpets.fabric.client.AdorableHamsterPetsFabricClient"

   ],

   "fabric-datagen": [

     "net.dawson.adorablehamsterpets.fabric.datagen.AdorableHamsterPetsDataGenerator"

   ],

   "jade": [

     "net.dawson.adorablehamsterpets.integration.jade.AHPJadePlugin"

   ]

 },

 "mixins": [

   "adorablehamsterpets.mixins.json"

 ],

 "accessWidener": "adorablehamsterpets.accesswidener",

 "depends": {

   "fabricloader": ">=0.16.14",

   "minecraft": "~1.21.1",

   "java": ">=21",

   "fabric-api": "*",

   "architectury": ">=13.0.8",

   "geckolib": ">=4.7.3",

   "fzzy_config": ">=0.7.0"

 },

 "suggests": {

   "modmenu": "*",

   "jade": "*"

 },

  "custom": {

    "fzzy_config": [

      "adorablehamsterpets:main"

    ],

    "architectury": {

      "common": "net.dawson.adorablehamsterpets.AdorableHamsterPets"

    }

  }

}

```

I reached out to the modding community, and someone on discord said "In your root build.gradle in allprojects, you need to remove:
```
architectury {
  compileOnly()
}
```
I did that and i stopped getting errors. Apparently, that disables features like @ExpectPlatform. https://docs.architectury.dev/plugin/compile_only"

then they said:
"If you get errors saying it can't find the file, you probably need to change the project structure of the fabric folder.
mine looks like this
``` 
fabric
├── .gradle/
├── build/
├── run/
├── src/
│   └── main/
│       ├── generated/
│       ├── java/
│       │   └── net/
│       │       └── dawson/
│       │           └── adorablehamsterpets/
│       │               ├── client/
│       │               ├── datagen/
│       │               ├── world/
│       │               │   └── fabric/
│       │               │       └── ModSpawnPlacementsImpl.java
│       │               └── AdorableHamsterPetsFabric.java
│       └── resources/
│           └── fabric.mod.json
└── build.gradle


```

so I tried that, and I removed the
```
architectury {
  compileOnly()
}
```
block from root/build.gradle, but I'm still getting an empty jar when I build, and the fabric client is still crashing.

When I try to run the fabric client like this:
`./gradlew :fabric:runClient --debug --stacktrace --scan --warning-mode all`

The result is this:

*   **The main crash reason:**
    ```
    java.lang.RuntimeException: Could not execute entrypoint stage 'main' due to errors, provided by 'adorablehamsterpets' at 'net.dawson.adorablehamsterpets.fabric.AdorableHamsterPetsFabric'!
    ```
*   **The root cause:**
    ```
    Caused by: net.fabricmc.loader.api.LanguageAdapterException: java.lang.ClassNotFoundException: net.dawson.adorablehamsterpets.fabric.AdorableHamsterPetsFabric
    ```

Additionally, there's another error that occurs just before the game crash:

```
java.lang.StringIndexOutOfBoundsException: Range [0, -1) out of bounds for length 62
    at dev.architectury.transformer.TransformerRuntime.lambda$null$8(TransformerRuntime.java:250)
```

Interestingly, when I try to run the fabric client by just clicking the button at the top of IntelliJ instead of through the terminal, the crash looks like this instead:

```
> Task :fabric:dev.architectury.transformer.TransformerRuntime.main() FAILED
Error occurred during initialization of VM
Error opening zip file or JAR manifest missing : C:\Users\tweek\OneDrive\Documents\MY MINECRAFT MODS\Data\Repositories\Adorable Hamster Pets\.gradle\architectury\architectury-transformer-agent.jar
agent library failed Agent_OnLoad: instrument

[Incubating] Problems report is available at: file:///C:/mods/Data/Repositories/Adorable%20Hamster%20Pets/build/reports/problems/problems-report.html

Execution failed for task ':fabric:dev.architectury.transformer.TransformerRuntime.main()'.
> Process 'command 'C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\java.exe'' finished with non-zero exit value 1
```

Then I tried changing my fabric/build.gradle shadowJar block to this:
```
shadowJar {
    exclude "architectury.common.json"
    from sourceSets.main.output
    from project(":common").sourceSets.main.output
    configurations = [project.configurations.shadowCommon]
    archiveClassifier = "dev-shadow"
}
``` 
and I ran I resynced gradle successfully, ran ./gradlew clean and then ./gradlew build, but after ./gradlew build, the .jar file in build/libs was still only 25 bytes, and the fabric client still failed to launch with these errors:
```
[ERROR] [system.err] java.lang.StringIndexOutOfBoundsException: Range [0, -1) out of bounds for length 62
[ERROR] [system.err]       at dev.architectury.transformer.TransformerRuntime.lambda$null$8(TransformerRuntime.java:250)
```


```
---- Minecraft Crash Report ----
// My bad.

Description: Initializing game

java.lang.RuntimeException: Could not execute entrypoint stage 'main' due to errors, provided by 'adorablehamsterpets' at 'net.dawson.adorablehamsterpets.fabric.AdorableHamsterPetsFabric'!
...
Caused by: net.fabricmc.loader.api.EntrypointException: Exception while loading entries for entrypoint 'main' provided by 'adorablehamsterpets'
...
Caused by: net.fabricmc.loader.api.LanguageAdapterException: java.lang.ClassNotFoundException: net.dawson.adorablehamsterpets.fabric.AdorableHamsterPetsFabric
...
Caused by: java.lang.ClassNotFoundException: net.dawson.adorablehamsterpets.fabric.AdorableHamsterPetsFabric
```

so then I tried rearranging the shadowJar block like this:
```
shadowJar {
    exclude "architectury.common.json"       // keep
    configurations = [project.configurations.shadowCommon]  // transformed common code
    from(sourceSets.main.output)             // ADD → include Fabric classes
    archiveClassifier = "dev-shadow"
    dependsOn project(':common').tasks.named('transformProductionFabric')
}

``` 
so it explicitly pulls in both the shadowCommon configuration (which contains the transformed common classes) and the Fabric module’s own compiled output, basically a “let’s be sure everything is physically in the jar” sanity check.

after I did that, ./gradlew clean build worked just fine.

Also, I know I should have mentioned this earlier, but I only just now discovered some other jar files that were being generated, and they do have content:

```text
AdorableHamsterPets/
├── .architecture-transformer/
├── .gradle/
├── .idea/
├── build/
│   ├── libs/
│   │   ├── adorablehamsterpets-2.0.0+adorablehamsterpets.jar
│   │   │   └── META-INF/
│   │   └── adorablehamsterpets-2.0.0+adorablehamsterpets-sources.jar
│   ├── reports/
│   └── tmp/
├── common/
│   ├── .gradle/
│   ├── build/
│   │   ├── classes/
│   │   ├── devlibs/
│   │   │   ├── adorablehamsterpets-2.0.0+common-dev.jar
│   │   │   │   ├── assets/
│   │   │   │   ├── data/
│   │   │   │   ├── META-INF/
│   │   │   │   ├── net/
│   │   │   │   ├── adorablehamsterpets.accesswidener
│   │   │   │   ├── adorablehamsterpets.mixins.json
│   │   │   │   └── adorablehamsterpets-common-refmap.json
│   │   │   └── adorablehamsterpets-2.0.0+common-sources.jar
│   │   ├── generated/
│   │   │   └── sources/
│   │   │       ├── annotationProcessor/
│   │   │       └── headers/
│   │   ├── libs/
│   │   │   ├── adorablehamsterpets-2.0.0+common.jar
│   │   │   │   ├── assets/
│   │   │   │   ├── data/
│   │   │   │   ├── META-INF/
│   │   │   │   ├── net/
│   │   │   │   ├── adorablehamsterpets.accesswidener
│   │   │   │   ├── adorablehamsterpets.mixins.json
│   │   │   │   ├── adorablehamsterpets-common-refmap.json
│   │   │   │   └── architecture.common.marker
│   │   │   ├── adorablehamsterpets-2.0.0+common-transformProductionFabric.jar
│   │   │   │   ├── architectury_inject_adorablehamsterpets_common.../
│   │   │   │   ├── assets/
│   │   │   │   ├── data/
│   │   │   │   ├── META-INF/
│   │   │   │   ├── net/
│   │   │   │   ├── adorablehamsterpets.accesswidener
│   │   │   │   ├── adorablehamsterpets.mixins.json
│   │   │   │   └── adorablehamsterpets-common-refmap.json
│   │   │   └── adorablehamsterpets-2.0.0+common-transformProductionNeoForge.jar
│   │   │       ├── architectury_inject_adorablehamsterpets_common.../
│   │   │       ├── assets/
│   │   │       ├── data/
│   │   │       ├── META-INF/
│   │   │       ├── net/
│   │   │       ├── adorablehamsterpets.accesswidener
│   │   │       ├── adorablehamsterpets.mixins.json
│   │   │       └── adorablehamsterpets-common-refmap.json
│   │   └── loom-cache/
│   ├── processIncludeJars/
│   ├── resources/
│   └── tmp/
├── run/
├── src/
└── build.gradle
```

So this whole time, when I've been saying the "jar file is only 25 bytes," it was this file I was looking at:
```
│   ├── libs/
│   │   ├── adorablehamsterpets-2.0.0+adorablehamsterpets.jar
```

I didn't even know about the others— each of them is about 1.3 megabytes.

However, `./gradlew :fabric:runClient --stacktrace --warning-mode all` still ultimately failed.

First, an error occurred inside the build tool responsible for preparing the mod's code.

```
java.lang.StringIndexOutOfBoundsException: Range [0, -1) out of bounds for length 62
        at java.base/jdk.internal.util.Preconditions$1.apply(Preconditions.java:55)
        at java.base/jdk.internal.util.Preconditions$1.apply(Preconditions.java:52)
        ...
        at dev.architectury.transformer.TransformerRuntime.main(TransformerRuntime.java:214)
```

Then the game crashed because it couldn't load the "adorablehamsterpets" mod.

```java
java.lang.RuntimeException: Could not execute entrypoint stage 'main' due to errors, provided by 'adorablehamsterpets' at 'net.dawson.adorablehamsterpets.fabric.AdorableHamsterPetsFabric'!
```

The specific reason for the crash was that the mod's main class file could not be found.

```java
Caused by: net.fabricmc.loader.api.LanguageAdapterException: java.lang.ClassNotFoundException: net.dawson.adorablehamsterpets.fabric.AdorableHamsterPetsFabric
```

Gradle reported that the `:fabric:runClient` task failed because the Java process it launched (the game) exited with an error.

```
> Task :fabric:runClient FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':fabric:runClient'.
> Process 'command 'C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin\java.exe'' finished with non-zero exit value -1
```

So I tried installing two of these other jars (adorablehamsterpets-2.0.0+common.jar and then I tried adorablehamsterpets-2.0.0+common-transformProductionFabric.jar) in my external Modrinth launcher, but it still wouldn't let me do it. It said "Invalid input: Unable to infer project type for input file" just like before. And when I tried manually copying either of the jars to my mods folder and launching the game, it was as if my mod was not installed at all. No mod menu config option and no in-game items or anything.