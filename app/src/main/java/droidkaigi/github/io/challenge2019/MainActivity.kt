package droidkaigi.github.io.challenge2019

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import com.squareup.moshi.JsonAdapter
import droidkaigi.github.io.challenge2019.data.api.HackerNewsApi
import droidkaigi.github.io.challenge2019.data.db.ArticlePreferences
import droidkaigi.github.io.challenge2019.domain.Item
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    companion object {
        private const val STATE_STORIES = "stories"
        private const val ACTIVITY_REQUEST = 1
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressView: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var storyAdapter: StoryAdapter

    @Inject
    lateinit var hackerNewsApi: HackerNewsApi
    @Inject
    lateinit var articlePreferences: ArticlePreferences
    @Inject
    lateinit var itemJsonAdapter: JsonAdapter<Item>
    @Inject
    lateinit var itemsJsonAdapter: JsonAdapter<List<Item?>>

    private var getStoriesTask: AsyncTask<Long, Unit, List<Item?>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appComponent.inject(this)
        recyclerView = findViewById(R.id.item_recycler)
        progressView = findViewById(R.id.progress)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh)

        val itemDecoration = DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL)
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
                        hackerNewsApi.getItem(item.id).enqueue(object : Callback<Item> {
                            override fun onResponse(call: Call<Item>, response: Response<Item>) {
                                response.body()?.let { newItem ->
                                    val index = storyAdapter.stories.indexOf(item)
                                    if (index == -1 ) return

                                    storyAdapter.stories[index] = newItem
                                    runOnUiThread {
                                        storyAdapter.alreadyReadStories = articlePreferences.getArticleIds()
                                        storyAdapter.notifyItemChanged(index)
                                    }
                                }
                            }

                            override fun onFailure(call: Call<Item>, t: Throwable) {
                                showError(t)
                            }
                        })
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
        hackerNewsApi.getTopStories().enqueue(object : Callback<List<Long>> {

            override fun onResponse(call: Call<List<Long>>, response: Response<List<Long>>) {
                if (!response.isSuccessful) return

                response.body()?.let { itemIds ->
                    getStoriesTask = @SuppressLint("StaticFieldLeak") object : AsyncTask<Long, Unit, List<Item?>>() {

                        override fun doInBackground(vararg itemIds: Long?): List<Item?> {
                            val ids = itemIds.mapNotNull { it }
                            val itemMap = ConcurrentHashMap<Long, Item?>()
                            val latch = CountDownLatch(ids.size)

                            ids.forEach { id ->
                                hackerNewsApi.getItem(id).enqueue(object : Callback<Item> {
                                    override fun onResponse(call: Call<Item>, response: Response<Item>) {
                                        response.body()?.let { item -> itemMap[id] = item }
                                        latch.countDown()
                                    }

                                    override fun onFailure(call: Call<Item>, t: Throwable) {
                                        showError(t)
                                        latch.countDown()
                                    }
                                })
                            }

                            try {
                                latch.await()
                            } catch (e: InterruptedException) {
                                showError(e)
                                return emptyList()
                            }

                            return ids.map { itemMap[it] }
                        }

                        override fun onPostExecute(items: List<Item?>) {
                            progressView.visibility = View.GONE
                            swipeRefreshLayout.isRefreshing = false
                            storyAdapter.stories = items.toMutableList()
                            storyAdapter.alreadyReadStories = articlePreferences.getArticleIds()
                            storyAdapter.notifyDataSetChanged()
                        }
                    }

                    getStoriesTask?.execute(*itemIds.take(20).toTypedArray())
                }
            }

            override fun onFailure(call: Call<List<Long>>, t: Throwable) {
                showError(t)
            }
        })
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
        getStoriesTask?.run {
            if (!isCancelled) cancel(true)
        }
    }
}
