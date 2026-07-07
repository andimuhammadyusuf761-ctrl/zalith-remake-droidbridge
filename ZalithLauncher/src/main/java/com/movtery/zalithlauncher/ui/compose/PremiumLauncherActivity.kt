package com.movtery.zalithlauncher.ui.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ca.dnamobile.javalauncher.performance.PerformanceCenterActivity

class PremiumLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PremiumLauncherApp(
                onOpenPerformanceCenter = {
                    startActivity(PerformanceCenterActivity.intent(this))
                }
            )
        }
    }
}
