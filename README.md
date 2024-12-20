# LevelDB for Kotlin Multiplatform

Wrapper for using LevelDB in Kotlin Multiplatform projects and created and designed for use in [lamba92/kotlin.document.store](https://github.com/lamba92/kotlin.document.store).

From [google/leveldb](https://github.com/google/leveldb) repository:

> LevelDB is a fast key-value storage library written at Google that 
provides an ordered mapping from string keys to string values.

google/leveldb is licensed under [BSD-3-Clause license](https://github.com/google/leveldb/blob/main/LICENSE), all rights reserved to the original authors. 

# Index

1. [Introduction](#leveldb-for-kotlin-multiplatform)
   - [Overview](#leveldb-for-kotlin-multiplatform)
   - [Supported Platforms](#supported-platforms)
2. [Usage](#usage)
   - [Dependency Setup](#dependency-setup)
   - [Code Example](#in-your-code)
   - [Snapshots](#snapshots-of-latest-commits)
3. [Benchmarks](#benchmarks)
   - [Ubuntu x64 Benchmarks](#ubuntu-x64)
   - [Windows x64 Benchmarks](#windows-x64)
   - [macOS arm64 Benchmarks](#macos-arm64)


## Supported Platforms

The library is available for:
- Windows (mingwX64)
- Linux (x64, arm64)
- MacOS (x64, arm64)
- Android Native (arm64-v8a, armeabi-v7a, x86, x86_64)
- iOS (arm64, arm64-simulator, x64, x64-simulator)
- watchOS (arm64, arm64-simulator, x64-simulator)
- tvOS (arm64, arm64-simulator, x64-simulator)
- JVM:
  - Windows (x64, arm64)
  - Linux (x64, arm64)
  - macOS (x64, arm64)
  - Android (arm64-v8a, armeabi-v7a, x86, x86_64)

No JS or WASM, sorry!

LevelDB binaries are built in [lamba92/leveldb-builds](http://github.com/lamba92/leveldb-builds) repository.

## Usage

### Dependency Setup
**See [releases](https://github.com/lamba92/kotlin-leveldb/releases) for the latest version!**

In your `build.gradle.kts` file:

```kotlin
// Kotlin Multiplatform
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("com.github.lamba92:kotlin-leveldb:{latest-version}")
            }
        }
    }
}

// Kotlin JVM
dependencies {
    implementation("com.github.lamba92:kotlin-leveldb:{latest-version}")
}
```
### In your code

```kotlin
val db = LevelDB("/path/to/db/as/string")

db.put("key", "value")
val value = db.get("key")
db.delete("key")
db.close()
```

### Snapshots of latest commits
Snapshots of latest commits are available in 
[Sonatype's snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/).
Can be used by adding the repository and the dependency:

```kotlin
repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}
```

Use version `x.y-SNAPSHOT` to get the latest version. Check the current one [here](build.gradle.kts#L38).

## Benchmarks

Benchmarks are available in the [benchmarks](benchmarks/src/commonMain/kotlin/com/github/lamba92/leveldb/benchmarks/Main.kt) module. 
On each commit, the benchmarks are run and the results are
published in the GitHub Actions logs. You can find the latest results 
[here](https://github.com/lamba92/kotlin-leveldb/actions/workflows/benchmarks.yml).
Benchmarks are executed on fre GitHub Actions runners for Windows x64, macOS arm64 
and Linux x64, specs available 
[here](https://docs.github.com/en/actions/using-github-hosted-runners/using-github-hosted-runners/about-github-hosted-runners#standard-github-hosted-runners-for-public-repositories).
Benchmarks uses Kotlin/Native only, so the JVM version is not tested.

As of December 2024:

### Ubuntu x64

| Operation                  | Ops/sec (avg over 10 runs) |
|----------------------------|----------------------------|
| Burst Single Put           | 262.85k                    |
| Burst Single Get           | 735.99k                    |
| Burst Single Override Put  | 257.76k                    |
| Burst Single Delete        | 287.94k                    |
| Batch Put                  | 1116.69k                   |

### Windows x64

| Operation                  | Ops/sec (avg over 10 runs) |
|----------------------------|----------------------------|
| Burst Single Put           | 315.42k                    |
| Burst Single Get           | 522.96k                    |
| Burst Single Override Put  | 302.40k                    |
| Burst Single Delete        | 358.45k                    |
| Batch Put                  | 936.79k                    |
| Batch Override Put         | 918.24k                    |
| Batch Delete               | 1266.11k                   |


### macOS arm64

| Operation                  | Ops/sec (avg over 10 runs) |
|----------------------------|----------------------------|
| Burst Single Put           | 290.51k                    |
| Burst Single Get           | 689.45k                    |
| Burst Single Override Put  | 283.43k                    |
| Burst Single Delete        | 349.12k                    |
| Batch Put                  | 1392.60k                   |
| Batch Override Put         | 1374.63k                   |
| Batch Delete               | 1919.41k                   |

Quite good, I'd say!
