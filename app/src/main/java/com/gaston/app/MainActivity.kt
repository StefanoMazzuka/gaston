package com.gaston.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gaston.app.ui.GastonApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val statusBarColor = android.graphics.Color.rgb(243, 247, 238)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(statusBarColor))
        setContent { GastonApp(viewModel()) }
    }
}
