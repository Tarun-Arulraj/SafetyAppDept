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

class MainActivity2 : AppCompatActivity() {

    lateinit var username: EditText
    lateinit var password: EditText
    lateinit var loginButton: Button
    lateinit var registerText: TextView
    lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        username = findViewById<EditText>(R.id.username)
        password = findViewById<EditText>(R.id.password)
        loginButton = findViewById<Button>(R.id.loginButton)
        registerText = findViewById<TextView>(R.id.registerText)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (firebaseAuth.currentUser != null) {
            // User is already signed in, start the main activity
            val intent = Intent(this, MainActivity3::class.java)
            startActivity(intent)
            finish()
        }

        loginButton.setOnClickListener(View.OnClickListener {
            val usernameText = username.text.toString()
            val passwordText = password.text.toString()

            if (passwordText.length >= 8) {
                var hasCapitalLetter = false
                var hasSmallLetter = false
                var hasNumber = false
                var hasSymbol = false
                for (char in passwordText) {
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
                    authenticateUser(usernameText, passwordText)
                } else {
                    Toast.makeText(this, "Password must have at least one capital letter, one small letter, one number, and one symbol!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Password must be at least 8 characters long!", Toast.LENGTH_SHORT).show()
            }
        })

        val spannableString = SpannableString("Don't have an account? Register")
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                val intent = Intent(this@MainActivity2, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
        spannableString.setSpan(clickableSpan, 23, spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        registerText.text = spannableString
        registerText.movementMethod = LinkMovementMethod.getInstance()
    }

    fun onLoginRegisterSuccess() {
        val intent = Intent(this, MainActivity3::class.java)
        startActivity(intent)
        finish()
    }

    private fun authenticateUser(username: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "Department logged in successfully")
                    Toast.makeText(this, "Department Logged In.", Toast.LENGTH_SHORT).show()
                    onLoginRegisterSuccess()
                } else {
                    Log.e("FirebaseAuth", "Error logging in department: ${task.exception?.message}")
                    Toast.makeText(this, "Login Failed! Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}