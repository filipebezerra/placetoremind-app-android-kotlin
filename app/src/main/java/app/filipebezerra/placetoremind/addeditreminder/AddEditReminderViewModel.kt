package app.filipebezerra.placetoremind.addeditreminder

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import app.filipebezerra.placetoremind.R
import app.filipebezerra.placetoremind.base.BaseViewModel
import app.filipebezerra.placetoremind.base.NavigationCommand
import app.filipebezerra.placetoremind.data.ReminderDataSource
import app.filipebezerra.placetoremind.data.dto.ReminderDTO
import app.filipebezerra.placetoremind.geofence.GeofenceBroadcastReceiver
import app.filipebezerra.placetoremind.reminders.ReminderDataItem
import app.filipebezerra.placetoremind.utils.ext.getHumanReadableErrorMessage
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.PointOfInterest
import kotlinx.coroutines.launch
import timber.log.Timber
import app.filipebezerra.placetoremind.addeditreminder.AddEditReminderFragmentDirections.Companion.actionAddEditReminderFragmentToSelectLocationFragment as selectLocationFragment


class AddEditReminderViewModel(
    val app: Application,
    private val dataSource: ReminderDataSource,
) : BaseViewModel(app) {
    private val selectedLocationAddress = MutableLiveData<Address>()

    val reminderTitle = MutableLiveData<String>()
    val reminderDescription = MutableLiveData<String>()
    val reminderSelectedLocationStr: LiveData<String> = selectedLocationAddress.map {
        it.getAddressLine(0)
    }
    val selectedPOI = MutableLiveData<PointOfInterest>()

    private val geofencingClient by lazy { LocationServices.getGeofencingClient(app) }
    private val geofencePendingIntent by lazy {
        val intent = Intent(app, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            app,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun onSelectLocationClicked() {
        navigationCommand.value = NavigationCommand.To(selectLocationFragment())
    }

    fun selectLocation(locationAddress: Address) {
        selectedLocationAddress.postValue(locationAddress)
    }

    fun onSaveReminderClicked() {
        val reminder = buildReminder()
        if (validateEnteredData(reminder)) {
            val geofence = buildGeofence(reminder)
            if (geofence != null && checkLocationPermission()) {
                geofencingClient.addGeofences(buildGeofencingRequest(geofence),
                    geofencePendingIntent)
                    .addOnSuccessListener {
                        Timber.d("Geofence added successfully")
                        saveReminder(reminder)
                    }
                    .addOnFailureListener {
                        Timber.e(it, "Failed to add Geofence")
                        showToast.value = it.getHumanReadableErrorMessage(app)
                    }
            }
        }
    }

    private fun buildReminder() = ReminderDataItem(
        reminderTitle.value,
        reminderDescription.value,
        reminderSelectedLocationStr.value,
        selectedLocationAddress.value?.latitude,
        selectedLocationAddress.value?.longitude
    )

    private fun buildGeofence(reminder: ReminderDataItem): Geofence? {
        val latitude = reminder.latitude
        val longitude = reminder.longitude
        if (latitude != null && longitude != null) {
            return Geofence.Builder()
                .setRequestId(reminder.id)
                .setCircularRegion(
                    reminder.latitude!!,
                    reminder.longitude!!,
                    150f
                )
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
        }
        return null
    }

    private fun checkLocationPermission() = ContextCompat.checkSelfPermission(
        app,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun buildGeofencingRequest(geofence: Geofence) = GeofencingRequest.Builder()
        .addGeofence(geofence)
        // The notification behavior. It's a bit-wise of GeofencingRequest.INITIAL_TRIGGER_ENTER
        // and/or GeofencingRequest.INITIAL_TRIGGER_EXIT and/or GeofencingRequest.INITIAL_TRIGGER_DWELL.
        // When initialTrigger is set to 0 (setInitialTrigger(0)), initial trigger would be disabled.
        .setInitialTrigger(0)
        .build()

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    private fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
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

    /**
     * Save the reminder to the data source
     */
    private fun saveReminder(reminderData: ReminderDataItem) {
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
}