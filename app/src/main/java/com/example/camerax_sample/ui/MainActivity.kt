package com.example.camerax_sample.ui

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.camerax_sample.R
import com.example.camerax_sample.utils.FLAGS_FULLSCREEN
import java.io.File

const val KEY_EVENT_ACTION = "key_event_action"
const val KEY_EVENT_EXTRA = "key_event_extra"
private const val IMMERSIVE_FLAG_TIMEOUT = 500L

class MainActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout

    //region Init Class
    companion object {

        // Su dung thu muc media file nen no ton tai, cac file hinh se duoc luu o day
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.getString(R.string.app_name)).apply { mkdir() }
            }
            return if (mediaDir != null && mediaDir.exists()) {
                mediaDir
            } else {
                appContext.filesDir
            }
        }
    }

    //endregion

    //region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        container = findViewById(R.id.fragment_container)
    }

    override fun onResume() {
        super.onResume()
        container.postDelayed({
            container.systemUiVisibility = FLAGS_FULLSCREEN
        }, IMMERSIVE_FLAG_TIMEOUT)
    }
    //endregion
}
