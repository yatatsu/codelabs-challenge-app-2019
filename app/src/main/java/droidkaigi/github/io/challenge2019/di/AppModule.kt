package droidkaigi.github.io.challenge2019.di

import android.app.Application
import android.content.Context
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.Module
import dagger.Provides
import droidkaigi.github.io.challenge2019.data.api.HackerNewsApi2
import droidkaigi.github.io.challenge2019.data.db.ArticlePreferences
import droidkaigi.github.io.challenge2019.data.repository.ApiItemRepository
import droidkaigi.github.io.challenge2019.domain.BuildConfig
import droidkaigi.github.io.challenge2019.domain.Item
import droidkaigi.github.io.challenge2019.domain.ItemRepository
import droidkaigi.github.io.challenge2019.ingest.IngestManager
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
class AppModule(private val application: Application) {

    @Singleton
    @Provides
    fun context(): Context = application

    @Singleton
    @Provides
    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .apply {
            if (BuildConfig.DEBUG) {
                addNetworkInterceptor(StethoInterceptor())
            }
        }
        .build()

    @Singleton
    @Provides
    fun baseUrl(): HttpUrl = HttpUrl.parse("https://hacker-news.firebaseio.com/v0/")
        ?: throw IllegalStateException("url is illegal")

    @Singleton
    @Provides
    fun hackerNewsApi2(
        okHttpClient: OkHttpClient,
        url: HttpUrl): HackerNewsApi2 =
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(url)
            .addConverterFactory(MoshiConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
            .create(HackerNewsApi2::class.java)

    @Singleton
    @Provides
    fun moshi(): Moshi = Moshi.Builder().build()

    @Singleton
    @Provides
    fun itemJsonAdapter(moshi: Moshi): JsonAdapter<Item> = moshi.adapter(Item::class.java)

    @Singleton
    @Provides
    fun itemsJsonAdapter(moshi: Moshi): JsonAdapter<List<Item?>> =
        moshi.adapter<List<Item?>>(Types.newParameterizedType(List::class.java, Item::class.java))

    @Singleton
    @Provides
    fun articlePreferences(context: Context) = ArticlePreferences(context)

    @Singleton
    @Provides
    fun ingestManager() = IngestManager()

    @Singleton
    @Provides
    fun itemRepository(api: HackerNewsApi2): ItemRepository = ApiItemRepository(api)
}