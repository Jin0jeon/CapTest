plugins {
    id("com.android.application")
}


android {
    namespace = "com.example.captest"
    compileSdk = 34

//    packagingOptions{
//        exclude("META-INF/DEPENDENCIES")
//        exclude("META-INF/functions_es.properties")
//    }

    defaultConfig {
        applicationId = "com.example.captest"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildFeatures{
        viewBinding = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

}


dependencies {
    // Location Helper 를 위한 의존성
    implementation ("com.google.android.gms:play-services-location:21.3.0")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation(files("libs\\jxl-2.6.12.jar"))
    implementation("androidx.activity:activity:1.9.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    //implementation("com.github.MatteoBattilana:WeatherView:3.0.0")
    //implementation("com.github.Dimezis:BlurView:version-2.0.3")

    // glide 라이브러리 사용 의존성
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation ("com.github.angads25:toggle:1.1.0")

    // google calendar 의존성
//    implementation("com.google.api-client:google-api-client-android:1.33.2")
//    implementation("com.google.apis:google-api-services-calendar:v3-rev305-1.25.0")
//    implementation("com.google.android.gms:play-services-auth:21.2.0")
//    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
//    implementation("com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0")
//    implementation("com.google.api-client:google-api-client-gson:1.33.2")
}