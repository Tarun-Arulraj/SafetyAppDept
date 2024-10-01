package com.example.safetyappdept

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle incoming notifications
        val message = remoteMessage.data["message"]
        val location = remoteMessage.data["location"]
        val otherInfo = remoteMessage.data["otherInfo"]

        // Display the user's location and info on the app screen
        Log.d("DepartmentApp", "Received message: $message")
        Log.d("DepartmentApp", "Received location: $location")
        Log.d("DepartmentApp", "Received other info: $otherInfo")

        // Show an alert dialog with the received message and location
        val alertDialog = AlertDialog.Builder(applicationContext)
        alertDialog.setTitle("Emergency Service")
        alertDialog.setMessage("User needs help! Location: $location")
        alertDialog.setPositiveButton("Yes") { _, _ ->
            // Handle the yes case
        }
        alertDialog.setNegativeButton("No") { _, _ ->
            // Handle the no case
        }
        alertDialog.show()
    }
}