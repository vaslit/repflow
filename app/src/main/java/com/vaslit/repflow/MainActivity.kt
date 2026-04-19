package com.vaslit.repflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.vaslit.repflow.ui.AppViewModel
import com.vaslit.repflow.ui.RepFlowApp
import com.vaslit.repflow.ui.RepFlowTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<AppViewModel> {
        AppViewModel.Factory((application as RepFlowApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RepFlowTheme {
                RepFlowApp(viewModel = viewModel)
            }
        }
    }
}
