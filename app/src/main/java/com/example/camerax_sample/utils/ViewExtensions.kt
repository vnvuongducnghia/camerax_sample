package com.example.camerax_sample.utils

import android.os.Build
import android.view.DisplayCutout
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog

/** Ket hop tat ca flags yeu cau de activity full screen*/
const val FLAGS_FULLSCREEN =
    View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

/** Milliseconds su dung cho animations*/
const val ANIMATION_FAS_MILLIS = 50L
const val ANIMATION_SLOW_MILLIS = 100L

/** Cutout (i.e. notch)  */
@RequiresApi(Build.VERSION_CODES.P)
fun View.padWithDisplayCutout() {

    fun doPadding(cutout: DisplayCutout) = setPadding(
        cutout.safeInsetLeft, cutout.safeInsetTop,
        cutout.safeInsetRight, cutout.safeInsetBottom
    )

    // Apply padding using the display cutout designated "safe area"
    rootWindowInsets?.displayCutout?.let { doPadding(it) }

    // Lang nghe rootWindowInset da san sang.
    setOnApplyWindowInsetsListener { view, insets ->
        insets.displayCutout?.let { doPadding(it) }
        insets
    }
}

fun AlertDialog.showImmersive() {

    // Thiet lap dialog khong the focus
    window?.setFlags(
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    )

    // Lam dialog full screen
    window?.decorView?.systemUiVisibility = FLAGS_FULLSCREEN

    show()

    // Xoa tinh nang khong the focus
    window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
}