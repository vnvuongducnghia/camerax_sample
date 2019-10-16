package com.example.camerax_sample.ui.camera

import android.content.Context
import android.view.TextureView
import androidx.fragment.app.Fragment
import com.example.camerax_sample.ui.BasePresenter
import com.example.camerax_sample.ui.BaseView
import java.io.File

interface CameraContract {

    interface Presenter : BasePresenter {
        fun createCamera(fragment: Fragment, viewFinder: TextureView)
        fun switchCamera()
        fun capturePhoto(context: Context)
    }

    interface View :
        BaseView<Presenter> {
        fun capturePhotoCompleted(photoFile: File)
        fun capturePhotoError()
    }
}