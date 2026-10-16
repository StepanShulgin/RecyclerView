package otus.gpb.recyclerview

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import kotlin.math.roundToInt
import otus.gpb.recyclerview.databinding.ItemChatBinding

class ChatAdapter : ListAdapter<Chat, ChatAdapter.ChatViewHolder>(ChatDiffCallback) {
    private var icons: ChatIcons? = null

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val iconCache = icons ?: ChatIcons(parent.context).also { icons = it }
        return ChatViewHolder(binding, iconCache)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ChatViewHolder) {
        holder.clear()
        super.onViewRecycled(holder)
    }

    class ChatViewHolder
    internal constructor(private val binding: ItemChatBinding, private val icons: ChatIcons) :
        RecyclerView.ViewHolder(binding.root) {
        private val numberFormat = NumberFormat.getIntegerInstance()

        init {
            ViewCompat.setScreenReaderFocusable(binding.root, true)
            binding.root.children.forEach {
                it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            }
            with(binding) {
                typingDots.setImageDrawable(icons.drawable(R.raw.chat_typing))
                verified.setImageDrawable(icons.drawable(R.raw.chat_verified))
                muted.setImageDrawable(icons.drawable(R.raw.chat_muted))
                secret.setImageDrawable(icons.drawable(R.raw.chat_secret))
                scam.setImageDrawable(icons.drawable(R.raw.chat_scam))
                voiceBadge.setImageDrawable(icons.drawable(R.raw.chat_voice))
                mention.setImageDrawable(icons.drawable(R.raw.chat_mention))
                pinned.setImageDrawable(icons.drawable(R.raw.chat_pinned))
            }
        }

        var chatId: Long? = null
            private set

        fun bind(chat: Chat) =
            with(binding) {
                chatId = chat.id
                avatar.setImageResource(chat.avatar)
                name.text = chat.name
                time.text = chat.time
                author.text = chat.author
                author.isVisible = chat.author != null
                val previewResource = chat.preview.takeUnless { chat.isTyping }
                preview.isVisible = previewResource != null
                if (previewResource != null) preview.setImageResource(previewResource)
                else preview.setImageDrawable(null)
                message.text =
                    if (chat.isTyping) root.context.getString(R.string.typing) else chat.message
                message.setTextColor(
                    ContextCompat.getColor(
                        root.context,
                        if (chat.isTyping) R.color.chat_typing else R.color.chat_message,
                    )
                )
                message.textSize = if (chat.isTyping) 14f else 15f
                typingDots.isVisible = chat.isTyping
                verified.isVisible = chat.isVerified
                muted.isVisible = chat.isMuted
                secret.isVisible = chat.isSecret
                scam.isVisible = chat.isScam
                voiceBadge.isVisible = chat.hasVoiceChat
                mention.isVisible = chat.hasMention
                pinned.isVisible = chat.isPinned
                delivery.isVisible = chat.delivery != Delivery.NONE
                delivery.setImageDrawable(
                    when (chat.delivery) {
                        Delivery.SENT -> icons.drawable(R.raw.chat_sent)
                        Delivery.READ -> icons.drawable(R.raw.chat_read)
                        Delivery.NONE -> null
                    }
                )
                unread.isVisible = chat.unreadCount > 0
                unread.text = numberFormat.format(chat.unreadCount)
                unread.backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            root.context,
                            if (chat.isUnreadMuted) R.color.chat_counter_muted
                            else R.color.chat_counter,
                        )
                    )
                val density = root.resources.displayMetrics.density
                titleRow.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    marginStart = ((if (chat.isSecret) 75 else 79) * density).roundToInt()
                    topMargin = ((if (chat.author == null) 8 else 9) * density).roundToInt()
                }
                messageRow.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = ((if (chat.author == null) 5 else 4) * density).roundToInt()
                }
                root.contentDescription =
                    buildList {
                            add(chat.name)
                            if (chat.isVerified) add(root.context.getString(R.string.verified))
                            if (chat.isMuted) add(root.context.getString(R.string.muted))
                            if (chat.isSecret) add(root.context.getString(R.string.secret_chat))
                            if (chat.isScam) add(root.context.getString(R.string.scam))
                            chat.author?.let(::add)
                            add(message.text.toString())
                            if (chat.unreadCount > 0)
                                add(
                                    root.resources.getQuantityString(
                                        R.plurals.unread_messages,
                                        chat.unreadCount,
                                        chat.unreadCount,
                                    )
                                )
                            if (chat.hasMention) add(root.context.getString(R.string.mentioned))
                            if (chat.hasVoiceChat) add(root.context.getString(R.string.voice_chat))
                            if (chat.isPinned) add(root.context.getString(R.string.pinned))
                            when (chat.delivery) {
                                Delivery.SENT -> add(root.context.getString(R.string.sent))
                                Delivery.READ -> add(root.context.getString(R.string.read))
                                Delivery.NONE -> Unit
                            }
                            add(chat.time)
                        }
                        .joinToString(", ")
            }

        fun clear() {
            chatId = null
        }
    }
}

internal object ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
    override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean = oldItem == newItem
}
