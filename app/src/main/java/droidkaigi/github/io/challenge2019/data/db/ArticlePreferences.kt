package droidkaigi.github.io.challenge2019.data.db

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class ArticlePreferences(private val context: Context) {

    private val preferences : SharedPreferences by lazy {  PreferenceManager.getDefaultSharedPreferences(context) }

    companion object {
        private const val ARTICLE_IDS_KEY = "article_ids_key"
    }

    fun saveArticleIds(articleId: String) {
        val data = preferences.getStringSet(ARTICLE_IDS_KEY, mutableSetOf()) ?: mutableSetOf()
        val tmps = mutableSetOf<String>()
        tmps.addAll(data)
        tmps.add(articleId)
        preferences.edit().putStringSet(ARTICLE_IDS_KEY, tmps).apply()
    }

    fun getArticleIds(): Set<String> {
        return preferences.getStringSet(ARTICLE_IDS_KEY, setOf()) ?: setOf()
    }

}