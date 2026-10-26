package com.test.design.presentation.ivi.glanceables

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Which half of the screen the climate and vehicle quarters occupy.
 * Both panel activities share one process, so this stays in step.
 */
internal object ClimateVehicleSide {
    var onLeft by mutableStateOf(true)
}

/**
 * Opens the full climate or vehicle screen outside the widget panel.
 * Application context plus [Intent.FLAG_ACTIVITY_NEW_TASK] keeps the launch off
 * the panel root task, so it comes up as a normal activity.
 */
internal fun openFullActivity(
    context: Context,
    activityClass: Class<out Activity>,
    action: String,
) {
    val app = context.applicationContext
    app.startActivity(
        Intent(app, activityClass).apply {
            this.action = action
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        },
    )
}

/** Asks Scalable UI to slide both quarters in from the other side. */
internal fun slideQuartersFromOtherSide(activity: Activity) {
    val target = if (ClimateVehicleSide.onLeft) {
        SlideFromRightActivity::class.java
    } else {
        SlideFromLeftActivity::class.java
    }
    ClimateVehicleSide.onLeft = !ClimateVehicleSide.onLeft
    activity.startActivity(
        Intent(activity, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/**
 * Invisible task. Its panel open event is what moves the quarters.
 * The activity itself never draws.
 */
open class SlideTriggerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed({ finish() }, 500)
    }
}

class SlideFromLeftActivity : SlideTriggerActivity()

class SlideFromRightActivity : SlideTriggerActivity()
