package otus.gpb.recyclerview

import androidx.annotation.DrawableRes

data class Chat(
    val id: Long,
    val name: String,
    val message: String,
    val time: String,
    @DrawableRes val avatar: Int,
    val author: String? = null,
    @DrawableRes val preview: Int? = null,
    val delivery: Delivery = Delivery.NONE,
    val isVerified: Boolean = false,
    val isMuted: Boolean = false,
    val isSecret: Boolean = false,
    val isScam: Boolean = false,
    val isTyping: Boolean = false,
    val isPinned: Boolean = false,
    val hasMention: Boolean = false,
    val hasVoiceChat: Boolean = false,
    val unreadCount: Int = 0,
    val isUnreadMuted: Boolean = false,
)

enum class Delivery {
    NONE,
    SENT,
    READ,
}
