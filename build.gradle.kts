// Build and publishing conventions are maintained by gradle.android-repo-setup.
plugins {
    id("com.apexfission.android-repo-setup.root") version "v0.1.23"
    id("com.apexfission.android-repo-setup.application") version "v0.1.23" apply false
    id("com.android.application") apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.android.library") apply false
}
