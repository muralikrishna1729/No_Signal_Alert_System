package com.example.nosignalalertsystem.ui.map
import com.example.nosignalalertsystem.geofence.GeofenceBroadcastReceiver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.nosignalalertsystem.R
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.data.geojson.GeoJsonFeature
import com.google.maps.android.data.geojson.GeoJsonLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var geofencingClient: GeofencingClient

    private val TAG = "MapFragment"

    // Default radius when not provided in GeoJSON (in meters)
    private val DEFAULT_RADIUS_METERS = 300f

    // Geofence pending intent action constant (same as in Manifest / receiver filter)
    companion object {
        const val GEOFENCE_REQUEST_ID_PREFIX = "dead_zone_"
        const val GEOFENCE_ACTION = "com.example.nosignalalertsystem.action.GEOFENCE_EVENT"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        // Map fragment setup
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Center camera to a sensible default if you have dataset bounds
        // googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(25.0,85.0), 6f))

        // Load GeoJSON & draw + register geofences
        lifecycleScope.launch {
            loadGeoJsonAndSetupZones()
        }
    }

    // Reads the asset, parses GeoJSON, draws circles and registers geofences
    private suspend fun loadGeoJsonAndSetupZones() = withContext(Dispatchers.IO) {
        try {
            val json = loadJSONFromAsset("dead_zones.geojson") ?: run {
                Log.e(TAG, "GeoJSON asset not found")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "dead_zones.geojson not found in assets", Toast.LENGTH_LONG).show()
                }
                return@withContext
            }

            val geoJsonObj = JSONObject(json)
            withContext(Dispatchers.Main) {
                // Use GeoJsonLayer to visualize features if you want
                val layer = GeoJsonLayer(googleMap, geoJsonObj)
                layer.addLayerToMap()
            }

            // Iterate features, draw circles and create geofence requests
            val features = geoJsonObj.getJSONArray("features")
            val geofenceList = ArrayList<Geofence>()

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val props = feature.optJSONObject("properties")
                val geom = feature.optJSONObject("geometry")
                if (geom == null) continue
                val type = geom.optString("type")
                if (type != "Point") continue // Expect Point features

                val coords = geom.getJSONArray("coordinates")
                val lon = coords.getDouble(0)
                val lat = coords.getDouble(1)
                val center = LatLng(lat, lon)

                // radius from properties or default
                val radiusMeters =
                    props?.optDouble("radius", DEFAULT_RADIUS_METERS.toDouble())?.toFloat()
                        ?: DEFAULT_RADIUS_METERS

                val zoneId = props?.optString("zoneId") ?: "$i"
                val requestId = GEOFENCE_REQUEST_ID_PREFIX + zoneId

                // Draw circle on map (UI thread)
                withContext(Dispatchers.Main) {
                    googleMap.addCircle(
                        CircleOptions()
                            .center(center)
                            .radius(radiusMeters.toDouble())
                            .strokeColor(0x66FF0000) // semi-transparent red stroke
                            .fillColor(0x22FF0000)   // very transparent fill
                            .strokeWidth(2f)
                    )
                }

                // Build geofence
                val gf = Geofence.Builder()
                    .setRequestId(requestId)
                    .setCircularRegion(lat, lon, radiusMeters.toFloat())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setLoiteringDelay(60 * 1000) // optional: require 60 seconds dwell for ENTER -> helps reduce flapping
                    .build()

                geofenceList.add(gf)
            }

            // Now register geofences (on Main thread)
            withContext(Dispatchers.Main) {
                if (geofenceList.isNotEmpty()) {
                    addGeofences(geofenceList)
                } else {
                    Toast.makeText(requireContext(), "No geofences found in GeoJSON", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error loading geojson: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to load dead_zones.geojson", Toast.LENGTH_LONG).show()
            }
        }
    }

    // read asset file to String
    private fun loadJSONFromAsset(filename: String): String? {
        return try {
            val `is`: InputStream = requireContext().assets.open(filename)
            val size: Int = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            String(buffer, Charsets.UTF_8)
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    // Request location permission check and add geofences
    private fun addGeofences(geofenceList: List<Geofence>) {
        // Permission checks
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Ask for permission (UI should prompt to user)
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 302)
            Toast.makeText(requireContext(), "Location permission required to add geofences", Toast.LENGTH_LONG).show()
            return
        }

        // Background location permission on Android 10+ required to receive geofence transitions while app is backgrounded
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // You must request ACCESS_BACKGROUND_LOCATION separately (show rationale)
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 302)
                Toast.makeText(requireContext(), "Background location permission required for geofence monitoring", Toast.LENGTH_LONG).show()
                return
            }
        }

        // Build GeofencingRequest
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        // PendingIntent points to BroadcastReceiver (GeofenceBroadcastReceiver)
        val pendingIntent = GeofenceBroadcastReceiver.getGeofencePendingIntent(requireContext())

        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Dead zone geofences added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to add geofences: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "addGeofences failure", e)
            }
    }
}
