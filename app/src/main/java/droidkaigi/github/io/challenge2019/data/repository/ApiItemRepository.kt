package droidkaigi.github.io.challenge2019.data.repository

import droidkaigi.github.io.challenge2019.data.api.HackerNewsApi2
import droidkaigi.github.io.challenge2019.domain.Item
import droidkaigi.github.io.challenge2019.domain.ItemRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

class ApiItemRepository(
    private val api: HackerNewsApi2
) : ItemRepository {

    override suspend fun getItem(id: Long): Item {
        return api.getItem(id).await()
    }

    override suspend fun getTopStories(size: Int): List<Item> {
        val ids = api.getTopStories().await().take(size)
        return runBlocking {
            ids.map { async { api.getItem(it) } }.map { it.await() }
        }.awaitAll()
    }

    override suspend fun getComments(item: Item): List<Item> {
        return runBlocking {
            item.kids.map { async { api.getItem(it) } }.map { it.await() }
        }.awaitAll()
    }
}