package com.example.musicplayer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUp : AppCompatActivity() {

    private lateinit var database : DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        database = FirebaseDatabase.getInstance().getReference("Users")

        val idEditText = findViewById<TextInputEditText>(R.id.userIdEditText)
        val nameEditText = findViewById<TextInputEditText>(R.id.nameEditText)
        val emailEditText = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditText)

        val signup = findViewById<Button>(R.id.button)

        signup.setOnClickListener {
            val id = idEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (id.isNotEmpty() && name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                // Check if User ID already exists
                database.child(id).get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        Toast.makeText(this, "User ID already used, use another", Toast.LENGTH_SHORT).show()
                    } else {
                        // ID is unique, proceed with registration (Email check removed to avoid index errors)
                        val user = Users(id, name, email, password)
                        database.child(id).setValue(user).addOnSuccessListener {
                            Toast.makeText(this, "Successfully Registered", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this, SignIn::class.java)
                            startActivity(intent)
                            finish()
                        }.addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to register: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.addOnFailureListener { e ->
                    Log.e("SignUp", "ID check failed", e)
                    Toast.makeText(this, "Connection error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        val signin = findViewById<TextView>(R.id.textView2)
        signin.setOnClickListener {
            val intent = Intent(this, SignIn::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
