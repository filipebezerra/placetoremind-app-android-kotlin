package app.filipebezerra.placetoremind.utils.ext

import android.content.res.Resources
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun Bitmap.asBitmapDescriptor(): BitmapDescriptor =
    BitmapDescriptorFactory.fromBitmap(this)

fun Resources.drawableAsBitmap(@DrawableRes drawableId: Int): Bitmap? =
    ResourcesCompat.getDrawable(this, drawableId, null)?.toBitmap()