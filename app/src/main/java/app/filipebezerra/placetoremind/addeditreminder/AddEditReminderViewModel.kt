package app.filipebezerra.placetoremind.addeditreminder

import android.app.Application
import android.location.Address
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.PointOfInterest
import app.filipebezerra.placetoremind.R
import app.filipebezerra.placetoremind.base.BaseViewModel
import app.filipebezerra.placetoremind.base.NavigationCommand
import app.filipebezerra.placetoremind.data.ReminderDataSource
import app.filipebezerra.placetoremind.data.dto.ReminderDTO
import app.filipebezerra.placetoremind.reminders.ReminderDataItem
import kotlinx.coroutines.launch
import app.filipebezerra.placetoremind.addeditreminder.AddEditReminderFragmentDirections.Companion.actionAddEditReminderFragmentToSelectLocationFragment as selectLocationFragment


class AddEditReminderViewModel(
    val app: Application,
    val dataSource: ReminderDataSource,
) : BaseViewModel(app) {
    private val selectedLocationAddress = MutableLiveData<Address>()

    val reminderTitle = MutableLiveData<String>()
    val reminderDescription = MutableLiveData<String>()
    val reminderSelectedLocationStr: LiveData<String> = selectedLocationAddress.map {
        it.getAddressLine(0)
    }
    val selectedPOI = MutableLiveData<PointOfInterest>()
    val latitude = MutableLiveData<Double>()
    val longitude = MutableLiveData<Double>()

    fun onSaveReminderClicked() {
        val title = reminderTitle.value
        val description = reminderDescription.value
        val location = reminderSelectedLocationStr.value
        val latitude = latitude.value
        val longitude = longitude.value

//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
        ReminderDataItem(
            title,
            description,
            location,
            latitude,
            longitude
        ).run { validateAndSaveReminder(this) }
    }

    fun onSelectLocationClicked() {
        navigationCommand.value = NavigationCommand.To(selectLocationFragment())
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder(reminderData: ReminderDataItem) {
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
        }
    }

    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.id
                )
            )
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            navigationCommand.value = NavigationCommand.Back
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }
        return true
    }

    fun selectLocation(locationAddress: Address) {
        selectedLocationAddress.postValue(locationAddress)
    }
}