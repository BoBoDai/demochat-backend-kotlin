package com.example.demochatbackendkotlin.controller

import com.example.demochatbackendkotlin.model.Message
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.cloud.FirestoreClient
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller


@Controller
class ChatController(
    val simpMessagingTemplate: SimpMessagingTemplate,
    realTimeBase: FirebaseApp,
    fireStoreBase: FirebaseApp
) {
    val realTimeDatabase: FirebaseDatabase = FirebaseDatabase.getInstance(realTimeBase)
    val fireStoreDatabase: Firestore = FirestoreClient.getFirestore(fireStoreBase)

    @MessageMapping("/chat")
    fun sendMessage(message: Message) {
        val ref = realTimeDatabase.getReference("service/chat")
        ref.push().setValueAsync(message)
    }

    @MessageMapping("/chat")
    fun fsSendMessage(message: Message) {
        val ref = fireStoreDatabase.collection("service/chat").document("userId").collection("messages")
        ref.add(message)
    }

    @MessageMapping("/data/sync")
    fun syncData() {
        val ref = realTimeDatabase.getReference("service/chat")
        val limitToLast = ref.limitToLast(5)
        limitToLast.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue() as Map<*, *>
                val messageList = value.values.map { it as Map<*, *> }
                    .map { Message(it.get("text") as String, it.get("timestamp") as Long) }.sortedBy { it.timestamp }
                    .toList()
                simpMessagingTemplate.convertAndSend(
                    "/topic/messages",
                    messageList
                )
            }

            override fun onCancelled(error: DatabaseError?) {
                TODO("Not yet implemented")
            }
        })
    }
}
