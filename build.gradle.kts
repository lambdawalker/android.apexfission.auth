// Build and publishing conventions are maintained by gradle.android-repo-setup.
plugins {
    id("com.apexfission.android-repo-setup.root") version "v0.1.26"
    id("com.apexfission.android-repo-setup.application") version "v0.1.26" apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
}
