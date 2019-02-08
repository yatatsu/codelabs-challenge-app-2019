package droidkaigi.github.io.challenge2019

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import com.squareup.moshi.JsonAdapter
import droidkaigi.github.io.challenge2019.data.db.ArticlePreferences
import droidkaigi.github.io.challenge2019.domain.Item
import droidkaigi.github.io.challenge2019.domain.ItemRepository
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    companion object {
        private const val STATE_STORIES = "stories"
        private const val ACTIVITY_REQUEST = 1
        private const val SIZE_TOP_STORY_ITEMS = 20
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressView: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var storyAdapter: StoryAdapter

    @Inject
    lateinit var articlePreferences: ArticlePreferences
    @Inject
    lateinit var itemJsonAdapter: JsonAdapter<Item>
    @Inject
    lateinit var itemsJsonAdapter: JsonAdapter<List<Item?>>
    @Inject
    lateinit var itemRepository: ItemRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appComponent.inject(this)
        recyclerView = findViewById(R.id.item_recycler)
        progressView = findViewById(R.id.progress)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh)

        val itemDecoration = DividerItemDecoration(
            recyclerView.context,
            DividerItemDecoration.VERTICAL
        )
        recyclerView.addItemDecoration(itemDecoration)
        storyAdapter = StoryAdapter(
            stories = mutableListOf(),
            onClickItem = { item ->
                val itemJson = itemJsonAdapter.toJson(item)
                val intent = Intent(this@MainActivity, StoryActivity::class.java).apply {
                    putExtra(StoryActivity.EXTRA_ITEM_JSON, itemJson)
                }
                startActivityForResult(intent, ACTIVITY_REQUEST)
            },
            onClickMenuItem = { item, menuItemId ->
                when (menuItemId) {
                    R.id.copy_url -> {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.primaryClip = ClipData.newPlainText("url", item.url)
                    }
                    R.id.refresh -> {
                        loadStory(item)
                    }
                }
            },
            alreadyReadStories = articlePreferences.getArticleIds()
        )
        recyclerView.adapter = storyAdapter

        swipeRefreshLayout.setOnRefreshListener { loadTopStories() }

        val savedStories = savedInstanceState?.let { bundle ->
            bundle.getString(STATE_STORIES)?.let { itemsJson ->
                itemsJsonAdapter.fromJson(itemsJson)
            }
        }

        if (savedStories != null) {
            storyAdapter.stories = savedStories.toMutableList()
            storyAdapter.alreadyReadStories = articlePreferences.getArticleIds()
            storyAdapter.notifyDataSetChanged()
            return
        }

        progressView.visibility = Util.setVisibility(true)
        loadTopStories()
    }

    private fun loadTopStories() {
        launch {
            try {
                val items = withContext(Dispatchers.Default) {
                    itemRepository.getTopStories(SIZE_TOP_STORY_ITEMS)
                }
                progressView.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                storyAdapter.stories = items.toMutableList()
                storyAdapter.alreadyReadStories = articlePreferences.getArticleIds()
                storyAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                showError(e) // TODO: error handling
            }
        }
    }

    private fun loadStory(item: Item) {
        launch {
            try {
                val newItem = withContext(Dispatchers.Default) {
                    itemRepository.getItem(item.id)
                }
                val index = storyAdapter.stories.indexOf(item)
                if (index > 0) {
                    storyAdapter.stories[index] = newItem
                    runOnUiThread {
                        storyAdapter.alreadyReadStories = articlePreferences.getArticleIds()
                        storyAdapter.notifyItemChanged(index)
                    }
                }
            } catch (e: Exception) {
                showError(e) // TODO: error handling
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(resultCode) {
            Activity.RESULT_OK -> {
                data?.getLongExtra(StoryActivity.READ_ARTICLE_ID, 0L)?.let { id ->
                    if (id != 0L) {
                        articlePreferences.saveArticleIds(id.toString())
                        storyAdapter.alreadyReadStories = articlePreferences.getArticleIds()
                        storyAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.refresh -> {
                progressView.visibility = Util.setVisibility(true)
                loadTopStories()
                return true
            }
            R.id.exit -> {
                this.finish()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_menu, menu)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.apply {
            putString(STATE_STORIES, itemsJsonAdapter.toJson(storyAdapter.stories))
        }

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
