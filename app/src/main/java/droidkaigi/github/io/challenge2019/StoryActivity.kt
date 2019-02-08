package droidkaigi.github.io.challenge2019

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import com.squareup.moshi.JsonAdapter
import droidkaigi.github.io.challenge2019.data.api.HackerNewsApi
import droidkaigi.github.io.challenge2019.domain.Item
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

class StoryActivity : BaseActivity() {

    companion object {
        const val EXTRA_ITEM_JSON = "droidkaigi.github.io.challenge2019.EXTRA_ITEM_JSON"
        const val READ_ARTICLE_ID = "read_article_id"
        private const val STATE_COMMENTS = "comments"
    }

    private lateinit var webView: WebView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressView: ProgressBar

    private lateinit var commentAdapter: CommentAdapter

    @Inject
    lateinit var hackerNewsApi: HackerNewsApi
    @Inject
    lateinit var itemJsonAdapter: JsonAdapter<Item>
    @Inject
    lateinit var itemsJsonAdapter: JsonAdapter<List<Item?>>

    private var getCommentsTask: AsyncTask<Long, Unit, List<Item?>>? = null
    private var hideProgressTask: AsyncTask<Unit, Unit, Unit>? = null

    private var item: Item? = null

    override fun getContentView(): Int {
        return R.layout.activity_story
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        webView = findViewById(R.id.web_view)
        recyclerView = findViewById(R.id.comment_recycler)
        progressView = findViewById(R.id.progress)

        item = intent.getStringExtra(EXTRA_ITEM_JSON)?.let {
            itemJsonAdapter.fromJson(it)
        }

        recyclerView.isNestedScrollingEnabled = false
        val itemDecoration = DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)
        commentAdapter = CommentAdapter(emptyList())
        recyclerView.adapter = commentAdapter

        if (item == null) return

        val savedComments = savedInstanceState?.let { bundle ->
            bundle.getString(STATE_COMMENTS)?.let { itemsJson ->
                itemsJsonAdapter.fromJson(itemsJson)
            }
        }

        if (savedComments != null) {
            commentAdapter.comments = savedComments
            commentAdapter.notifyDataSetChanged()
            webView.loadUrl(item!!.url)
            return
        }

        progressView.visibility = View.VISIBLE
        loadUrlAndComments()
    }

    private fun loadUrlAndComments() {
        if (item == null) return

        val progressLatch = CountDownLatch(2)

        hideProgressTask = @SuppressLint("StaticFieldLeak") object : AsyncTask<Unit, Unit, Unit>() {

            override fun doInBackground(vararg unit: Unit?) {
                try {
                    progressLatch.await()
                } catch (e: InterruptedException) {
                    showError(e)
                }
            }

            override fun onPostExecute(result: Unit?) {
                progressView.visibility = Util.setVisibility(false)
            }
        }

        hideProgressTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                progressLatch.countDown()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                progressLatch.countDown()
            }
        }
        webView.loadUrl(item!!.url)

        getCommentsTask = @SuppressLint("StaticFieldLeak") object : AsyncTask<Long, Unit, List<Item?>>() {
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
                            latch.countDown()
                            showError(t)
                        }
                    })
                }

                try {
                    latch.await()
                } catch (e: InterruptedException) {
                    return emptyList()
                }

                return ids.map { itemMap[it] }
            }

            override fun onPostExecute(items: List<Item?>) {
                progressLatch.countDown()
                commentAdapter.comments = items
                commentAdapter.notifyDataSetChanged()
            }
        }

        getCommentsTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, *item!!.kids.toTypedArray())
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when(item?.itemId) {
            R.id.refresh -> {
                progressView.visibility = Util.setVisibility(true)
                loadUrlAndComments()
                return true
            }
            android.R.id.home -> {
                val intent = Intent().apply {
                    putExtra(READ_ARTICLE_ID, this@StoryActivity.item?.id)
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.apply {
            putString(STATE_COMMENTS, itemsJsonAdapter.toJson(commentAdapter.comments))
        }

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        getCommentsTask?.run {
            if (!isCancelled) cancel(true)
        }
        hideProgressTask?.run {
            if (!isCancelled) cancel(true)
        }
    }
}
