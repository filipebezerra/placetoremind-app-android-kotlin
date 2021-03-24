package app.filipebezerra.placetoremind.addeditreminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import app.filipebezerra.placetoremind.*
import app.filipebezerra.placetoremind.R
import app.filipebezerra.placetoremind.addeditreminder.AddEditReminderViewModel
import app.filipebezerra.placetoremind.base.BaseFragment
import app.filipebezerra.placetoremind.databinding.SelectLocationFragmentBinding
import app.filipebezerra.placetoremind.utils.ext.*
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.*
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.IOException


class SelectLocationFragment : BaseFragment() {

    override val _viewModel: AddEditReminderViewModel by inject()

    private lateinit var binding: SelectLocationFragmentBinding

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var googleMap: GoogleMap

    private lateinit var currentLocation: Location

    private var locationRequest: LocationRequest? = null

    private var locationCallback: LocationCallback? = null

    private var currentMarker: Marker? = null

    private var currentGroundOverlay: GroundOverlay? = null

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private val navController: NavController by lazy { findNavController() }

    private var locationRequestFlow = LocationRequestFlow.NONE

    private val mapReadyCallback = OnMapReadyCallback { map ->
        Timber.d("Google map is ready")
        googleMap = map
        googleMap.apply {
            mapType = GoogleMap.MAP_TYPE_SATELLITE
            tryStyleMap()
            setOnMapClickListener { latLng -> latLng.showInMap() }
        }
        showCurrentDeviceLocationOnMap()
    }

    private fun GoogleMap.tryStyleMap() {
        // https://mapstyle.withgoogle.com
        context?.let {
            try {
                setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        it,
                        R.raw.map_style
                    )
                ).also { success ->
                    if (!success) {
                        Timber.e("Map style parsing failed")
                    }
                }
            } catch (error: Resources.NotFoundException) {
                Timber.e(error, "Can't find map style resource")
            }
        }
    }

    private fun LatLng.showInMap() {
        if (currentMarker != null) {
            currentMarker?.let { marker ->
                Timber.d("Changing current marker to ${toString()}")
                marker.position = this
                marker.isInfoWindowShown.takeIf { it.not() }?.run { marker.showInfoWindow() }
                createGroundOverlay()
            }
        } else {
            Timber.d("Showing marker at ${toString()}")
            googleMap.addMarker(
                MarkerOptions().position(this)
                    .title(getString(R.string.my_location))
                    .snippet(getString(R.string.remind_me_here))
                    .draggable(true)
            ).apply {
                showInfoWindow()
                createGroundOverlay()
            }.also { currentMarker = it }
        }
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(this, 16F))
    }

    private fun LatLng.createGroundOverlay() {
        if (currentGroundOverlay != null) {
            currentGroundOverlay?.let { overlay ->
                Timber.d("Changing current ground overlay to ${toString()}")
                overlay.position = this
            }
        } else {
            googleMap.addGroundOverlay(
                GroundOverlayOptions()
                    .image(resources.drawableAsBitmap(R.mipmap.ic_launcher)?.asBitmapDescriptor()!!)
                    .position(this, 48f)
            ).also { currentGroundOverlay = it }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = SelectLocationFragmentBinding.inflate(inflater, container, false)
        .apply {
            binding = this
            setHasOptionsMenu(true)
        }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)?.run {
            this.getMapAsync(mapReadyCallback)
        }
        activity?.let { fragmentActivity ->
            fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(fragmentActivity)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
//        menu.setGroupVisible(R.id., ::googleMap.isInitialized)
        menu.findItem(R.id.normal_map).isEnabled = ::googleMap.isInitialized
        menu.findItem(R.id.hybrid_map).isEnabled = ::googleMap.isInitialized
        menu.findItem(R.id.satellite_map).isEnabled = ::googleMap.isInitialized
        menu.findItem(R.id.terrain_map).isEnabled = ::googleMap.isInitialized
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map,
        R.id.hybrid_map,
        R.id.satellite_map,
        R.id.terrain_map,
        -> {
            with(item) {
                itemId.takeIf { it == R.id.normal_map }?.let { googleMap.mapType = MAP_TYPE_NORMAL }
                itemId.takeIf { it == R.id.hybrid_map }?.let { googleMap.mapType = MAP_TYPE_HYBRID }
                itemId.takeIf { it == R.id.satellite_map }
                    ?.let { googleMap.mapType = MAP_TYPE_SATELLITE }
                itemId.takeIf { it == R.id.terrain_map }
                    ?.let { googleMap.mapType = MAP_TYPE_TERRAIN }
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndStartShowingLocationOnMap()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    /*
    *  When we get the result from asking the user to turn on device location, we call
    *  checkDeviceLocationSettingsAndStartShowingLocationOnMap() again to make sure it's actually on, but
    *  we don't resolve the check to keep the user from seeing an endless loop.
    */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            // We don't rely on the result code, but just check the location setting again
            TURN_DEVICE_LOCATION_ON_REQUEST_CODE -> {
                Timber.d("onActivityResult() resulted REQUEST_TURN_DEVICE_LOCATION_ON")
                locationRequestFlow = LocationRequestFlow.NONE
                checkDeviceLocationSettingsAndStartShowingLocationOnMap()
            }
            GRANT_LOCATION_PERMISSIONS_APP_DETAILS_SETTINGS_REQUEST_CODE -> {
                Timber.d("""onActivityResult() resulted 
                    |GRANT_LOCATION_PERMISSIONS_ON_APP_DETAILS_SETTINGS_REQUEST_CODE""".trimMargin())
                locationRequestFlow = LocationRequestFlow.NONE
                checkPermissionsAndStartShowingLocationOnMap()
            }
            TURN_DEVICE_LOCATION_ON_LOCATION_SOURCE_SETTINGS_REQUEST_CODE -> {
                Timber.d("""onActivityResult() resulted 
                    |TURN_DEVICE_LOCATION_ON_LOCATION_SOURCE_SETTINGS_REQUEST_CODE""".trimMargin())
                locationRequestFlow = LocationRequestFlow.NONE
                checkDeviceLocationSettingsAndStartShowingLocationOnMap()
            }
        }
    }

    /*
     * In all cases, we need to have the location permission.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        Timber.d(
            "onRequestPermissionsResult() resulted %s for permissions %s",
            grantResults.joinToString(
                prefix = "[", postfix = "]"
            ) {
                if (it == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"
            },
            permissions.joinToString(prefix = "[", postfix = "]")
        )
        when(requestCode) {
            FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE,
            FOREGROUND_AND_BACKGROUND_PERMISSION_REQUEST_CODE -> {
                locationRequestFlow.takeIf { it == LocationRequestFlow.REQUESTING_PERMISSIONS }
                    ?.run {
                        locationRequestFlow = LocationRequestFlow.NONE
                        grantResults.isLocationPermissionsGranted(requestCode).takeIf { it }
                            ?.run {
                                Timber.d("Location permission was granted")
                                checkDeviceLocationSettingsAndStartShowingLocationOnMap()
                            } ?: run {
                                Timber.d("Location permission was denied")
                                Snackbar.make(
                                    binding.selectLocationRootLayout,
                                    getString(R.string.permission_denied_explanation),
                                    Snackbar.LENGTH_INDEFINITE
                                ).setAction(R.string.settings) {
                                    locationRequestFlow = LocationRequestFlow.REQUESTING_PERMISSIONS
                                    startActivityForResult(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts(
                                                "package",
                                                BuildConfig.APPLICATION_ID,
                                                null
                                            )
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        },
                                        GRANT_LOCATION_PERMISSIONS_APP_DETAILS_SETTINGS_REQUEST_CODE
                                    )
                                }.show()
                        }
                    } ?: run {
                        Timber.d("""isLocationPermissionsGranted() check not executed. 
                            |Reason: requestCode was FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE or 
                            |FOREGROUND_AND_BACKGROUND_PERMISSION_REQUEST_CODE but 
                            |LocationRequestFlow wasn't REQUESTING_PERMISSIONS""".trimMargin())
                    }
            }
        }
    }

    /**
     * Starts the permission check and start showing the device location on the map.
     */
    private fun checkPermissionsAndStartShowingLocationOnMap() {
        when (foregroundAndBackgroundLocationPermissionApproved()) {
            true -> {
                Timber.d("Checking location permission and it's already approved")
                checkDeviceLocationSettingsAndStartShowingLocationOnMap()
            }
            false -> {
                Timber.d("Checking location permission and isn't approved")
                checkAndRequestForegroundAndBackgroundLocationPermissionsIfNeeded()
            }
        }
    }

    /*
     *  Determines whether the app has the appropriate permissions across Android 10+ and all other
     *  Android versions.
     */
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved =
            context?.isThatPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ?: false
        val backgroundLocationApproved = runningQOrLater.takeIf { it }?.run {
            context?.isThatPermissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } ?: true
        Timber.d("Foreground and background permissions are granted: %b",
            "${foregroundLocationApproved && backgroundLocationApproved}")
        return foregroundLocationApproved && backgroundLocationApproved
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettingsAndStartShowingLocationOnMap(resolve: Boolean = true) {
        locationRequestFlow.takeUnless { it == LocationRequestFlow.REQUESTING_DEVICE_LOCATION_ON }
            ?.run {
                activity?.let { fragmentActivity ->
                    createOrGetLocationRequest().let {
                        LocationSettingsRequest.Builder()
                            .addLocationRequest(it)
                            .build()
                    }.run {
                        locationRequestFlow = LocationRequestFlow.REQUESTING_DEVICE_LOCATION_ON
                        LocationServices.getSettingsClient(fragmentActivity)
                            .checkLocationSettings(this).apply {
                                addOnFailureListener { exception ->
                                    Timber.d("Failed to check location settings")
                                    if (exception is ResolvableApiException && resolve) {
                                        Timber.d("Check location settings failure is resolvable")
                                        try {
                                            exception.startResolutionForResult(
                                                fragmentActivity,
                                                TURN_DEVICE_LOCATION_ON_REQUEST_CODE
                                            )
                                        } catch (sendException: IntentSender.SendIntentException) {
                                            Timber.e(
                                                sendException,
                                                "Error getting location settings resolution"
                                            )
                                            Snackbar.make(
                                                binding.selectLocationRootLayout,
                                                R.string.fail_to_check_device_location_settings,
                                                Snackbar.LENGTH_INDEFINITE
                                            ).setAction(R.string.let_user_try_to_fix) {
                                                startActivityForResult(
                                                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                                                    TURN_DEVICE_LOCATION_ON_LOCATION_SOURCE_SETTINGS_REQUEST_CODE
                                                )
                                            }.show()
                                        }
                                    } else {
                                        Timber.d("Check location settings failure couldn't be resolved")
                                        locationRequestFlow = LocationRequestFlow.NONE
                                        Snackbar.make(
                                            binding.selectLocationRootLayout,
                                            R.string.location_required_error,
                                            Snackbar.LENGTH_INDEFINITE
                                        ).setAction(R.string.try_again) {
                                            checkDeviceLocationSettingsAndStartShowingLocationOnMap()
                                        }.show()
                                    }
                                }
                                addOnSuccessListener {
                                    Timber.d("Succeed to check location settings")
                                    locationRequestFlow = LocationRequestFlow.NONE
                                    getCurrentDeviceLocation()
                                }
                            }
                    }
                }
            } ?: run {
                Timber.d("""checkLocationSettings() not executed. 
                            |Reason: LocationRequestFlow was already REQUESTING_DEVICE_LOCATION_ON""".trimMargin())
            }
    }

    private fun createOrGetLocationRequest(): LocationRequest =
        locationRequest ?: LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = LOCATION_REQUEST_INTERVAL
            fastestInterval = LOCATION_REQUEST_FASTEST_INTERVAL
        }.also { locationRequest = it }

    /*
     *  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    private fun checkAndRequestForegroundAndBackgroundLocationPermissionsIfNeeded() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        locationRequestFlow.takeUnless {
            (it == LocationRequestFlow.REQUESTING_PERMISSIONS) or
                    (it == LocationRequestFlow.REQUESTING_DEVICE_LOCATION_ON)
        }?.run {
            Timber.d("Requesting location permissions")
            locationRequestFlow = LocationRequestFlow.REQUESTING_PERMISSIONS
            requestForegroundAndBackgroundLocationPermissions(runningQOrLater)
        } ?: run {
            Timber.d("""requestForegroundAndBackgroundLocationPermissions() not executed. 
                        |Reason: LocationRequestFlow was already REQUESTING_PERMISSIONS 
                        |or REQUESTING_DEVICE_LOCATION_ON""".trimMargin())
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentDeviceLocation() {
        if (
            ::fusedLocationProviderClient.isInitialized && foregroundAndBackgroundLocationPermissionApproved()
        ) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                Timber.d("Succeed to get last location")
                // Also checks accuracy/time of the last known location
                if (it != null && it.checkForMostNewerAndAccurateLocation()) {
                    Timber.d("Last location was satisfied")
                    currentLocation = it
                    showCurrentDeviceLocationOnMap()
                } else {
                    Timber.d("No Last location known")
                    startLocationUpdates()
                }
            }
        }
    }

    private fun Location.checkForMostNewerAndAccurateLocation(): Boolean {
        val timeDeltaComparedToSystemBoot = SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos
        val isSignificantlyNewer = timeDeltaComparedToSystemBoot > TWO_MINUTES
        val isSignificantlyOlder = timeDeltaComparedToSystemBoot < -TWO_MINUTES
        val isNewer = timeDeltaComparedToSystemBoot > 0

        isSignificantlyNewer.takeIf { it }?.run { return true }
        isSignificantlyOlder.takeIf { it }?.run { return false }

        val isSignificantlyLessAccurate = accuracy > 200
        takeIf { isNewer and !isSignificantlyLessAccurate }?.run { return true }
        return false
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationRequestFlow.takeUnless { it == LocationRequestFlow.REQUESTING_LOCATION_UPDATE }
            ?.run {
                if (::fusedLocationProviderClient.isInitialized) {
                    Timber.d("Requesting location updates")
                    fusedLocationProviderClient.requestLocationUpdates(
                        createOrGetLocationRequest(),
                        createOrGetLocationCallback(),
                        Looper.getMainLooper()
                    ).apply {
                        addOnSuccessListener {
                            locationRequestFlow = LocationRequestFlow.REQUESTING_LOCATION_UPDATE
                        }
                        addOnFailureListener {
                            Timber.e(it, "Failed to request location updates")
                            locationRequestFlow = LocationRequestFlow.PENDING_REQUEST_LOCATION_UPDATES
                        }
                    }
                }
            } ?: run {
                Timber.d("""startLocationUpdates() not executed. 
                        |Reason: LocationRequestFlow was already REQUESTING_LOCATION_UPDATE""".trimMargin())
            }
    }

    private fun createOrGetLocationCallback(): LocationCallback =
        locationCallback ?: object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (locationResult != null && locationResult.lastLocation != null) {
                    Timber.d("Succeed to get last location from location updates")
                    stopLocationUpdates()
                    currentLocation = locationResult.lastLocation
                    showCurrentDeviceLocationOnMap()
                } else {
                    Timber.d("No Last location known from location updates")
                }
            }
        }.also { locationCallback = it }

    private fun stopLocationUpdates() {
        locationRequestFlow.takeIf { it == LocationRequestFlow.REQUESTING_LOCATION_UPDATE }
            ?.run {
                Timber.d("Stopping requesting location updates")
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                    .apply {
                        addOnCompleteListener {
                            locationRequestFlow = LocationRequestFlow.NONE
                            locationCallback = null
                            it.takeUnless { it.isSuccessful }?.run {
                                Timber.e(it.exception, "Failed to remove location updates")
                                locationRequestFlow = LocationRequestFlow.PENDING_REMOVE_LOCATION_UPDATES
                            }
                        }
                    }
            } ?: run {
                Timber.d("""stopLocationUpdates() not executed. 
                    |Reason: LocationRequestFlow wasn't REQUESTING_LOCATION_UPDATE""".trimMargin())
            }
    }

    @SuppressLint("MissingPermission")
    private fun showCurrentDeviceLocationOnMap() {
        if (::googleMap.isInitialized && ::currentLocation.isInitialized) {
            Timber.d("Google Maps and device location were initialized")
            googleMap.apply {
                isMyLocationEnabled = true
                currentLocation.toLatLng().let { latLng ->
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        latLng,
                        16F
                    ))
                    latLng.showInMap()
                }
                binding.selectLocationFab.setOnClickListener { onSelectLocationClicked() }
                this@SelectLocationFragment.activity?.invalidateOptionsMenu()
            }
        } else {
            when {
                !::googleMap.isInitialized && !::currentLocation.isInitialized ->
                    Timber.d("Both Google Maps and device location weren't initialized")
                !::googleMap.isInitialized ->
                    Timber.d("Google Maps wasn't initialized")
                !::currentLocation.isInitialized ->
                    Timber.d("Device location wasn't initialized")
            }
        }
    }

    private fun onSelectLocationClicked() {
        currentMarker?.let { marker ->
            val latLng = marker.position
            lifecycleScope.launchWhenResumed {
                withContext(Dispatchers.IO) {
                    try {
                        Geocoder(requireContext()).getFromLocation(
                            latLng.latitude,
                            latLng.longitude,
                            1
                        ).firstOrNull()?.let { address ->
                            _viewModel.selectLocation(address)
                            navController.popBackStack()
                        }
                    } catch (ioException: IOException) {
                        Timber.e(ioException, "Calling Geocoder to get first address")
                    }
                }
            }
        }
    }
}
