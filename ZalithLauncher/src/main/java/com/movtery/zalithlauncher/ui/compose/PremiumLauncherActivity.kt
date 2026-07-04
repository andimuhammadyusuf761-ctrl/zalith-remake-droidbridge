package com.movtery.zalithlauncher.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class PremiumLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PremiumLauncherApp(
                onOpenPerformanceCenter = {
                    // Connect this callback to the existing Performance Center fragment/activity during rollout.
                }
            )
        }
    }
}
