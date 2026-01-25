package com.example.musicplayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase

class SignIn : AppCompatActivity() {
    private var database = FirebaseDatabase.getInstance().getReference("Users")

    companion object {
        const val KEY1 = "com.example.musicplayer.SignIn.name"
        const val KEY2 = "com.example.musicplayer.SignIn.email"
        const val KEY3 = "com.example.musicplayer.SignIn.password"
        const val KEY4 = "com.example.musicplayer.SignIn.id"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)



        val signInbutton = findViewById<Button>(R.id.button)
        val userid = findViewById<TextInputEditText>(R.id.userIdEditText)

        signInbutton.setOnClickListener {
            val useridstring = userid.text.toString()
            if(useridstring.isNotEmpty()){
                readData(useridstring)
            }else{
                Toast.makeText(this,"Please enter the User Id",Toast.LENGTH_SHORT).show()
            }

        }



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


    }
    private fun readData(userid: String) {
        database = FirebaseDatabase.getInstance().getReference("Users")
        database.child(userid).get().addOnSuccessListener {
            if (it.exists()) {
                val email = it.child("email").value.toString()
                val name = it.child("name").value.toString()
                val password = it.child("password").value.toString()
                
                // Save login state AND user details
                val sharedPreferences = getSharedPreferences("MusicPlayerPrefs", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putBoolean("isLoggedIn", true)
                editor.putString("userName", name)
                editor.putString("userEmail", email)
                editor.putString("userId", userid)
                editor.apply()

                Toast.makeText(this, "Successfully Logged In", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, WelcomePg::class.java)
                intent.putExtra(KEY1, name)
                intent.putExtra(KEY2, email)
                intent.putExtra(KEY3, password)
                intent.putExtra(KEY4, userid)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "User Doesn't Exist", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener{
            Toast.makeText(this,"Failed",Toast.LENGTH_SHORT).show()
        }
    }
}
