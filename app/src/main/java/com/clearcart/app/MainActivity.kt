package com.clearcart.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.clearcart.app.ui.ClearCartApp
import com.clearcart.app.ui.theme.ClearCartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as com.clearcart.app.ClearCartApp).container
        setContent {
            ClearCartTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ClearCartApp(container)
                }
            }
        }
    }
}
