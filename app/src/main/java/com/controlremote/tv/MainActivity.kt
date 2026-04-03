package com.controlremote.tv

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.controlremote.tv.ui.RemoteScreen
import com.controlremote.tv.ui.theme.ControlRemoteTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ControlRemoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val app = LocalContext.current.applicationContext as Application
                    val vm: RemoteViewModel = viewModel(factory = RemoteViewModel.factory(app))
                    RemoteScreen(
                        viewModel = vm,
                        onLocaleSelected = { tag ->
                            AppLocale.persistAndApply(this@MainActivity, tag)
                            recreate()
                        }
                    )
                }
            }
        }
    }
}
