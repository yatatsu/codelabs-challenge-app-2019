package droidkaigi.github.io.challenge2019.data.api

import droidkaigi.github.io.challenge2019.domain.Item
import droidkaigi.github.io.challenge2019.domain.User
import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Path

interface HackerNewsApi2 {
    @GET("item/{id}.json")
    fun getItem(@Path("id") id: Long): Deferred<Item>

    @GET("user/{id}.json")
    fun getUser(@Path("id") id: String): Deferred<User>

    @GET("topstories.json")
    fun getTopStories(): Deferred<List<Long>>

    @GET("newstories.json")
    fun getNewStories(): Deferred<List<Long>>

    @GET("jobstories.json")
    fun getJobStories(): Deferred<List<Long>>
}
