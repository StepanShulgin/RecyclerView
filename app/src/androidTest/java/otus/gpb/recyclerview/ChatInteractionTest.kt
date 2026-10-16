package otus.gpb.recyclerview

import android.content.pm.ActivityInfo
import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatInteractionTest {
    @Test
    fun bindingAPlainChatClearsAllRecycledStates() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val recycler = activity.findViewById<RecyclerView>(R.id.recyclerView)
                val adapter = recycler.adapter as ChatAdapter
                val holder = adapter.onCreateViewHolder(recycler, 0)
                val decorated =
                    DemoChats.create(1)
                        .copy(
                            author = "Author",
                            isMuted = true,
                            isSecret = true,
                            isScam = true,
                            isTyping = true,
                            hasVoiceChat = true,
                            hasMention = true,
                            unreadCount = 50,
                        )
                holder.bind(decorated)
                val states =
                    listOf(
                        R.id.author,
                        R.id.muted,
                        R.id.secret,
                        R.id.scam,
                        R.id.typingDots,
                        R.id.voiceBadge,
                        R.id.mention,
                        R.id.unread,
                        R.id.pinned,
                        R.id.verified,
                        R.id.delivery,
                    )
                states.forEach {
                    assertEquals(View.VISIBLE, holder.itemView.findViewById<View>(it).visibility)
                }
                assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.preview).visibility)
                assertNull(holder.itemView.findViewById<ImageView>(R.id.preview).drawable)
                holder.bind(DemoChats.create(1))
                assertEquals(
                    View.VISIBLE,
                    holder.itemView.findViewById<View>(R.id.preview).visibility,
                )
                assertNotNull(holder.itemView.findViewById<ImageView>(R.id.preview).drawable)
                val plain = Chat(100, "Plain", "Message", "12:00", R.drawable.avatar_dima)
                holder.bind(plain)
                (states + R.id.preview).forEach {
                    assertEquals(View.GONE, holder.itemView.findViewById<View>(it).visibility)
                }
                assertNull(holder.itemView.findViewById<ImageView>(R.id.preview).drawable)
                assertNull(holder.itemView.findViewById<ImageView>(R.id.delivery).drawable)
                assertEquals(
                    "Message",
                    holder.itemView.findViewById<TextView>(R.id.message).text.toString(),
                )
                assertEquals("Plain, Message, 12:00", holder.itemView.contentDescription.toString())
                assertEquals(plain.id, holder.chatId)
                holder.clear()
                assertNull(holder.chatId)
            }
        }
    }

    @Test
    fun chatGeometryMatchesTheDesignAndLongNamesDoNotCoverTime() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val recycler = activity.findViewById<RecyclerView>(R.id.recyclerView)
                val holder = (recycler.adapter as ChatAdapter).onCreateViewHolder(recycler, 0)
                val row = holder.itemView
                val density = activity.resources.displayMetrics.density
                fun dp(value: Int) = (value * density + 0.5f).toInt()
                val width = dp(360)
                fun layout() {
                    row.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    )
                    row.layout(0, 0, row.measuredWidth, row.measuredHeight)
                }
                holder.bind(DemoChats.create(1))
                layout()
                assertEquals(dp(80), row.height)
                val avatar = row.findViewById<View>(R.id.avatar)
                assertEquals(dp(57), avatar.width)
                assertEquals(dp(57), avatar.height)
                assertEquals(dp(10), avatar.left)
                assertEquals(dp(11), avatar.top)
                assertEquals(dp(79), row.findViewById<View>(R.id.titleRow).left)
                holder.bind(
                    DemoChats.create(1)
                        .copy(
                            name =
                                "A very long chat name that must never overlap the delivery status"
                        )
                )
                layout()
                val title = row.findViewById<View>(R.id.titleRow)
                val service = row.findViewById<View>(R.id.service)
                assertTrue(title.right <= service.left)
                assertTrue(row.findViewById<View>(R.id.verified).right <= title.width)
                assertEquals(dp(80), row.height)
            }
        }
    }

    @Test
    fun swipeRemovalSurvivesActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val removed = snapshot(scenario).first().id
            swipeFirstLeft()
            awaitState(scenario) { it.size == 19 }
            assertFalse(snapshot(scenario).any { it.id == removed })
            scenario.recreate()
            awaitState(scenario) { it.size == 19 }
            assertFalse(snapshot(scenario).any { it.id == removed })
        }
    }

    @Test
    fun rightSwipeDoesNotRemoveChat() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val old = snapshot(scenario)
            onView(withId(R.id.recyclerView))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, swipeRight()))
            assertEquals(old, snapshot(scenario))
        }
    }

    @Test
    fun incompleteLeftSwipeDoesNotRemoveChat() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val old = snapshot(scenario)
            val partialSwipe =
                GeneralSwipeAction(
                    Swipe.SLOW,
                    { view -> coordinates(view, 0.9f) },
                    { view -> coordinates(view, 0.55f) },
                    Press.FINGER,
                )
            onView(withId(R.id.recyclerView))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, partialSwipe))
            assertEquals(old, snapshot(scenario))
        }
    }

    @Test
    fun rapidConsecutiveSwipesRemoveTheCorrectChats() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val old = snapshot(scenario)
            repeat(3) { index ->
                swipeFirstLeft()
                awaitState(scenario) { it.size == old.size - index - 1 }
            }
            assertEquals(old.drop(3), snapshot(scenario))
        }
    }

    @Test
    fun reachingTheEndLoadsAnotherPageAndPreservesItAfterRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val old = snapshot(scenario)
            onView(withId(R.id.recyclerView))
                .perform(scrollToPosition<RecyclerView.ViewHolder>(old.lastIndex))
            awaitState(scenario) { it.size >= old.size + ChatStore.PAGE_SIZE }
            val loaded = snapshot(scenario)
            assertEquals(old, loaded.take(old.size))
            assertEquals(loaded.size, loaded.map { it.id }.distinct().size)
            scenario.recreate()
            awaitState(scenario) { it == loaded }
            assertEquals(loaded, snapshot(scenario))
        }
    }

    @Test
    fun removingMostChatsLoadsMoreWithoutRequiringAnotherScroll() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val deleted = snapshot(scenario).drop(1).map { it.id }.toSet()
            scenario.onActivity { activity ->
                val model = ViewModelProvider(activity)[ChatViewModel::class.java]
                deleted.forEach(model::remove)
            }
            awaitState(scenario) { chats -> chats.size > 1 && chats.none { it.id in deleted } }
            assertTrue(snapshot(scenario).none { it.id in deleted })
        }
    }

    @Test
    fun landscapeCanLoadAndDeleteChats() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            val removed = snapshot(scenario).first().id
            swipeFirstLeft()
            awaitState(scenario) { it.none { chat -> chat.id == removed } }
            val oldCount = snapshot(scenario).size
            onView(withId(R.id.recyclerView))
                .perform(scrollToPosition<RecyclerView.ViewHolder>(oldCount - 1))
            awaitState(scenario) { it.size > oldCount }
        }
    }

    private fun swipeFirstLeft() {
        onView(withId(R.id.recyclerView))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, swipeLeft()))
    }

    private fun snapshot(scenario: ActivityScenario<MainActivity>): List<Chat> {
        var chats = emptyList<Chat>()
        scenario.onActivity { activity ->
            val adapter =
                activity.findViewById<RecyclerView>(R.id.recyclerView).adapter as ChatAdapter
            chats = adapter.currentList.toList()
        }
        return chats
    }

    private fun awaitState(
        scenario: ActivityScenario<MainActivity>,
        predicate: (List<Chat>) -> Boolean,
    ) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        do {
            if (predicate(snapshot(scenario))) return
            Thread.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        assertTrue("Timed out waiting for the RecyclerView state", predicate(snapshot(scenario)))
    }

    private fun coordinates(view: View, fraction: Float): FloatArray {
        val position = IntArray(2)
        view.getLocationOnScreen(position)
        return floatArrayOf(position[0] + view.width * fraction, position[1] + view.height / 2f)
    }
}
