package droidkaigi.github.io.challenge2019.data.api

import droidkaigi.github.io.challenge2019.domain.Item
import droidkaigi.github.io.challenge2019.domain.User
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path

interface HackerNewsApi2 {
    @GET("item/{id}.json")
    fun getItem(@Path("id") id: Long): Single<Item>

    @GET("user/{id}.json")
    fun getUser(@Path("id") id: String): Single<User>

    @GET("topstories.json")
    fun getTopStories(): Single<List<Long>>

    @GET("newstories.json")
    fun getNewStories(): Single<List<Long>>

    @GET("jobstories.json")
    fun getJobStories(): Single<List<Long>>
}
