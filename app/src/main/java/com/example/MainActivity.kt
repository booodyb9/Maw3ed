package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MawaeeViewModel
import com.example.ui.navigation.AppNavGraph
import com.example.ui.theme.MawaeedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MawaeedTheme {
                val viewModel: MawaeeViewModel = viewModel()
                AppNavGraph(viewModel = viewModel)
            }
        }
    }
}
