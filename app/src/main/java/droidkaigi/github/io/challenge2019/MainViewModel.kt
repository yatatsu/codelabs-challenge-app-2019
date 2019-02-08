package droidkaigi.github.io.challenge2019

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import droidkaigi.github.io.challenge2019.data.db.ArticlePreferences
import droidkaigi.github.io.challenge2019.domain.Item
import droidkaigi.github.io.challenge2019.domain.ItemRepository
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

interface MainViewModelType {
    val alreadyReadStories : LiveData<Set<String>>

    val stories : LiveData<List<Item>>

    val showableError : LiveData<Throwable>

    val refreshing : LiveData<Boolean>

    val progressBarVisibility : LiveData<Boolean>

    fun refresh(item: Item)

    fun refreshAll()

    fun markAsRead(id: Long)

    fun onErrorShown()
}

private const val SIZE_TOP_STORY_ITEMS = 20

class MainViewModel(
  private val articlePreferences: ArticlePreferences,
  private val itemRepository: ItemRepository
) : ViewModel(), MainViewModelType, CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override val alreadyReadStories: MutableLiveData<Set<String>> = MutableLiveData()
    override val stories: MutableLiveData<List<Item>> = MutableLiveData()
    override val showableError: MutableLiveData<Throwable> = MutableLiveData()
    override val refreshing: MutableLiveData<Boolean> = MutableLiveData()
    override val progressBarVisibility: MutableLiveData<Boolean> = MutableLiveData()

    init {
        alreadyReadStories.value = articlePreferences.getArticleIds()
        loadTopStories()
        progressBarVisibility.value = true
    }

    override fun onCleared() {
        job.cancel()
    }

    override fun refresh(item: Item) {
        launch {
            refreshing.value = true
            try {
                val newItem = withContext(Dispatchers.Default) {
                    itemRepository.getItem(item.id)
                }
                stories.value?.toMutableList().let { list ->
                    val index = list?.indexOf(item)
                    if (index != null && index > 0) {
                        list[index] = newItem
                        stories.value = list
                    }
                }
            } catch (e: Exception) {
                showableError.value = e
            }
            refreshing.value = false
        }
    }

    override fun refreshAll() {
        refreshing.value = true
        loadTopStories()
    }

    override fun markAsRead(id: Long) {
        articlePreferences.saveArticleIds(id.toString())
        alreadyReadStories.value = articlePreferences.getArticleIds()
    }

    override fun onErrorShown() {
        showableError.value = null
    }

    private fun loadTopStories() {
        launch {
            try {
                stories.value = withContext(Dispatchers.Default) {
                    itemRepository.getTopStories(SIZE_TOP_STORY_ITEMS)
                }
            } catch (e: Exception) {
                showableError.value = e
            }
            progressBarVisibility.value = false
            refreshing.value = false
        }
    }

    class Factory(
        private val articlePreferences: ArticlePreferences,
        private val itemRepository: ItemRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(articlePreferences, itemRepository) as T
        }
    }
}