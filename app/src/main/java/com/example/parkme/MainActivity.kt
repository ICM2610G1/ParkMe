package com.example.parkme

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.parkme.navigation.Navigation

<<<<<<< HEAD
//HOLAAA
=======

>>>>>>> 55a3899c187e427b4c40b77a05eddbe3f5ce0cd2
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Navigation()
        }
    }
}