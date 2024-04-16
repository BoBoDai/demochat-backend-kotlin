package com.example.demochatbackendkotlin.config

import com.example.demochatbackendkotlin.model.Message
import com.google.cloud.firestore.Query
import com.google.firebase.FirebaseApp
import com.google.firebase.cloud.FirestoreClient
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class FirebaseListener(
    val simpMessagingTemplate: SimpMessagingTemplate,
    @Qualifier("realTimeBase") private val realTimeBase: FirebaseApp,
    @Qualifier("fireStoreBase") private val fireStoreBase: FirebaseApp,
) {
    private lateinit var listener: ChildEventListener

    @EventListener(ApplicationReadyEvent::class)
    fun startListening() {
        val databaseRef = FirebaseDatabase.getInstance(realTimeBase).getReference("service/chat")
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val message = dataSnapshot.value as Map<*, *>
                simpMessagingTemplate.convertAndSend(
                    "/topic/messages",
                    listOf(Message(message.get("text") as String, message.get("timestamp") as Long))
                )
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                println("Child Changed: ${dataSnapshot.key}")
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                println("Child Removed: ${dataSnapshot.key}")
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                println("Child Moved: ${dataSnapshot.key}")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("Child Event was cancelled: ${databaseError.toException()}")
            }
        }

        val startTime = System.currentTimeMillis()
        val query = databaseRef.orderByChild("timestamp").startAt(startTime.toDouble())
        listener = query.addChildEventListener(childEventListener)
    }

    @EventListener(ApplicationReadyEvent::class)
    fun firestoreListening() {
        val databaseRef = FirestoreClient.getFirestore(fireStoreBase).collection("service/chat").document("userId")
            .collection("messages").orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)

        databaseRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                val message = snapshot as Map<*, *>
                simpMessagingTemplate.convertAndSend(
                    "/topic/messages",
                    listOf(Message(message.get("text") as String, message.get("timestamp") as Long))
                )
            }
        }
    }

    @PreDestroy
    fun stopListening() {
        FirebaseDatabase.getInstance().getReference("service/chat").removeEventListener(listener)
    }
}
