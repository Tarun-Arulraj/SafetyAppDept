package com.example.safetyappdept

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    lateinit var name: EditText
    lateinit var username: EditText
    lateinit var password1: EditText
    lateinit var confirmPassword: EditText
    lateinit var registerButton: Button
    lateinit var loginText: TextView
    lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        name = findViewById<EditText>(R.id.name)
        username = findViewById<EditText>(R.id.username)
        password1 = findViewById<EditText>(R.id.password)
        confirmPassword = findViewById<EditText>(R.id.confirmPassword)
        registerButton = findViewById<Button>(R.id.registerButton)
        loginText = findViewById<TextView>(R.id.loginText)
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (firebaseAuth.currentUser != null) {
            onLoginRegisterSuccess()
        }

        registerButton.setOnClickListener(View.OnClickListener {
            val email = username.text.toString().trim()
            val password = password1.text.toString().trim()
            val departmentName = name.text.toString().trim()

            if (password.length >= 8) {
                var hasCapitalLetter = false
                var hasSmallLetter = false
                var hasNumber = false
                var hasSymbol = false
                for (char in password) {
                    if (char.isUpperCase()) {
                        hasCapitalLetter = true
                    } else if (char.isLowerCase()) {
                        hasSmallLetter = true
                    } else if (char.isDigit()) {
                        hasNumber = true
                    } else if (!char.isLetterOrDigit()) {
                        hasSymbol = true
                    }
                }
                if (hasCapitalLetter && hasSmallLetter && hasNumber && hasSymbol) {
                    if (password == confirmPassword.text.toString()) {
                        firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("FirebaseAuth", "Department created successfully")

                                    // Create a department document in Firestore
                                    val department = task.result.user
                                    val departmentData = hashMapOf(
                                        "departmentName" to departmentName,
                                        "email" to email,
                                        "password" to password
                                    )
                                    firestore.collection("departments").document(department!!.uid).set(departmentData)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                Log.d("Firestore", "Department document created successfully")
                                            } else {
                                                Log.e("Firestore", "Error creating department document: ${task.exception?.message}")
                                            }
                                        }

                                    Toast.makeText(this, "Department Created.", Toast.LENGTH_SHORT).show()
                                    onLoginRegisterSuccess()
                                } else {
                                    Log.e("FirebaseAuth", "Error creating department: ${task.exception?.message}")
                                    Toast.makeText(this, "Department Not Created.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Password and Confirm Password do not match!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Password must have at least one capital letter, one small letter, one number, and one symbol!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Password must be at least 8 characters long!", Toast.LENGTH_SHORT).show()
            }
        })

        val spannableString = SpannableString("Already have an account? Login")
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                val intent = Intent(this@MainActivity, MainActivity2::class.java)
                startActivity(intent)
            }
        }
        spannableString.setSpan(clickableSpan, 24, spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        loginText.text = spannableString
        loginText.movementMethod = LinkMovementMethod.getInstance()
    }

    fun onLoginRegisterSuccess() {
        val department = firebaseAuth.currentUser
        if (department != null) {
            firestore.collection("departments").document(department.uid).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val departmentData = task.result.data
                        if (departmentData != null) {
                            Log.d("Firestore", "Department data retrieved successfully")
                            // You can use the department data here
                        } else {
                            Log.e("Firestore", "Error retrieving department data")
                        }
                    } else {
                        Log.e("Firestore", "Error retrieving department data: ${task.exception?.message}")
                    }
                }
        }

        val intent = Intent(this, MainActivity3::class.java)
        startActivity(intent)
        finish()
    }
}