package otus.gpb.recyclerview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewModel : ViewModel() {
    private val store = ChatStore()
    private val mutableChats = MutableLiveData(store.snapshot)
    val chats: LiveData<List<Chat>> = mutableChats

    fun remove(id: Long) {
        if (store.remove(id)) {
            mutableChats.value = store.snapshot
        }
    }

    fun loadNextPage() {
        store.loadNextPage()
        mutableChats.value = store.snapshot
    }
}
