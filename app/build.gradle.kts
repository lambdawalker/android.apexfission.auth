import com.apexfission.release.EndpointValidation

plugins {
    alias(libs.plugins.screenshot)
    id("com.apexfission.android-repo-setup.application")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

androidRepoSetup {
    signing {
        defaultAlias = "attestra"
        createSignedDebug = true
        debugSourceDirectory = "src/debug/java"
    }

    endpoints {
        configFileProperty = "attestraConfigFile"

        string(
            property = "attestraEnvironment",
            buildConfig = "ATTESTRA_ENVIRONMENT",
            defaultValue = "dev",
            requiredInConfig = true,
            validation = EndpointValidation.ENVIRONMENT,
            allowBlank = false,
        )
        string(
            property = "attestraApiBaseUrl",
            buildConfig = "AUTH_API_BASE_URL",
            requiredInConfig = true,
            validation = EndpointValidation.HTTPS_URL,
        )
        string(
            property = "attestraIndexUrl",
            buildConfig = "ATTESTRA_INDEX_URL",
            defaultValue = "https://index.attestrabond.com/v1/environments",
            validation = EndpointValidation.HTTPS_URL,
            allowBlank = false,
        )
        string(
            property = "attestraLinkHost",
            buildConfig = "AUTH_LINK_HOST",
            manifestPlaceholder = "attestraLinkHost",
            defaultValue = "{attestraEnvironment}.attestrabond.com",
            requiredInConfig = true,
            validation = EndpointValidation.HOSTNAME,
            allowBlank = false,
        )
        string(
            property = "attestraAwsRegion",
            buildConfig = "AWS_REGION",
            requiredInConfig = true,
        )
        string(
            property = "attestraCognitoUserPoolId",
            buildConfig = "COGNITO_USER_POOL_ID",
            requiredInConfig = true,
        )
        string(
            property = "attestraCognitoClientId",
            buildConfig = "COGNITO_CLIENT_ID",
            requiredInConfig = true,
        )
        boolean(
            property = "attestraCaptureEnabled",
            buildConfig = "CAPTURE_ENABLED",
            defaultValue = false,
            requiredInConfig = true,
        )
        string(
            property = "attestraCaptureDocumentType",
            buildConfig = "CAPTURE_DOCUMENT_TYPE",
            defaultValue = "sample_card",
            requiredInConfig = true,
        )
    }
}

android {
    namespace = "com.apexfission.android.auth"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.apexfission.android.auth"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":auth"))
    implementation(libs.apexfission.demo.common)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Ktor client & Serialization
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
    screenshotTestImplementation(libs.screenshot.validation.api)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
