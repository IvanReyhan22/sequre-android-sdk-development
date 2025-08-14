import groovy.util.Node

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

group = "id.sequre"
version = "1.0.5"

android {
    namespace = "id.sequre.scanner_sdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        defaultConfig.targetSdk = 36 // Set targetSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        renderscriptTargetApi = 19
        renderscriptSupportModeEnabled = true
    }

    flavorDimensions += "environment"
    productFlavors {
        create("production") {
            dimension = "environment"
            buildConfigField("String", "FLAVOR_NAME", "\"production\"")

            buildConfigField(
                "String",
                "BASE_API_URL",
                "\"https://mobile.sequre.id/v3/\""
            )
            buildConfigField(
                "String",
                "API_CLASSIFICATION_URL",
                "\"https://sequrepro-prod-362191430784.asia-southeast2.run.app\""
            )
        }

        create("staging") {
            dimension = "environment"
            buildConfigField("String", "FLAVOR_NAME", "\"staging\"")

            buildConfigField(
                "String",
                "BASE_API_URL",
                "\"https://smobile.sequre.id/v3/\""
            )
            buildConfigField(
                "String",
                "API_CLASSIFICATION_URL",
                "\"https://qtrustaiteam-update-staging-362191430784.asia-southeast2.run.app\""
            )
        }

        create("dev") {
            dimension = "environment"
            buildConfigField("String", "FLAVOR_NAME", "\"dev\"")
            buildConfigField(
                "String",
                "BASE_API_URL",
                "\"https://smobile.sequre.id/v3/\""
            )
            buildConfigField(
                "String",
                "API_CLASSIFICATION_URL",
                "\"https://qtrustaiteam-update-staging-362191430784.asia-southeast2.run.app\""
            )
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
            assets.srcDirs("assets")
        }
    }

    // Configure publishing for each flavor
    publishing {
        singleVariant("productionRelease") {
            withSourcesJar()
        }
        singleVariant("stagingRelease") {
            withSourcesJar()
        }
        singleVariant("devRelease") {
            withSourcesJar()
        }
    }
}

// Create publications after project evaluation
afterEvaluate {
    publishing {
        publications {
            create("mavenProduction", MavenPublication::class) {
                groupId = project.group.toString()
                artifactId = "scanner-sdk"
                version = project.version.toString()

                // Use the correct AAR for production flavor
//                artifact(tasks.named("bundleProductionReleaseAar").get())

                from(components["productionRelease"])

//                pom {
//                    withXml {
//                        asNode().appendNode("dependencies").apply {
//                            addAllDependencies()
//                        }
//                    }
//                }
            }

            create("mavenStaging", MavenPublication::class) {
                groupId = project.group.toString()
                artifactId = "scanner-sdk-staging"
                version = project.version.toString()

                // Use the correct AAR for staging flavor
//                artifact(tasks.named("bundleStagingReleaseAar").get())

                from(components["stagingRelease"])

//                pom {
//                    withXml {
//                        asNode().appendNode("dependencies").apply {
//                            addAllDependencies()
//                        }
//                    }
//                }
            }

            create("mavenDev", MavenPublication::class) {
                groupId = project.group.toString()
                artifactId = "scanner-sdk-dev"
                version = project.version.toString()

                // Use the correct AAR for dev flavor
//                artifact(tasks.named("bundleDevReleaseAar").get())

                from(components["devRelease"])

//                pom {
//                    withXml {
//                        asNode().appendNode("dependencies").apply {
//                            addAllDependencies()
//                        }
//                    }
//                }
            }
        }
    }
}

// Helper function to append dependencies in Kotlin DSL
fun Node.appendDependency(
    groupId: String,
    artifactId: String,
    version: String,
    scope: String = "runtime"
) {
    appendNode("dependency").apply {
        appendNode("groupId", groupId)
        appendNode("artifactId", artifactId)
        appendNode("version", version)
        appendNode("scope", scope)
    }
}

// Helper function to add all dependencies
fun Node.addAllDependencies() {
    // lifecycle-viewmodel-compose
    appendDependency(
        groupId = "androidx.lifecycle",
        artifactId = "lifecycle-viewmodel-compose",
        version = "2.8.6"
    )
    // coroutine
    appendDependency(
        "org.jetbrains.kotlinx",
        "kotlinx-coroutines-core",
        "1.8.1"
    )

    // html parser
    appendDependency(
        groupId = "org.jsoup",
        artifactId = "jsoup",
        version = "1.15.3"
    )

    /// networking
    appendDependency("com.squareup.retrofit2", "retrofit", "2.11.0")
    appendDependency(
        "com.squareup.retrofit2",
        "converter-gson",
        "2.11.0"
    )
    appendDependency(
        "com.squareup.okhttp3",
        "logging-interceptor",
        "4.11.0"
    )

    /// datastore
    appendDependency(
        "androidx.datastore",
        "datastore-preferences",
        "1.1.1"
    )

    /// camera
    appendDependency("androidx.camera", "camera-core", "1.4.0")
    appendDependency("androidx.camera", "camera-camera2", "1.4.0")
    appendDependency("androidx.camera", "camera-view", "1.4.0")
    appendDependency("androidx.camera", "camera-lifecycle", "1.4.0")
    appendDependency("androidx.camera", "camera-extensions", "1.4.0")

    /// tensorflow
    appendDependency(
        "org.tensorflow",
        "tensorflow-lite-task-vision",
        "0.4.4"
    )

    /// coil
    appendDependency("io.coil-kt", "coil", "2.7.0")
    appendDependency("io.coil-kt", "coil-compose", "2.7.0")
    appendDependency("io.coil-kt", "coil-gif", "2.7.0")

    // barcode
    appendDependency("com.google.zxing", "core", "3.4.1")
}

dependencies {
    // compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)

    /// viewmodel
    api(libs.androidx.lifecycle.viewmodel.compose)

    // coroutine
    api(libs.kotlinx.coroutines.core)

    // html parser
    api(libs.jsoup)

    // network service retrofit
    api(libs.retrofit)
    api(libs.retrofit.gson)
    api(libs.okhttp.logging)

    // camerax
    api(libs.camera.core)
    api(libs.camera.camera2)
    api(libs.camera.view)
    api(libs.camera.lifecycle)
    api(libs.camera.extensions)
    // coil
    api(libs.coil)
    api(libs.coil.compose)
    api(libs.coil.svg)
    api(libs.coil.gif)

    // tensorflow
    api(libs.tensorflow.vision)

    api(libs.zxing)

    // preference datastore
    api(libs.datastore.preferences)
    implementation(libs.androidx.exifinterface)
}