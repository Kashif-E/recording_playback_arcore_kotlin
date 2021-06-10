/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.kotlin.common.helpers

import android.app.Activity
import android.view.WindowManager
import com.google.ar.core.Camera
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState

/** Gets human readibly tracking failure reasons and suggested actions.  */
class TrackingStateHelper(private val activity: Activity) {
    private var previousTrackingState: TrackingState? = null

    /** Keep the screen unlocked while tracking, but allow it to lock when tracking stops.  */
    fun updateKeepScreenOnFlag(trackingState: TrackingState) {
        if (trackingState == previousTrackingState) {
            return
        }
        previousTrackingState = trackingState
        when (trackingState) {
            TrackingState.PAUSED, TrackingState.STOPPED -> activity.runOnUiThread { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            TrackingState.TRACKING -> activity.runOnUiThread { activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }
    }

    companion object {
        private const val INSUFFICIENT_FEATURES_MESSAGE = "Can't find anything. Aim device at a surface with more texture or color."
        private const val EXCESSIVE_MOTION_MESSAGE = "Moving too fast. Slow down."
        private const val INSUFFICIENT_LIGHT_MESSAGE = "Too dark. Try moving to a well-lit area."
        private const val BAD_STATE_MESSAGE = "Tracking lost due to bad internal state. Please try restarting the AR experience."
        private const val CAMERA_UNAVAILABLE_MESSAGE = "Another app is using the camera. Tap on this app or try closing the other one."
        fun getTrackingFailureReasonString(camera: Camera): String {
            val reason = camera.trackingFailureReason
            return when (reason) {
                TrackingFailureReason.NONE -> ""
                TrackingFailureReason.BAD_STATE -> BAD_STATE_MESSAGE
                TrackingFailureReason.INSUFFICIENT_LIGHT -> INSUFFICIENT_LIGHT_MESSAGE
                TrackingFailureReason.EXCESSIVE_MOTION -> EXCESSIVE_MOTION_MESSAGE
                TrackingFailureReason.INSUFFICIENT_FEATURES -> INSUFFICIENT_FEATURES_MESSAGE
                TrackingFailureReason.CAMERA_UNAVAILABLE -> CAMERA_UNAVAILABLE_MESSAGE
            }
            return "Unknown tracking failure reason: $reason"
        }
    }
}