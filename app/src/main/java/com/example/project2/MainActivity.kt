package com.example.project2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.project2.ui.theme.Project2Theme
import com.example.project2.ui.MindMatchApp

/** Single-activity entry point that hosts the Compose app. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Project2Theme {
                MindMatchApp()
            }
        }
    }
}
