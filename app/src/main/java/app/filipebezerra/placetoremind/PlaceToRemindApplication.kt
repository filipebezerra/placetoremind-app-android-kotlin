package app.filipebezerra.placetoremind

import android.app.Application
import app.filipebezerra.placetoremind.authentication.AuthenticationViewModel
import app.filipebezerra.placetoremind.data.ReminderDataSource
import app.filipebezerra.placetoremind.data.local.LocalDB
import app.filipebezerra.placetoremind.data.local.RemindersLocalRepository
import app.filipebezerra.placetoremind.reminders.RemindersListViewModel
import app.filipebezerra.placetoremind.addeditreminder.AddEditReminderViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import timber.log.Timber


class PlaceToRemindApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        /**
         * use Koin Library as a service locator
         */
        val myModule = module {
            //Declare a ViewModel - be later inject into Fragment with dedicated injector using by viewModel()
            viewModel {
                RemindersListViewModel(
                    get(),
                    get() as ReminderDataSource
                )
            }
            viewModel { PlaceToRemindViewModel(get()) }
            viewModel { AuthenticationViewModel(get()) }
            //Declare singleton definitions to be later injected using by inject()
            single {
                //This view model is declared singleton to be used across multiple fragments
                AddEditReminderViewModel(
                    get(),
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(this@PlaceToRemindApplication) }
        }

        startKoin {
            androidContext(this@PlaceToRemindApplication)
            modules(listOf(myModule))
        }
    }
}