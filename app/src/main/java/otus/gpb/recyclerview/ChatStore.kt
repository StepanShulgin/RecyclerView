package otus.gpb.recyclerview

class ChatStore {
    private val chats = mutableListOf<Chat>()
    private var nextId = 1L

    val snapshot: List<Chat>
        get() = chats.toList()

    init {
        loadNextPage(INITIAL_PAGE_SIZE)
    }

    fun remove(id: Long): Boolean = chats.removeAll { it.id == id }

    fun loadNextPage(size: Int = PAGE_SIZE) {
        require(size > 0) { "Page size must be positive" }
        repeat(size) { chats += DemoChats.create(nextId++) }
    }

    companion object {
        const val INITIAL_PAGE_SIZE = 20
        const val PAGE_SIZE = 10
    }
}
