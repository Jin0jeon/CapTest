pluginManagement {
    repositories {
        google()
        mavenCentral()  // jcenter 대신 권장
        gradlePluginPortal()
        maven { url = uri("https://www.jitpack.io" ) } // JitPack 추가
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven { url = uri("https://www.jitpack.io" ) }
    }
}

rootProject.name = "CapTest"
include(":app")