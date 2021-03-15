package app.filipebezerra.placetoremind.reminderslist

import android.app.Application
import android.view.Menu
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.filipebezerra.placetoremind.R
import app.filipebezerra.placetoremind.base.BaseViewModel
import app.filipebezerra.placetoremind.base.NavigationCommand
import app.filipebezerra.placetoremind.data.ReminderDataSource
import app.filipebezerra.placetoremind.data.dto.ReminderDTO
import app.filipebezerra.placetoremind.data.dto.Result
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import app.filipebezerra.placetoremind.NavGraphDirections.Companion.globalActionAuthenticationFragment as authentication
import app.filipebezerra.placetoremind.reminderslist.ReminderListFragmentDirections.Companion.actionReminderListFragmentToAddEditReminderFragment as addEditReminderFragment

class RemindersListViewModel(
    app: Application,
    private val dataSource: ReminderDataSource
) : BaseViewModel(app) {
    // list that holds the reminder data to be displayed on the UI
    val remindersList = MutableLiveData<List<ReminderDataItem>>()

    private val auth = Firebase.auth

    /**
     * Get all the reminders from the DataSource and add them to the remindersList to be shown on the UI,
     * or show error if any
     */
    fun loadReminders() {
        showLoading.value = true
        viewModelScope.launch {
            //interacting with the dataSource has to be through a coroutine
            val result = dataSource.getReminders()
            showLoading.postValue(false)
            when (result) {
                is Result.Success<*> -> {
                    val dataList = ArrayList<ReminderDataItem>()
                    dataList.addAll((result.data as List<ReminderDTO>).map { reminder ->
                        //map the reminder data from the DB to the be ready to be displayed on the UI
                        ReminderDataItem(
                            reminder.title,
                            reminder.description,
                            reminder.location,
                            reminder.latitude,
                            reminder.longitude,
                            reminder.id
                        )
                    })
                    remindersList.value = dataList
                }
                is Result.Error ->
                    showSnackBar.value = result.message
            }

            //check if no data has to be shown
            invalidateShowNoData()
        }
    }

    fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.logout).isVisible = Firebase.auth.currentUser != null
    }

    fun onAddReminderCliked() {
        auth.currentUser.takeIf { it != null }?.run {
            navigationCommand.postValue(
                NavigationCommand.To(addEditReminderFragment())
            )
        } ?: run {
            navigateToAuthentication()
        }
    }

    fun onLogoutRequested() {
        auth.currentUser.takeIf { it != null }?.run {
            AuthUI.getInstance().signOut(getApplication())
                .addOnSuccessListener {
                    navigateToAuthentication()
                }
        }
    }

    /**
     * Inform the user that there's not any data if the remindersList is empty
     */
    private fun invalidateShowNoData() {
        showNoData.value = remindersList.value == null || remindersList.value!!.isEmpty()
    }

    private fun navigateToAuthentication() {
        navigationCommand.postValue(
            NavigationCommand.To(authentication())
        )
    }
}