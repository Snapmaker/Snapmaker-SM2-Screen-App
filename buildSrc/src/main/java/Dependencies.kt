object Versions {
    val supportV4 = "1.0.0"
    val appCompat = "1.3.1"
    val constraintLayout = "1.1.3"
    val lifecycle = "2.3.1"
    val material = "1.4.0"
    val butterKnife = "10.2.0"
    val rxJava = "2.2.12"
    val rxAndroid = "2.1.1"
    val autoDispose = "1.4.0"
    val okio = "2.4.0"
    val logger = "2.2.0"
    val retrofit = "2.5.0"
    val fileUpload = "1.4"
    val httpCore = "4.4.11"
    val andServer = "2.0.4"
    val firebase = "28.1.0"
    val libaums = "0.7.4"
    val segmentedButton = "v1.2.2@aar"
    val glide = "4.11.0"
    val leakCanary = "2.7"
    val aRouter = "1.5.2"
    val junit = "4.12"
    val androidxAnnotation = "1.2.0"
}

object Libs {
    // Official support libraries
    val supportV4 = "androidx.legacy:legacy-support-v4:${Versions.supportV4}"
    val appCompat = "androidx.appcompat:appcompat:${Versions.appCompat}"
    val constraintLayout = "androidx.constraintlayout:constraintlayout:${Versions.constraintLayout}"

    val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel:${Versions.lifecycle}"
    val lifecycleJava8 = "androidx.lifecycle:lifecycle-common-java8:${Versions.lifecycle}"
    val material = "com.google.android.material:material:${Versions.material}"
    val androidxAnnotation = "androidx.annotation:annotation:${Versions.androidxAnnotation}"

    // 3rd party libs ======
    // ButterKnife
    val butterknife =  "com.jakewharton:butterknife:${Versions.butterKnife}"
    val butterknifeAnnotation =  "com.jakewharton:butterknife-compiler:${Versions.butterKnife}"

    // RxJava
    val rxjava =  "io.reactivex.rxjava2:rxjava:${Versions.rxJava}"
    val rxAndroid =  "io.reactivex.rxjava2:rxandroid:${Versions.rxAndroid}"
    val autoDispose =  "com.uber.autodispose:autodispose:${Versions.autoDispose}"
    val autoDisposeLifecycle =  "com.uber.autodispose:autodispose-lifecycle:${Versions.autoDispose}"
    val autoDisposeArchComponents =  "com.uber.autodispose:autodispose-android-archcomponents:${Versions.autoDispose}"

    // IO
    val okio = "com.squareup.okio:okio:${Versions.okio}"

    // Logger
    val logger = "com.orhanobut:logger:${Versions.logger}"

    // HTTP
    val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
    val retrofitRxAdapter = "com.squareup.retrofit2:adapter-rxjava2:${Versions.retrofit}"
    val retrofitGsonConverter = "com.squareup.retrofit2:converter-gson:${Versions.retrofit}"
    val fileUpload = "com.yanzhenjie.apache:fileupload:${Versions.fileUpload}"
    val httpCore = "com.yanzhenjie.apache:httpcore:${Versions.httpCore}"
    val andServerApi = "com.yanzhenjie.andserver:api:${Versions.andServer}"
    val andServerAnnotation = "com.yanzhenjie.andserver:processor:${Versions.andServer}"

    // Firebase
    val firebaseBOM = "com.google.firebase:firebase-bom:${Versions.firebase}"
    val firebaseAnalytics = "com.google.firebase:firebase-analytics"
    val firebaseCrashlytics = "com.google.firebase:firebase-crashlytics"

    // USB
    val libaums = "com.github.mjdev:libaums:${Versions.libaums}"

    // UI widgets
    // https://github.com/ceryle/SegmentedButton
    val segmentedButton = "com.github.ceryle:SegmentedButton:${Versions.segmentedButton}"
    // Gif
    val glide = "com.github.bumptech.glide:glide:${Versions.glide}"
    val glideAnnotation = "com.github.bumptech.glide:compiler:${Versions.glide}"

    // LeakCanary
    val leakCanary = "com.squareup.leakcanary:leakcanary-android:${Versions.leakCanary}"

    //ARouter
    val aRouter = "com.alibaba:arouter-api:${Versions.aRouter}"
    val aRouterAnnotation = "com.alibaba:arouter-compiler:${Versions.aRouter}"

    // Test
    val junit = "junit:junit:${Versions.junit}"
}
