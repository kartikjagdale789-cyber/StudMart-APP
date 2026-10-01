package com.kartik.studentmart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kartik.studentmart.navigation.AppNavigation
import com.kartik.studentmart.ui.theme.StudMartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudMartTheme {
                AppNavigation()
            }
        }
    }
}
