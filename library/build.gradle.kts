import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.maven.publish)
}

group = "io.github.pauloaapereira"
version = "1.1.0"

kotlin {
    androidTarget {
        publishLibraryVariants("release")

        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }

    jvm()

    js(IR) {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    macosX64()
    macosArm64()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "animatedsequence"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.stately.collections)
            implementation(libs.atomicfu)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.pp.library"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    // Ensures JDK 17 is used to compile the Kotlin code.
    kotlin { jvmToolchain(17) }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates(group.toString(), "animatedsequence", version.toString())

    pom {
        name.set("AnimatedSequence")
        description.set("Animate your Jetpack Compose UI effortlessly with smooth, sequential animations using AnimationSequence")
        url.set("https://github.com/pauloaapereira/animatedsequence")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("pauloaapereira")
                name.set("Paulo Pereira")
                email.set("paulo_aa_pereira@outlook.pt")
            }
        }
        scm {
            url = "https://github.com/pauloaapereira/animatedsequence"
            connection = "scm:git:git://github.com/pauloaapereira/animatedsequence.git"
            developerConnection = "scm:git:ssh://git@github.com/pauloaapereira/animatedsequence.git"
        }
    }
}
