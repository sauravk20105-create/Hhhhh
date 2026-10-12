package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ChatMessage

@Entity(tableName = "chat_messages")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val actionTag: String? = null
) {
    fun toDomain(): ChatMessage = ChatMessage(
        id = id,
        text = text,
        isUser = isUser,
        timestamp = timestamp,
        actionTag = actionTag
    )

    companion object {
        fun fromDomain(message: ChatMessage): ChatEntity = ChatEntity(
            id = message.id,
            text = message.text,
            isUser = message.isUser,
            timestamp = message.timestamp,
            actionTag = message.actionTag
        )
    }
}
