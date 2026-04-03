package com.controlremote.tv

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.controlremote.tv.billing.BillingManager
import com.controlremote.tv.ui.RemoteScreen
import com.controlremote.tv.ui.theme.ControlRemoteTheme

class MainActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        billingManager = BillingManager(this)
        billingManager.start()
        setContent {
            ControlRemoteTheme {
                val adsRemoved by billingManager.adsRemoved.collectAsState()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val app = LocalContext.current.applicationContext as Application
                    val vm: RemoteViewModel = viewModel(factory = RemoteViewModel.factory(app))
                    RemoteScreen(
                        viewModel = vm,
                        showAds = !adsRemoved,
                        onRemoveAdsClick = { billingManager.launchRemoveAdsFlow() }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        billingManager.endConnection()
        super.onDestroy()
    }
}
