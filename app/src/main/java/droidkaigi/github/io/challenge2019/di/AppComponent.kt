package droidkaigi.github.io.challenge2019.di

import dagger.Component
import droidkaigi.github.io.challenge2019.MainActivity
import droidkaigi.github.io.challenge2019.StoryActivity
import javax.inject.Singleton

@Singleton
@Component(
    modules = [AppModule::class]
)
interface AppComponent {

    fun inject(activity: MainActivity)

    fun inject(activity: StoryActivity)
}