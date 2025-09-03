package org.traccar.client

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.content.Context
import android.provider.Settings
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class HistoryFragment : Fragment(), OnMapReadyCallback {

    private var recyclerView: RecyclerView? = null
    private lateinit var adapter: SubmissionAdapter
    private lateinit var dbHelper: DatabaseHelper
//    private val apiService = RetrofitClient.retrofit.create(SyncApiService::class.java)
    private lateinit var apiService: SyncApiService
    private var requestingPermissions: Boolean = false
    private var googleMap: GoogleMap? = null // To hold the GoogleMap object
    private lateinit var fusedLocationClient: FusedLocationProviderClient // For location updates

//
//    data class ShipmentTracking (
//        val id :Int,
//        val deviceId: String,
//    )

//    private  val apiService = object : SyncApiService {
//    override suspend fun sendPosition(position: Position): Unit = Unit
//    override suspend fun sendFormData(submission: FormSubmission): Unit = Unit
//    override suspend fun login(request: LoginRequest): LoginResponse = throw NotImplementedError()
//    override suspend fun verifyCode(request: CodeVerificationRequest): CodeVerificationResponse = throw NotImplementedError()
//    override suspend fun getShipmentHistory(userId: String): List<ShipmentTracking> {
//        return listOf(
//            ShipmentTracking(
//                id = 1,
//                deviceId = "hd2334s",
////                userId = userId.toLong()
//            )
//        ).takeLast(20) // Mimic the .takeLast(20) in the original code
//    }
//}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext()) // Initialize after onAttach
        recyclerView = view.findViewById(R.id.submission_list)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize Map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        if (recyclerView != null) {
            recyclerView?.layoutManager = LinearLayoutManager(requireContext())
            adapter = SubmissionAdapter()
            recyclerView?.adapter = adapter
            fetchSubmissions()
        } else {
            Log.e("History", "RecyclerView is null")
        }
//        Log.e("History", "${PreferenceManager.getDefaultSharedPreferences(requireContext())
//            .getString(MainFragment.KEY_DEVICE, null)}")

//        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
//        val permissionAsked = sharedPreferences.getBoolean("location_permission_asked", false)

        startTrackingService(checkPermission = true)


//        if (!isServiceRunning(TrackingService::class.java)) {
//            startTrackingService(checkPermission = true)
//        }

    }

    override fun onStart() {
        super.onStart()
        if (requestingPermissions) {
            requestingPermissions = BatteryOptimizationHelper().requestException(requireContext())
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val permissionAsked = prefs.getBoolean(KEY_PERMISSION_ASKED, false)

        Log.d("HistoryFragment", "onResume called")
        if (!isServiceRunning(TrackingService::class.java) && !permissionAsked) {
            startTrackingService(checkPermission = true)
        }

        googleMap?.let { map ->
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (!map.isMyLocationEnabled) { // Only set if not already enabled to avoid redundant calls
                    map.isMyLocationEnabled = true
                    getCurrentLocationAndAddMarker()
                    Log.d("HistoryFragment", "Map MyLocation enabled and location refreshed in onResume.")
                }
            }
        }

    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun startTrackingService(checkPermission: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val permissionAsked = prefs.getBoolean(KEY_PERMISSION_ASKED, false)

        val requiredPermissions = mutableSetOf<String>()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        if (requiredPermissions.isEmpty()) {
            // Permissions granted, start the Service
            ContextCompat.startForegroundService(requireContext(), Intent(requireContext(), TrackingService::class.java))
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .edit()
                .putBoolean("status", true)
                .apply()
            Log.d("HistoryFragment", "TrackingService started")
            // Request battery optimization exemption
            requestingPermissions = BatteryOptimizationHelper().requestException(requireContext())
            googleMap?.let { map ->
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    map.isMyLocationEnabled = true
                    getCurrentLocationAndAddMarker() // Re-attempt to get and show location
                    Log.d("HistoryFragment", "Map MyLocation enabled and location refreshed after granting permissions.")
                }
            }
        } else {
            prefs.edit().putBoolean(KEY_PERMISSION_ASKED, true).apply()
            // Check if we should show rationale
            val showRationale = requiredPermissions.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)
            }
            Log.d("HistoryFragment", "Permissions needed: $requiredPermissions, showRationale: $showRationale")
            if (showRationale) {
                showPermissionRationaleDialog(requiredPermissions)
            } else {
                // Check if permanently denied
                if (requiredPermissions.any { !ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), it) &&
                            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_DENIED }) {
                    showSettingsDialog()
                } else {
                    requestPermissions(requiredPermissions.toTypedArray(), PERMISSIONS_REQUEST_LOCATION)
                }
            }
        }
    }

    private fun showPermissionRationaleDialog(requiredPermissions: Set<String>) {
        AlertDialog.Builder(requireContext())
            .setTitle("Location Permissions Required")
            .setMessage("This app needs location permissions to track your location in the background. Please grant access to continue.")
            .setPositiveButton("OK") { _, _ ->
                Log.d("HistoryFragment", "Requesting permissions: $requiredPermissions")
                requestPermissions(requiredPermissions.toTypedArray(), PERMISSIONS_REQUEST_LOCATION)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Log.d("HistoryFragment", "Permission rationale dialog cancelled")
                Toast.makeText(requireContext(), "Permissions denied, tracking cannot start", Toast.LENGTH_SHORT).show()
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .edit()
                    .putBoolean("status", false)
                    .apply()
            }
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permissions Denied")
            .setMessage("Location permissions have been permanently denied. Please enable them in the app settings to use tracking.")
            .setPositiveButton("Go to Settings") { _, _ ->
                Log.d("HistoryFragment", "Navigating to app settings")
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", requireContext().packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Log.d("HistoryFragment", "Settings dialog cancelled")
                Toast.makeText(requireContext(), "Permissions denied, tracking cannot start", Toast.LENGTH_SHORT).show()
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .edit()
                    .putBoolean("status", false)
                    .apply()
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d("HistoryFragment", "onRequestPermissionsResult: requestCode=$requestCode, permissions=${permissions.joinToString()}, results=${grantResults.joinToString()}")
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            val fineLocationGranted = permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION).let { index ->
                index != -1 && grantResults[index] == PackageManager.PERMISSION_GRANTED
            }
            val backgroundLocationGranted = permissions.indexOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION).let { index ->
                index == -1 || grantResults[index] == PackageManager.PERMISSION_GRANTED
            }

            if (fineLocationGranted) {
                Log.d("HistoryFragment", "ACCESS_FINE_LOCATION granted, starting TrackingService")
                startTrackingService(checkPermission = false)
                if (!backgroundLocationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Log.d("HistoryFragment", "ACCESS_BACKGROUND_LOCATION denied, showing rationale for background")
                    showBackgroundLocationRationaleDialog()
                }
            } else {
                Log.d("HistoryFragment", "Permissions denied: fineLocation=$fineLocationGranted, backgroundLocation=$backgroundLocationGranted")
                Toast.makeText(requireContext(), "Permissions denied, tracking cannot start", Toast.LENGTH_SHORT).show()
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .edit()
                    .putBoolean("status", false)
                    .apply()
                // Check for permanently denied permissions
                if (permissions.any { !ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), it) }) {
                    Log.d("HistoryFragment", "Some permissions permanently denied, showing settings dialog")
                    showSettingsDialog()
                }
            }
        }
    }

    private fun showBackgroundLocationRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Background Location Permission")
            .setMessage("For continuous tracking, please allow background location access.")
            .setPositiveButton("OK") { _, _ ->
                Log.d("HistoryFragment", "Requesting ACCESS_BACKGROUND_LOCATION")
                requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSIONS_REQUEST_LOCATION)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Log.d("HistoryFragment", "Background location permission declined")
                Toast.makeText(requireContext(), "Background location denied, tracking may be limited", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun fetchSubmissions() {
        lifecycleScope.launch {
            try {
                val user = dbHelper.selectUser() ?: run {
                    Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                    return@launch
                }

                val rawToken = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("auth_token", null)

                val token = rawToken?.substringAfter("Authorization=")?.substringBefore(";")?.trim()

                Log.e("History", "Token: ${token}")

                if (token == null) {
                    Log.e("History", "Auth token is null, cannot fetch submissions.")
                    Toast.makeText(requireContext(), "Session expired, please log in.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                    return@launch
                }

                apiService = RetrofitClient.getApiKeyClient(token.toString()).create(SyncApiService::class.java)

                val response = withContext(Dispatchers.IO) {
                    apiService.getShipmentHistory(user.id.toString())
                }
                val submissions = response.data.takeLast(20)
                adapter.setSubmissions(submissions)


//                val submissions = withContext(Dispatchers.IO) {
//                    apiService.getShipmentHistory(user.id.toString()).takeLast(20)
//                }
                Log.d("History", "Submissions: ${submissions}")
                adapter.setSubmissions(submissions)
                Log.d("History", "Submissions set")
                if (submissions.isEmpty()) {
                    Log.e("History", "No submissions found")
                }else {
                    // Save the first shipmentTrackingId to SharedPreferences
                    val firstShipmentTrackingId = submissions.firstOrNull()?.id ?: -1
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .edit()
                        .putInt("first_shipment_tracking_id", firstShipmentTrackingId)
                        .apply()
                    Log.d("History", "Saved first shipmentTrackingId: $firstShipmentTrackingId")
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("History", "HttpException: Code=${e.code()}, Body=$errorBody")
                if (e.code() == 401) { // Fixed deprecated code()
                    Log.e("History", "Session expired. Please log in again.")

                    if (dbHelper.clearUserDataSync()) {
                        startActivity(Intent(requireContext(), LoginActivity::class.java))
                        requireActivity().finish()
                    } else {
                        Log.e("History", "Failed to clear user data")
//                        Toast.makeText(requireContext(), "Failed to clear user data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("History", "$e")
//                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                Log.e("History", "HttpException: Code=${e.code()}, Body=$errorBody")
            } catch (e: Exception) {
                Log.e("History", "$e")
//                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class SubmissionAdapter : RecyclerView.Adapter<SubmissionAdapter.SubmissionViewHolder>() {

        private var submissions: List<ShipmentTracking> = emptyList()

        fun setSubmissions(submissions: List<ShipmentTracking>) {
            this.submissions = submissions
            Log.d("History","Adapter Submissions ${this.submissions}, ${submissions}")
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_submission, parent, false)
            return SubmissionViewHolder(view)
        }

        override fun onBindViewHolder(holder: SubmissionViewHolder, position: Int) {
            val submission = submissions[position]
            holder.containerId.text = "Conatiner No: ${submission.containerNo}"
            holder.comment.text = "Device Id: ${submission.deviceId}"
            holder.status.text = "Status: ${submission.status ?: "PENDING"}"
            holder.name.text = "Driver: ${submission.firstName ?: "Paul"} ${submission.lastName ?: "Naftali"}"

            Log.d("History", "On  Bind ViewHolder: ${holder.containerId.text}, ${holder.comment.text}, ${submission}")
        }

        override fun getItemCount(): Int = submissions.size

        inner class SubmissionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val containerId: TextView = itemView.findViewById(R.id.container_id)
            val comment: TextView = itemView.findViewById(R.id.comment)
            val status : TextView = itemView.findViewById(R.id.status)
            val name : TextView = itemView.findViewById(R.id.name)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        Log.d("HistoryFragment", "Google Map is ready")

        // Enable zoom controls and my location button
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true

        // Check for location permissions before attempting to get location
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true // Show blue dot for current location
            getCurrentLocationAndAddMarker()
        } else {
            Log.d("HistoryFragment", "Location permission not granted for map. Blue dot might not show.")
        }
    }

    private fun getCurrentLocationAndAddMarker() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        Log.d("HistoryFragment", "Current Location: ${location.latitude}, ${location.longitude}")
                        googleMap?.apply {
                            clear() // Clear existing markers
                            addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))
                            moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f)) // Zoom level 15
                        }
                    } else {
                        Log.w("HistoryFragment", "Last known location is null.")
                        Toast.makeText(requireContext(), "Could not get current location. Please ensure location services are enabled.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("HistoryFragment", "Error getting last known location: ${e.message}", e)
                    Toast.makeText(requireContext(), "Error getting location: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Log.d("HistoryFragment", "Location permission not granted for getCurrentLocationAndAddMarker.")
            // Permissions are requested by startTrackingService, so we don't re-request here.
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_LOCATION = 2
        private const val KEY_PERMISSION_ASKED = "location_permission_asked"
        private const val KEY_TRACKING_RUNNING = "tracking_service_running"
    }
}