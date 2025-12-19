import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {

    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.get().compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }
    // JVM target
    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    
    // Android target
    androidLibrary {
        namespace = "org.graphviz.kotlin"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }
    
    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    // JavaScript target
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs {
            testTask {
                useMocha {
                    timeout = "30s"
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Common multiplatform dependencies will be added here
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.property)
        }
        
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
        }
        
        jsTest.dependencies {
            // JS-specific test dependencies if needed
        }
        
        iosTest.dependencies {
            // iOS-specific test dependencies if needed
        }
        
        // Platform-specific source sets are configured automatically by the hierarchy template
        androidMain.dependencies {
            // Android-specific dependencies
        }
        
        jsMain.dependencies {
            // JS-specific dependencies
        }
    }
}

// Configure test tasks after the kotlin block
tasks.withType<Test> {
    // Allow tests to pass even if no tests are found (for platforms with only commonTest)
    filter {
        setFailOnNoMatchingTests(false)
    }
}

// Configure Kotlin/Native test tasks specifically  
// Note: Kotest has limited support on Kotlin/Native, so we'll use kotlin.test for Native platforms
tasks.matching { it.name.contains("Test") && (it.name.contains("ios") || it.name.contains("native")) }.configureEach {
    // Set system property to disable strict test discovery for Native platforms
    if (this is JavaExec) {
        systemProperty("kotest.framework.disable.test.nested.jar.scanning", "true")
    }
}