package droidkaigi.github.io.challenge2019

import android.app.Activity
import android.view.View
import android.widget.Toast
import timber.log.Timber

class Util {
    companion object {
        fun setVisibility(isVisible: Boolean): Int {
            return if (isVisible) View.VISIBLE else View.GONE
        }
    }
}

fun Activity.showError(throwable: Throwable) {
    Timber.w(throwable.message, null)
    Toast.makeText(baseContext, throwable.message, Toast.LENGTH_SHORT).show()
}