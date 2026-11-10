package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HomeScreen
import com.example.ui.TodoViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.TaskWidgetProvider

class MainActivity : ComponentActivity() {
    private var todoViewModel: TodoViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: TodoViewModel = viewModel()
                todoViewModel = viewModel
                handleWidgetIntent(intent, viewModel)
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        todoViewModel?.let { handleWidgetIntent(intent, it) }
    }

    private fun handleWidgetIntent(intent: Intent?, viewModel: TodoViewModel) {
        if (intent?.getBooleanExtra(TaskWidgetProvider.EXTRA_OPEN_ADD, false) == true) {
            viewModel.openAddModal()
            intent.removeExtra(TaskWidgetProvider.EXTRA_OPEN_ADD)
        }
    }
}
