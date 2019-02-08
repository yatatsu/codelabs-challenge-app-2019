package droidkaigi.github.io.challenge2019

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.squareup.moshi.JsonAdapter
import droidkaigi.github.io.challenge2019.data.db.ArticlePreferences
import droidkaigi.github.io.challenge2019.domain.Item
import droidkaigi.github.io.challenge2019.domain.ItemRepository
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    companion object {
        private const val ACTIVITY_REQUEST = 1
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var storyAdapter: StoryAdapter

    @Inject
    lateinit var itemJsonAdapter: JsonAdapter<Item>
    @Inject
    lateinit var itemRepository: ItemRepository
    @Inject
    lateinit var articlePreferences: ArticlePreferences

    private val viewModel: MainViewModelType by lazy {
        ViewModelProviders.of(
            this, MainViewModel.Factory(articlePreferences, itemRepository)
        ).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appComponent.inject(this)
        recyclerView = findViewById(R.id.item_recycler)
        val progressView : ProgressBar = findViewById(R.id.progress)
        val swipeRefreshLayout : SwipeRefreshLayout = findViewById(R.id.swipe_refresh)

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
                    R.id.refresh -> viewModel.refresh(item)
                }
            },
            alreadyReadStories = viewModel.alreadyReadStories.value ?: setOf()
        )
        recyclerView.adapter = storyAdapter

        swipeRefreshLayout.setOnRefreshListener { viewModel.refreshAll() }

        viewModel.stories.observe(this, Observer { stories ->
            stories?.let {
                progressView.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                storyAdapter.stories = it
                storyAdapter.notifyDataSetChanged()
            }
        })

        viewModel.alreadyReadStories.observe(this, Observer { ids ->
            ids?.let {
                storyAdapter.alreadyReadStories = it
                storyAdapter.notifyDataSetChanged()
            }
        })

        viewModel.showableError.observe(this, Observer { t ->
            t?.let {
                showError(it)
                viewModel.onErrorShown()
            }
        })

        viewModel.progressBarVisibility.observe(this, Observer {
            Util.setVisibility(it ?: false)
        })

        viewModel.refreshing.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it ?: false
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(resultCode) {
            Activity.RESULT_OK -> {
                data?.getLongExtra(StoryActivity.READ_ARTICLE_ID, 0L)?.let { id ->
                    if (id != 0L) {
                        viewModel.markAsRead(id)
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.refresh -> {
                viewModel.refreshAll()
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
}
