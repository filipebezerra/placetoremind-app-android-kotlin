package app.filipebezerra.placetoremind.addeditreminder.selectreminderlocation


/**
 *
 */
enum class LocationRequestFlow {
    /**
     * No current state.
     */
    NONE,

    /**
     * When tried to request location updates and failed.
     */
    PENDING_REQUEST_LOCATION_UPDATES,
    /**
     * When was requesting location updates but tried to remove location updates and failed.
     */
    PENDING_REMOVE_LOCATION_UPDATES,

    /**
     * When device location is disabled and was requested to enable to the user.
     */
    REQUESTING_DEVICE_LOCATION_ON,

    /**
     * When Location permissions wasn't granted and was requested to the user.
     */
    REQUESTING_PERMISSIONS,

    /**
     * When device location in unknown or
     */
    REQUESTING_LOCATION_UPDATE
}