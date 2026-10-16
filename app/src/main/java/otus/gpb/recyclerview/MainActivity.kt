package otus.gpb.recyclerview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import otus.gpb.recyclerview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val chatAdapter = ChatAdapter()
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var layoutManager: LinearLayoutManager
    private var pendingSubmission = false
    private var pageCheckScheduled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.menuIcon.setImageDrawable(ChatIcons(this).drawable(R.raw.chat_menu))
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, binding.root).isAppearanceLightNavigationBars =
            true
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val safe =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        layoutManager = LinearLayoutManager(this)
        binding.recyclerView.apply {
            this.layoutManager = this@MainActivity.layoutManager
            adapter = chatAdapter
            addItemDecoration(
                ChatDividerDecoration(
                    color = ContextCompat.getColor(context, R.color.chat_divider),
                    thickness = resources.getDimension(R.dimen.chat_divider_height),
                    textInset = resources.getDimensionPixelSize(R.dimen.chat_text_inset),
                )
            )
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        schedulePageCheck()
                    }
                }
            )
        }
        ItemTouchHelper(ChatSwipeCallback(binding.recyclerView, viewModel::remove))
            .attachToRecyclerView(binding.recyclerView)
        viewModel.chats.observe(this) { chats ->
            pendingSubmission = true
            chatAdapter.submitList(chats) {
                pendingSubmission = false
                schedulePageCheck()
            }
        }
    }

    private fun schedulePageCheck() {
        if (pageCheckScheduled) return
        pageCheckScheduled = true
        binding.recyclerView.post {
            pageCheckScheduled = false
            requestNextPageIfNeeded()
        }
    }

    private fun requestNextPageIfNeeded() {
        if (
            isFinishing ||
                isDestroyed ||
                pendingSubmission ||
                binding.recyclerView.isComputingLayout
        )
            return
        val count = chatAdapter.itemCount
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (
            count == 0 ||
                (lastVisible != RecyclerView.NO_POSITION &&
                    lastVisible >= count - PREFETCH_DISTANCE)
        ) {
            viewModel.loadNextPage()
        }
    }

    companion object {
        private const val PREFETCH_DISTANCE = 3
    }
}
