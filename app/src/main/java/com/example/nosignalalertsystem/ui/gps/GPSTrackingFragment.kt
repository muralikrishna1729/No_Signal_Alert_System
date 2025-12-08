package com.example.nosignalalertsystem.ui.gps

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.nosignalalertsystem.R // Ensures resources are linked
import com.example.nosignalalertsystem.databinding.FragmentGpsTrackingBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import java.io.IOException
import androidx.fragment.app.FragmentActivity
import java.util.*

// Suppress deprecation for LocationRequest usage
@Suppress("DEPRECATION")
class GPSTrackingFragment : Fragment(R.layout.fragment_gps_tracking) {

    private var _binding: FragmentGpsTrackingBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocation: FusedLocationProviderClient
    private lateinit var locationRequest: com.google.android.gms.location.LocationRequest
    private lateinit var geocoder: Geocoder
    private var isLocationUpdatesRequested = false

    private companion object {
        const val TAG = "GPSTrackingFragment"
        // FIX: Changed to uppercase to resolve naming convention warning (image_02cb03.png)
        const val CHECK_SETTINGS_REQUEST_CODE = 103
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentGpsTrackingBinding.bind(view)

        fusedLocation = LocationServices.getFusedLocationProviderClient(requireContext())
        geocoder = Geocoder(requireContext(), Locale.getDefault())

        // Initial LocationRequest setup
        createLocationRequest(binding.swGps.isChecked)

        // Initial state update
        binding.tvUpdates.text = getString(R.string.status_off)
        binding.tvSensor.text = if (binding.swGps.isChecked) getString(R.string.sensor_gps) else getString(R.string.sensor_battery_saver)

        // Listeners
        binding.swLocationsupdates.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) checkLocationSettingsAndStartUpdates()
            else stopLocationUpdates()
        }

        binding.swGps.setOnCheckedChangeListener { _, isChecked ->
            setGPSMode(isChecked)
        }
    }

    // -------------------------------------------------------------
    // LOCATION REQUEST CREATION
    // -------------------------------------------------------------
    private fun createLocationRequest(enableGps: Boolean) {
        val priority = if (enableGps)
            Priority.PRIORITY_HIGH_ACCURACY
        else
            Priority.PRIORITY_BALANCED_POWER_ACCURACY

        locationRequest = LocationRequest.Builder(priority, 3000)
            .setMinUpdateIntervalMillis(1500)
            .setWaitForAccurateLocation(true)
            .build()
    }


    // -------------------------------------------------------------
    // LOCATION SETTINGS CHECK
    // -------------------------------------------------------------
    private fun checkLocationSettingsAndStartUpdates() {
        if (!hasLocationPermission()) {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            return
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(requireContext())
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // Location settings are satisfied, start updates
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, show dialog
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    requestLocationSettingsLauncher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(TAG, "Error showing location settings dialog", sendEx)
                }
            } else {
                Toast.makeText(requireContext(), "Location services required.", Toast.LENGTH_SHORT).show()
                binding.swLocationsupdates.isChecked = false
            }
        }
    }

    // -------------------------------------------------------------
    // LOCATION UPDATES START/STOP
    // -------------------------------------------------------------
    // FIX: Suppress the MissingPermission warning here, as it's checked in hasLocationPermission()
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        // Handle Background Location for Android 10 (Q) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isBackgroundLocationGranted()) {
            requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            return
        }

        fusedLocation.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        ).addOnSuccessListener {
            isLocationUpdatesRequested = true
            binding.tvUpdates.text = getString(R.string.status_tracking)
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start location updates", e)
            Toast.makeText(requireContext(), "Failed to start tracking.", Toast.LENGTH_SHORT).show()
            binding.swLocationsupdates.isChecked = false
        }
    }

    private fun stopLocationUpdates() {
        if (isLocationUpdatesRequested) {
            fusedLocation.removeLocationUpdates(locationCallback)
            isLocationUpdatesRequested = false
        }
        binding.tvUpdates.text = getString(R.string.status_off)
    }

    // -------------------------------------------------------------
    // LOCATION CALLBACK AND UI UPDATE LOGIC
    // -------------------------------------------------------------
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return

            // Update basic coordinates
            binding.tvLat.text = loc.latitude.toString()
            binding.tvLon.text = loc.longitude.toString()

            // Safe access to location attributes
            binding.tvAltitude.text = if (loc.hasAltitude()) loc.altitude.toString() else getString(R.string.value_na)

            // FIX: Use Locale.getDefault() for safe formatting (image_02cb03.png)
            binding.tvAccuracy.text = if (loc.hasAccuracy()) String.format(Locale.getDefault(), "%.2f m", loc.accuracy) else getString(R.string.value_na)
            binding.tvSpeed.text = if (loc.hasSpeed()) String.format(Locale.getDefault(), "%.2f m/s", loc.speed) else getString(R.string.value_na)

            // Sensor information update
            binding.tvSensor.text = when(loc.provider) {
                "gps" -> getString(R.string.sensor_gps)
                "network" -> getString(R.string.sensor_network)
                // Use the battery saver string if the priority was set low
                else -> if (locationRequest.priority == com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY) {
                    getString(R.string.sensor_battery_saver)
                } else {
                    getString(R.string.sensor_unknown)
                }
            }

            // Call the address update function
            updateAddress(loc.latitude, loc.longitude)
        }
    }

    // -------------------------------------------------------------
    // GEOCODING LOGIC (Address Lookup)
    // -------------------------------------------------------------
    private fun updateAddress(latitude: Double, longitude: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // New Asynchronous Geocoding (API 33+) - RECOMMENDED
            geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {

                // FIX: Correct required method name is 'onGeocode' not 'onGeocodeResults' (image_05f91c.png)
                override fun onGeocode(addresses: List<Address>) {
                    if (addresses.isNotEmpty()) {
                        binding.tvAddress.text = addresses[0].getAddressLine(0) ?: getString(R.string.address_na)
                    } else {
                        binding.tvAddress.text = getString(R.string.address_na)
                    }
                }

                override fun onError(errorMessage: String?) {
                    Log.e(TAG, "Geocoding error: $errorMessage")
                    binding.tvAddress.text = getString(R.string.address_na)
                }
            })
        } else {
            // Deprecated Synchronous Geocoding (API < 33)
            Thread {
                try {
                    @Suppress("DEPRECATION")
                    val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

                    val addressText = if (!addresses.isNullOrEmpty()) {
                        addresses[0].getAddressLine(0) ?: getString(R.string.address_na)
                    } else {
                        getString(R.string.address_na)
                    }

                    // Switch back to the main thread to update the UI
                    activity?.runOnUiThread {
                        binding.tvAddress.text = addressText
                    }

                } catch (e: IOException) {
                    Log.e(TAG, "Synchronous Geocoding IO Exception", e)
                    activity?.runOnUiThread {
                        binding.tvAddress.text = getString(R.string.address_service_unavailable)
                    }
                }
            }.start()
        }
    }

    private fun setGPSMode(enableGps: Boolean) {
        // Create the new LocationRequest based on the switch state
        createLocationRequest(enableGps)

        // Restart updates to immediately apply the mode change if tracking is active
        if (binding.swLocationsupdates.isChecked) {
            // Stop and restart to apply the new locationRequest
            stopLocationUpdates()
            checkLocationSettingsAndStartUpdates()
        }
    }

    // -------------------------------------------------------------
    // PERMISSION CHECKS
    // -------------------------------------------------------------
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isBackgroundLocationGranted(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // -------------------------------------------------------------
    // ACTIVITY RESULT LAUNCHERS
    // -------------------------------------------------------------
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {

            // Check for background permission only if API >= Q (29)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isBackgroundLocationGranted()) {
                requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                checkLocationSettingsAndStartUpdates()
            }
        } else {
            // Permission denied
            binding.swLocationsupdates.isChecked = false
            Toast.makeText(requireContext(), "Location permission is required for tracking.", Toast.LENGTH_LONG).show()
        }
    }

    private val requestBackgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkLocationSettingsAndStartUpdates()
        } else {
            // Non-fatal, tracking can still start without background permission
            checkLocationSettingsAndStartUpdates()
        }
    }

    private val requestLocationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == FragmentActivity.RESULT_OK) {
            // User accepted location settings change
            startLocationUpdates()
        } else {
            // User denied location settings change
            binding.swLocationsupdates.isChecked = false
            Toast.makeText(requireContext(), "GPS must be enabled for tracking.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Crucial: Stop location updates to save battery and prevent leaks
        if (this::fusedLocation.isInitialized && isLocationUpdatesRequested) {
            fusedLocation.removeLocationUpdates(locationCallback)
        }
        _binding = null
    }
}