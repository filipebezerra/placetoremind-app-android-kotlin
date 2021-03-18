package app.filipebezerra.placetoremind.utils.ext

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.filipebezerra.placetoremind.BACKGROUND_LOCATION_PERMISSION_INDEX
import app.filipebezerra.placetoremind.LOCATION_PERMISSION_INDEX
import app.filipebezerra.placetoremind.FOREGROUND_AND_BACKGROUND_PERMISSION_REQUEST_CODE
import app.filipebezerra.placetoremind.FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
import timber.log.Timber


fun Context.isThatPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun IntArray.isLocationPermissionsGranted(requestCodeReceived: Int): Boolean =
    isNotEmpty() &&
            isForegroundLocationPermissionGranted() &&
            isBackgroundLocationPermissionGrantedIfRequested(requestCodeReceived)

fun IntArray.isForegroundLocationPermissionGranted(): Boolean =
    get(LOCATION_PERMISSION_INDEX) == PackageManager.PERMISSION_GRANTED

fun IntArray.isBackgroundLocationPermissionGrantedIfRequested(requestCodeReceived: Int): Boolean =
    requestCodeReceived.takeIf {
        it == FOREGROUND_AND_BACKGROUND_PERMISSION_REQUEST_CODE
    }?.let {
        get(BACKGROUND_LOCATION_PERMISSION_INDEX) == PackageManager.PERMISSION_GRANTED
    } ?: true

@TargetApi(29)
fun Activity.requestForegroundAndBackgroundLocationPermissions(runningQOrLater: Boolean) {
    var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    val requestCode = when {
        runningQOrLater -> {
            Timber.d("Requesting foreground and background location permissions")
            permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
            FOREGROUND_AND_BACKGROUND_PERMISSION_REQUEST_CODE
        }
        else -> {
            Timber.d("Requesting only foreground location permission")
            FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
    }
    ActivityCompat.requestPermissions(
        this,
        permissionsArray,
        requestCode
    )
}
