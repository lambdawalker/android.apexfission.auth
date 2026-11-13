import java.util.zip.ZipFile
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Extracts only the pinned model's assets; no model classes or old core enter the runtime. */
abstract class PrepareCardModelAssets : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val modelArchive: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val archives: ArchiveOperations

    @get:Inject
    abstract val files: FileSystemOperations

    @TaskAction
    fun extract() {
        files.sync {
            from(archives.zipTree(modelArchive)) {
                include("assets/**")
                eachFile { path = path.removePrefix("assets/") }
                includeEmptyDirs = false
            }
            into(outputDirectory)
        }
    }
}

plugins {
    id("com.android.library")
    id("com.apexfission.android-repo-setup")
    alias(libs.plugins.screenshot)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.apexfission.android.auth.library"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    testOptions {
        unitTests.isReturnDefaultValues = true // JVM tests have no Android Log implementation.

    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.apexfission.permissions)
    implementation(libs.apexfission.card.detector)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)

    // Ktor client & Serialization
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
    screenshotTestImplementation(libs.screenshot.validation.api)
    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Resolve the model in isolation: its JitPack artifact ID is shared with the detector.
// Only assets enter this library, never the model AAR's classes or pinned core 0.1.0.
val cardModelAssets by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}
dependencies { cardModelAssets(libs.apexfission.card.model) }
androidComponents.onVariants { variant ->
    val prepareAssets = tasks.register<PrepareCardModelAssets>(
        "prepare${variant.name.replaceFirstChar { it.uppercaseChar() }}CardModelAssets"
    ) {
        modelArchive.set(layout.file(cardModelAssets.elements.map { it.single().asFile }))
    }
    variant.sources.assets?.addGeneratedSourceDirectory(
        prepareAssets, PrepareCardModelAssets::outputDirectory
    )
}
android.androidResources.noCompress += "tflite"
// BEGIN MAVEN PUBLICATION ONLY


// Public publication still requires an owner-selected license. Only the isolated
// unsigned verification repository may use fixture POM metadata.
val verificationPublication = gradle.startParameter.taskNames.any {
    it.endsWith(":publishAllPublicationsToVerificationRepository")
}
val projectUrl = "https://github.com/lambdawalker/android.apexfission.auth"
val pomLicenseName = providers.gradleProperty("POM_LICENSE_NAME").orElse("")
val pomLicenseUrl = providers.gradleProperty("POM_LICENSE_URL").orElse("")
mavenPublishing {
    pom {
        name.set("Attestra Auth")
        description.set("Android email, passkey and identity-capture onboarding for Attestra.")
        inceptionYear.set("2026")
        url.set(projectUrl)
        licenses {
            license {
                name.set(pomLicenseName.map {
                    it.ifBlank { if (verificationPublication) "CI validation fixture only" else "" }
                })
                url.set(pomLicenseUrl.map {
                    it.ifBlank { if (verificationPublication) "https://example.invalid/license" else "" }
                })
            }
        }
        developers { developer {
            id.set("lambdawalker")
            name.set("David Garcia")
            url.set("https://github.com/lambdawalker")
        } }
        scm {
            url.set(projectUrl)
            connection.set("scm:git:$projectUrl.git")
            developerConnection.set("scm:git:ssh://git@github.com/lambdawalker/android.apexfission.auth.git")
        }
    }
}
// Auth bundles assets from a separately pinned model AAR. Its class archive must
// never be merged, and the generic Git-backed asset contract cannot represent it.
val verifyBundledCardModel = tasks.register("verifyBundledCardModel") {
    dependsOn("assembleRelease")
    doLast {
        ZipFile(cardModelAssets.singleFile).use { model ->
            ZipFile(layout.buildDirectory.file("outputs/aar/auth-release.aar").get().asFile).use { output ->
                val entries = model.entries().asSequence().filter {
                    !it.isDirectory && it.name.startsWith("assets/") && it.name.endsWith(".tflite")
                }.toList()
                require(entries.isNotEmpty()) { "Pinned model AAR contains no TFLite assets" }
                entries.forEach { source ->
                    val expected = model.getInputStream(source).use { it.readBytes() }
                    val packaged = requireNotNull(output.getEntry(source.name)) { "Missing bundled model: ${source.name}" }
                    val actual = output.getInputStream(packaged).use { it.readBytes() }
                    require(expected.contentEquals(actual)) { "Bundled model differs from the pinned model AAR" }
                    require(actual.size > 8 && actual.copyOfRange(4, 8).contentEquals("TFL3".toByteArray())) { "Invalid TFLite asset" }
                }
            }
        }
    }
}
tasks.withType<org.gradle.api.publish.maven.tasks.AbstractPublishToMaven>().configureEach {
    dependsOn(verifyBundledCardModel)
}
val verifyPublicationLicense = tasks.register("verifyPublicationLicense") {
    doLast {
        require(pomLicenseName.get().isNotBlank() && pomLicenseUrl.get().startsWith("https://")) {
            "Owner must set POM_LICENSE_NAME/POM_LICENSE_URL in gradle.properties before publishing; no license is inferred"
        }
    }
}
tasks.configureEach {
    if (name.contains("MavenCentral", ignoreCase = true)) {
        dependsOn(verifyPublicationLicense, verifyBundledCardModel)
    }
    if (this is org.gradle.api.publish.maven.tasks.PublishToMavenLocal) dependsOn(verifyPublicationLicense)
}
// END MAVEN PUBLICATION ONLY
