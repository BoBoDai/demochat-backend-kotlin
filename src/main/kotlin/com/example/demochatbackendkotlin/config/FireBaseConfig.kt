package com.example.demochatbackendkotlin.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

@Configuration
class FireBaseConfig {

    @Bean
    fun realTimeBase(): FirebaseApp {
        // realtime db
        val serviceAccount = FileInputStream("./account/serviceAccountKey.json")
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .setDatabaseUrl("https://demochat-7f0be-default-rtdb.firebaseio.com")
            .build()

        return FirebaseApp.initializeApp(options)
    }

    @Bean
    fun fireStoreBase(): FirebaseApp {
        // fireStore db
        val serviceAccount = FileInputStream("./account/serviceAccountKey.json")
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        return FirebaseApp.initializeApp(options)
    }
}
