import com.android.build.api.attributes.BuildTypeAttr
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.LibraryElements.JAR
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.Usage.JAVA_API
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.attributes.java.TargetJvmEnvironment.ANDROID
import org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

dependencies {
    components {
        withModule("net.java.dev.jna:jna") {
            val androidBuildTypes = listOf("debug", "release")
            val usages = listOf("api", "runtime")

            for (buildType in androidBuildTypes) {
                for (usageType in usages) {
                    val variantName = "android${buildType.capitalized()}${usageType.capitalized()}"
                    addVariant(variantName) {
                        attributes {
                            attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
                            attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("aar"))
                            val usageAttr = if (usageType == "api") JAVA_API else JAVA_RUNTIME
                            attribute(USAGE_ATTRIBUTE, objects.named(usageAttr))
                            attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
                            attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(ANDROID))
                            attribute(BuildTypeAttr.ATTRIBUTE, objects.named(buildType))
                        }
                        withFiles {
                            addFile("jna-${id.version}.aar")
                        }
                    }
                }
            }

            addVariant("jvmRuntime") {
                attributes {
                    attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
                    attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(JAR))
                    attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
                    attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(STANDARD_JVM))
                }
                withFiles {
                    addFile("jna-${id.version}.jar")
                }
            }
        }
    }
}
