package droidkaigi.github.io.challenge2019.domain

interface ItemRepository {

    suspend fun getItem(id: Long): Item

    suspend fun getTopStories(size: Int): List<Item>

    suspend fun getComments(item: Item): List<Item>
}