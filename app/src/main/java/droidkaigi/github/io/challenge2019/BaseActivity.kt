package droidkaigi.github.io.challenge2019

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

abstract class BaseActivity : AppCompatActivity() {

    companion object {
        const val ACTIVITY_REQUEST = 1
    }

    abstract fun getContentView(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getContentView())
    }

    fun showError(throwable: Throwable) {
        Log.v("error", throwable.message)
        Toast.makeText(baseContext, throwable.message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.exit -> {
                this.finish()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun startActivityForResult(intent: Intent?) {
        intent?.let { intent2 ->
            startActivityForResult(intent2, ACTIVITY_REQUEST)
        }
    }
}