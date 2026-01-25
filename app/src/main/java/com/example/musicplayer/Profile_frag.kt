package com.example.musicplayer

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class Profile_frag : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile_frag, container, false)

        // Get SharedPreferences
        val sharedPreferences = requireActivity().getSharedPreferences("MusicPlayerPrefs", Context.MODE_PRIVATE)

        // Try to get data from SharedPreferences first, then fall back to Intent extras
        val name = sharedPreferences.getString("userName", null) ?: activity?.intent?.getStringExtra(SignIn.KEY1)
        val email = sharedPreferences.getString("userEmail", null) ?: activity?.intent?.getStringExtra(SignIn.KEY2)
        val id = sharedPreferences.getString("userId", null) ?: activity?.intent?.getStringExtra(SignIn.KEY4)

        // Initialize views using the inflated 'view'
        val welcomeText = view.findViewById<TextView>(R.id.textView2)
        val useridView = view.findViewById<TextView>(R.id.button2)
        val emailIdView = view.findViewById<TextView>(R.id.button3)

        // Set text to views
        welcomeText.text = "Welcome ${name ?: "User"}"
        useridView.text = "User Id : ${id ?: "N/A"}"
        emailIdView.text = "Email : ${email ?: "N/A"}"

        return view
    }
}
