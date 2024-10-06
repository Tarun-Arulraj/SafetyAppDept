package com.example.safetyappdept

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.maps.android.PolyUtil
import org.json.JSONException
import org.json.JSONObject

class HomeFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    private lateinit var myMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var userMarker: Marker? = null
    private var deptMarker: Marker? = null
    private var polyline: com.google.android.gms.maps.model.Polyline? = null
    private lateinit var resolveButton: Button
    private var userId: String? = null

    companion object {
        private const val LOCATION_REQUEST_CODE = 1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        if (mapFragment != null) {
            mapFragment.getMapAsync(this@HomeFragment)
        } else {
            // Handle the null case, e.g. show an error message
            println ("Error: Map fragment is null")
        }

        // Initialize the resolve button
        resolveButton = view.findViewById(R.id.resolve_button)
        resolveButton.visibility = View.GONE // Hide the button by default

        // Set a click listener for the button
        resolveButton.setOnClickListener {
            val alertDialog = AlertDialog.Builder(requireContext())
            alertDialog.setTitle("Resolve Emergency")
            alertDialog.setMessage("Is the emergency resolved?")
            alertDialog.setPositiveButton("Yes") { _, _ ->
                // Remove the blue marker from the map
                if (userMarker != null) {
                    userMarker?.remove()
                    userMarker = null
                }

                // Hide the resolve button
                resolveButton.visibility = View.GONE

                // Display a message to the department
                Toast.makeText(requireContext(), "Emergency resolved successfully", Toast.LENGTH_SHORT).show()
            }
            alertDialog.setNegativeButton("No") { _, _ ->
                // Do nothing
            }
            alertDialog.show()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.requireContext())

        // Receive alert message and location from user app
        FirebaseMessaging.getInstance().subscribeToTopic("department-topic")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("DepartmentApp", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            val msg = "DepartmentApp: Got FCM registration token: $token"
            Log.d("DepartmentApp", msg)

            // Handle incoming notifications
            FirebaseMessaging.getInstance().isAutoInitEnabled = true
            FirebaseMessaging.getInstance().subscribeToTopic("department-topic")
        }

        // Add a listener to the notifications collection
        val db = FirebaseFirestore.getInstance()
        db.collection("notifications").addSnapshotListener { querySnapshot, e ->
            if (e != null) {
                Log.w("Notification", "Listen failed.", e)
                return@addSnapshotListener
            }

            querySnapshot?.forEach { document ->
                val notificationData = document.data
                val userId = notificationData["userId"] as String
                val locationMap = notificationData["location"] as HashMap<*, *> // Get the location as a HashMap
                val location = Location("") // Create a new Location object
                location.latitude = locationMap["latitude"] as Double
                location.longitude = locationMap["longitude"] as Double
                val message = notificationData["message"] as String

                // Display the notification on the screen
                displayNotification(userId, location, message)
            }
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(googleMap: GoogleMap) {
        myMap = googleMap

        myMap.uiSettings.isZoomControlsEnabled = true
        myMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        myMap.setOnMarkerClickListener(this)
        setUpMap()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode , permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setUpMap()
            }
        }
    }

    private fun setUpMap(){
        if (ActivityCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            myMap.isMyLocationEnabled = true
            val locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            locationRequest.interval = 60000
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        val location = locationResult.lastLocation
                        if (location != null) {
                            currentLocation = location
                            val currentLatLng = LatLng(location.latitude, location.longitude)
                            // Remove the old marker
                            if (deptMarker != null) {
                                deptMarker?.remove()
                            }
                            // Add a new marker
                            val deptMarkerOptions = MarkerOptions().position(currentLatLng)
                            deptMarkerOptions.title("Department Location")
                            deptMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            deptMarker = myMap.addMarker(deptMarkerOptions)
                            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                            storeCurrentLocationInFirestore(location)
                            updateDepartmentLocationInFirestore(location)
                        }
                    }
                },
                Looper.myLooper()
            )
        } else {
            ActivityCompat.requestPermissions(this.requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE)
        }
    }

    private fun updateDepartmentLocationInFirestore(location: Location) {
        val db = FirebaseFirestore.getInstance()
        val departmentId = FirebaseAuth.getInstance().currentUser?.uid
        if (departmentId != null) {
            db.collection("departments").document(departmentId).update("location", location)
                .addOnSuccessListener {
                    Log.d("Department Location", "Department location updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("Department Location", "Error updating department location", e)
                }
        }
    }

    private fun placeMarkerOnMap(currentLatLng: LatLng) {
        val markerOptions = MarkerOptions().position(currentLatLng)
        markerOptions.title("$currentLatLng")
        myMap.addMarker(markerOptions)
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        return false
    }

    private fun displayNotification(userId: String, location: Location, message: String) {
        this.userId = userId // Assign the userId to the class variable
        // Get the user's name from Firestore
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val userName = document.get("name") as String
                Log.d("User  Name", "User   name: $userName")

                // Display the notification on the screen
                val alertDialog = AlertDialog.Builder(requireContext())
                alertDialog.setTitle("Emergency Notification")
                alertDialog.setMessage("User  $userName needs assistance at location $location")
                alertDialog.setPositiveButton("Respond") { _, _ ->
                    // Remove the old marker if it exists
                    if (userMarker != null) {
                        userMarker?.remove()
                    }

                    // Display the user's location on the map
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    val userMarkerOptions = MarkerOptions().position(userLatLng)
                    userMarkerOptions.title("User  Location")
                    userMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    userMarker = myMap.addMarker(userMarkerOptions)
                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))

                    // Show the resolve button
                    resolveButton.visibility = View.VISIBLE

                    // Respond to the notification
                    respondToNotification(userId, location)
                    removeNotificationFromDatabase(userId)
                }
                alertDialog.setNegativeButton ("Ignore") { _, _ ->
                    // Ignore the notification
                    removeNotificationFromDatabase(userId)
                }
                alertDialog.show()
            } else {
                Log.e("User  Name", "User  document does not exist")
            }
        }.addOnFailureListener { e ->
            Log.e("User  Name", "Error getting user name: $e")
        }
    }

    private fun removeNotificationFromDatabase(userId: String) {
        // Remove the notification from the database
        val db = FirebaseFirestore.getInstance()
        db.collection("notifications").whereEqualTo("userId", userId).get().addOnSuccessListener { querySnapshot ->
            if (querySnapshot.documents.isNotEmpty()) {
                val notificationId = querySnapshot.documents[0].id
                db.collection("notifications").document(notificationId).delete().addOnSuccessListener {
                    Log.d("Notification", "Notification removed from database")
                }.addOnFailureListener { e ->
                    Log.e("Notification", "Error removing notification from database", e)
                }
            }
        }.addOnFailureListener { e ->
            Log.e("Notification", "Error getting notification from database", e)
        }
    }

    private fun respondToNotification(userId: String, location: Location) {
        try {
            // Get the department's current location from Firestore
            val db = FirebaseFirestore.getInstance()
            val departmentId = FirebaseAuth.getInstance().currentUser?.uid
            if (departmentId != null) {
                db.collection("departments").document(departmentId).get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        val locationMap = document.get("location") as HashMap<*, *>
                        val departmentLocation = Location("")
                        departmentLocation.latitude = locationMap["latitude"] as Double
                        departmentLocation.longitude = locationMap["longitude"] as Double

                        // Remove the old marker
                        if (deptMarker != null) {
                            deptMarker?.remove()
                        }

                        // Display both locations on the map
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        val departmentLatLng = LatLng(departmentLocation.latitude, departmentLocation.longitude)
                        if (userMarker != null) {
                            userMarker?.remove()
                        }
                        val userMarkerOptions = MarkerOptions().position(userLatLng)
                        userMarkerOptions.title("User   Location")
                        userMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        userMarker = myMap.addMarker(userMarkerOptions)
                        val departmentMarkerOptions = MarkerOptions().position(departmentLatLng)
                        departmentMarkerOptions.title("Department Location")
                        departmentMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        deptMarker = myMap.addMarker(departmentMarkerOptions)

                        // Draw the route between the user and department locations
                        val url = "https://maps.googleapis.com/maps/api/directions/json"
                        val params = HashMap<String, String>()
                        params["origin"] = "${departmentLocation.latitude},${departmentLocation.longitude}" // Department's location as origin
                        params["destination"] = "${location.latitude},${location.longitude}" // User's location as destination
                        params["key"] = "AIzaSyCVHc1BOnFGwq1E6xwEYGei3VYgHvVIgu8" // Replace with your actual API key

                        val uri = Uri.parse(url).buildUpon()
                        params.forEach { param ->
                            uri.appendQueryParameter(param.key, param.value)
                        }
                        val fullUrl = uri.toString()

                        val request = StringRequest(
                            Request.Method.GET, fullUrl,
                            { response ->
                                try {
                                    val jsonObject = JSONObject(response)
                                    val routes = jsonObject.getJSONArray("routes")
                                    val route = routes.getJSONObject(0)
                                    val overviewPolyline = route.getJSONObject("overview_polyline")
                                    val polylineString = overviewPolyline.getString("points")
                                    val polylineList = PolyUtil.decode(polylineString)
                                    polyline = myMap.addPolyline(
                                        PolylineOptions()
                                            .width(10f)
                                            .geodesic(true)
                                            .addAll(polylineList)
                                    )
                                } catch (e: JSONException) {
                                    Log.e("Error", "Error parsing JSON response: $e")
                                }
                            },
                            { error ->
                                Log.e("Error", "Error getting directions: $error")
                            })

                        // Set up the request queue
                        val requestQueue = Volley.newRequestQueue(requireContext())
                        requestQueue.add(request)

                        myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    } else {
                        Log.e("Department Location", "Department document does not exist")
                    }
                }.addOnFailureListener { e ->
                    Log.e("Department Location", "Error getting department location: $e")
                }
            }
        } catch (e: Exception) {
            Log.e("Respond to Notification", "Error responding to notification: $e")
        }
    }

    private fun storeCurrentLocationInFirestore(location: Location) {
        val db = FirebaseFirestore.getInstance()
        val departmentId = FirebaseAuth.getInstance().currentUser?.uid
        if (departmentId != null) {
            db.collection("departments").document(departmentId).update("location", location)
                .addOnSuccessListener {
                    Log.d("Department Location", "Department location updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("Department Location", "Error updating department location", e)
                }
        }
    }
}