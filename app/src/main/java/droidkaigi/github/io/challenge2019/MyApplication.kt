package droidkaigi.github.io.challenge2019

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.facebook.stetho.Stetho
import droidkaigi.github.io.challenge2019.di.AppComponent
import droidkaigi.github.io.challenge2019.di.AppModule
import droidkaigi.github.io.challenge2019.di.DaggerAppComponent
import timber.log.Timber
import timber.log.Timber.DebugTree


class MyApplication : Application() {

    val appComponent: AppComponent by lazy { createComponent() }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
            Stetho.initializeWithDefaults(this)
        }
    }

    @VisibleForTesting
    fun createComponent(): AppComponent =
        DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
}


val Context.appComponent: AppComponent
    get() = (applicationContext as MyApplication).appComponent