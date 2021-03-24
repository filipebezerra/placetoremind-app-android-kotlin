package app.filipebezerra.placetoremind.utils.ext

import android.content.Context
import app.filipebezerra.placetoremind.geofence.GeofenceErrorMessages


fun Exception.getHumanReadableErrorMessage(context: Context): String =
    GeofenceErrorMessages.getErrorString(context, this)