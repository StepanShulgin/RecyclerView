package otus.gpb.recyclerview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatStoreTest {
    @Test
    fun initialPageHasExpectedSizeAndUniqueIds() {
        val chats = ChatStore().snapshot
        assertEquals(ChatStore.INITIAL_PAGE_SIZE, chats.size)
        assertEquals(chats.size, chats.map { it.id }.distinct().size)
        assertEquals("Dima Murantsev", chats.first().name)
        assertTrue(chats.all { it.avatar != 0 })
    }

    @Test
    fun rapidRemovalsUseStableIdsInsteadOfChangingPositions() {
        val store = ChatStore()
        val old = store.snapshot
        assertTrue(store.remove(old[0].id))
        assertTrue(store.remove(old[1].id))
        assertEquals(old.drop(2), store.snapshot)
        assertEquals(ChatStore.INITIAL_PAGE_SIZE, old.size)
    }

    @Test
    fun repeatedRemovalIsSafeAndPaginationDoesNotReuseDeletedIds() {
        val store = ChatStore()
        val id = store.snapshot.first().id
        assertTrue(store.remove(id))
        assertFalse(store.remove(id))
        store.loadNextPage()
        assertEquals(29, store.snapshot.size)
        assertEquals(29, store.snapshot.map { it.id }.distinct().size)
        assertFalse(store.snapshot.any { it.id == id })
    }

    @Test
    fun paginationAppendsWithoutChangingExistingChats() {
        val store = ChatStore()
        val old = store.snapshot
        repeat(10) { store.loadNextPage() }
        val current = store.snapshot
        assertEquals(old, current.take(old.size))
        assertEquals(120, current.size)
        assertEquals(current.size, current.map { it.id }.distinct().size)
        assertTrue(current.zipWithNext().all { (previous, next) -> previous.id < next.id })
    }

    @Test
    fun invalidPageSizeDoesNotChangeStateOrConsumeIds() {
        val store = ChatStore()
        val old = store.snapshot
        for (size in listOf(0, -1)) {
            assertThrows(IllegalArgumentException::class.java) { store.loadNextPage(size) }
            assertEquals(old, store.snapshot)
        }
        store.loadNextPage(1)
        assertEquals(old.last().id + 1, store.snapshot.last().id)
    }

    @Test
    fun removingAllChatsAndLoadingAgainDoesNotResurrectDeletedIds() {
        val store = ChatStore()
        val deletedIds = store.snapshot.map { it.id }.toSet()
        deletedIds.forEach(store::remove)
        assertTrue(store.snapshot.isEmpty())
        store.loadNextPage()
        assertEquals(ChatStore.PAGE_SIZE, store.snapshot.size)
        assertTrue(store.snapshot.none { it.id in deletedIds })
    }

    @Test
    fun removingUnknownChatLeavesStateUntouched() {
        val store = ChatStore()
        val old = store.snapshot
        assertFalse(store.remove(Long.MAX_VALUE))
        assertEquals(old, store.snapshot)
    }

    @Test
    fun diffCallbackSeparatesIdentityAndContent() {
        val chat = ChatStore().snapshot.first()
        val edited = chat.copy(message = "Changed")
        assertTrue(ChatDiffCallback.areItemsTheSame(chat, edited))
        assertFalse(ChatDiffCallback.areContentsTheSame(chat, edited))
        assertTrue(ChatDiffCallback.areContentsTheSame(chat, chat.copy()))
        assertFalse(ChatDiffCallback.areItemsTheSame(chat, chat.copy(id = chat.id + 1)))
    }

    @Test
    fun diffCallbackDetectsEveryVisualStateChange() {
        val chat = DemoChats.create(1)
        val changes =
            listOf(
                chat.copy(name = "Changed"),
                chat.copy(message = "Changed"),
                chat.copy(time = "12:00"),
                chat.copy(avatar = R.drawable.avatar_news),
                chat.copy(author = "Author"),
                chat.copy(preview = null),
                chat.copy(delivery = Delivery.READ),
                chat.copy(isVerified = !chat.isVerified),
                chat.copy(isMuted = !chat.isMuted),
                chat.copy(isSecret = !chat.isSecret),
                chat.copy(isScam = !chat.isScam),
                chat.copy(isTyping = !chat.isTyping),
                chat.copy(isPinned = !chat.isPinned),
                chat.copy(hasMention = !chat.hasMention),
                chat.copy(hasVoiceChat = !chat.hasVoiceChat),
                chat.copy(unreadCount = 50),
                chat.copy(isUnreadMuted = !chat.isUnreadMuted),
            )
        changes.forEach {
            assertTrue(ChatDiffCallback.areItemsTheSame(chat, it))
            assertFalse(ChatDiffCallback.areContentsTheSame(chat, it))
        }
    }

    @Test
    fun demoPageIncludesAllStatesFromTheDesign() {
        val chats = ChatStore().snapshot
        assertTrue(chats.any { it.isVerified })
        assertTrue(chats.any { it.isMuted })
        assertTrue(chats.any { it.isSecret && it.isScam })
        assertTrue(chats.any { it.isTyping })
        assertTrue(chats.any { it.isPinned && it.hasMention })
        assertTrue(chats.any { it.hasVoiceChat })
        assertTrue(chats.any { it.unreadCount > 0 })
        assertTrue(chats.any { it.author != null && it.preview != null })
        assertEquals(Delivery.entries.toSet(), chats.map { it.delivery }.toSet())
    }
}
