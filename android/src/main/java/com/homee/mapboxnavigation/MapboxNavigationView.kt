package com.homee.mapboxnavigation

import android.location.Location
import com.facebook.react.bridge.Arguments
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.mapbox.api.directions.v5.models.DirectionsRoute

import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.NavigationView
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.OnNavigationReadyCallback
import com.mapbox.navigation.ui.listeners.NavigationListener
// ToDo Whats the difference between those two?
import com.mapbox.navigation.core.MapboxNavigation


class MapboxNavigationView(private val context: ThemedReactContext) : NavigationView(context.baseContext), NavigationListener, OnNavigationReadyCallback {
    private var origin: Point? = null
    private var destination: Point? = null
    private var shouldSimulateRoute = false
    private var routes: List<DirectionsRoute>? = null
    private var mapboxNavigation: MapboxNavigation? = null

    private var lastLocation: Location? = null

    init {
        onCreate(null)
        onResume()
        initialize(this, getInitialCameraPosition())
    }

    override fun requestLayout() {
        super.requestLayout()

        // This view relies on a measure + layout pass happening after it calls requestLayout().
        // https://github.com/facebook/react-native/issues/4990#issuecomment-180415510
        // https://stackoverflow.com/questions/39836356/react-native-resize-custom-ui-component
        post(measureAndLayout)
    }

    private val measureAndLayout = Runnable {
        measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
        layout(left, top, right, bottom)
    }

    private fun getInitialCameraPosition(): CameraPosition {
        return CameraPosition.Builder()
                .zoom(15.0)
                .build()
    }

    override fun onNavigationReady(isRunning: Boolean) {
        try {
            val accessToken = Mapbox.getAccessToken()
            if (accessToken == null) {
                sendErrorToReact("Mapbox access token is not set")
                return
            }

            if (origin == null || destination == null) {
                sendErrorToReact("origin and destination are required")
                return
            }

            val routes = this.routes
            if (routes != null) {
                startNav(routes[0])
            } else {
                throw Exception("Route not accepted")
            }
        } catch (ex: Exception) {
            sendErrorToReact(ex.toString())

            for(it in ex.getStackTrace()){
                sendErrorToReact(it.toString())
            }
        }
    }

    private fun startNav(route: DirectionsRoute) {
        val optionsBuilder = NavigationViewOptions.builder(this.getContext())
        optionsBuilder.navigationListener(this)
        optionsBuilder.locationObserver(locationObserver)
        optionsBuilder.routeProgressObserver(routeProgressObserver)
        optionsBuilder.directionsRoute(route)
        optionsBuilder.shouldSimulateRoute(this.shouldSimulateRoute)
        optionsBuilder.waynameChipEnabled(true)
        val navigation = optionsBuilder.build();
        this.startNavigation(navigation)
    }

    public fun updateRoute(routes: List<DirectionsRoute>?) {
        if (routes != null) {
            this.retrieveMapboxNavigation()?.setRoutes(routes);
        }
    }

    private val offRouteObserver = object : OffRouteObserver {
        override fun onOffRouteStateChanged(offRoute: Boolean) {
            val event = Arguments.createMap()
            event.putBoolean("offRoute", offRoute)
            if (lastLocation != null) {
                event.putDouble("longitude", lastLocation!!.longitude)
                event.putDouble("latitude", lastLocation!!.latitude)
            }
            context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, "onUserOffRoute", event)
        }
    }

    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {

        }

        override fun onEnhancedLocationChanged(
                enhancedLocation: Location,
                keyPoints: List<Location>
        ) {
            lastLocation = enhancedLocation;
            val event = Arguments.createMap()
            event.putDouble("longitude", enhancedLocation.longitude)
            event.putDouble("latitude", enhancedLocation.latitude)
            context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, "onLocationChange", event)
        }
    }


    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            val event = Arguments.createMap()
            event.putDouble("distanceTraveled", routeProgress.distanceTraveled.toDouble())
            event.putDouble("durationRemaining", routeProgress.durationRemaining.toDouble())
            event.putDouble("fractionTraveled", routeProgress.fractionTraveled.toDouble())
            event.putDouble("distanceRemaining", routeProgress.distanceRemaining.toDouble())
            context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, "onRouteProgressChange", event)
        }
    }


    private fun sendErrorToReact(error: String?) {
        val event = Arguments.createMap()
        event.putString("error", error)
        context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, "onError", event)
    }

    override fun onNavigationRunning() {
        this.mapboxNavigation = this.retrieveMapboxNavigation()
        this.mapboxNavigation?.registerOffRouteObserver(offRouteObserver)
    }

    override fun onFinalDestinationArrival(enableDetailedFeedbackFlowAfterTbt: Boolean, enableArrivalExperienceFeedback: Boolean) {
        super.onFinalDestinationArrival(enableDetailedFeedbackFlowAfterTbt, enableArrivalExperienceFeedback)
        val event = Arguments.createMap()
        event.putString("onArrive", "")
        context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, "onArrive", event)
    }

    override fun onNavigationFinished() {

    }

    override fun onCancelNavigation() {
        val event = Arguments.createMap()
        event.putString("onCancelNavigation", "Navigation Closed")
        context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, "onCancelNavigation", event)
    }

    override fun onDestroy() {
        this.stopNavigation()
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
    }

    fun setOrigin(origin: Point?) {
        this.origin = origin
    }

    fun setDestination(destination: Point?) {
        this.destination = destination
    }

    fun setRoutes(routes: List<DirectionsRoute>?) {
        this.routes = routes
    }

    fun setShouldSimulateRoute(shouldSimulateRoute: Boolean) {
        this.shouldSimulateRoute = shouldSimulateRoute
    }

    fun onDropViewInstance() {
        this.onDestroy()
    }
}