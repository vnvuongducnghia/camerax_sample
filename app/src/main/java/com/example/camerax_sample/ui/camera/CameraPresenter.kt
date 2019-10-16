package com.example.camerax_sample.ui.camera

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.view.TextureView
import android.webkit.MimeTypeMap
import androidx.camera.core.ImageCapture
import androidx.fragment.app.Fragment
import com.example.camerax_sample.data.camera.CameraApp
import java.io.File

class CameraPresenter(private val cameraFragment: CameraContract.View) : CameraContract.Presenter {

    private var camera: CameraApp? = null

    override fun start() {
    }

    override fun end() {
        this.camera?.unregisters()
    }

    override fun createCamera(fragment: Fragment, viewFinder: TextureView) {
        this.camera = CameraApp(fragment, viewFinder).apply {
            registers()
            setLens()
        }
    }

    override fun capturePhoto(context: Context) {
        this.camera?.capturePhoto(object : ImageCapture.OnImageSavedListener {
            override fun onImageSaved(file: File) {
                cameraFragment.capturePhotoCompleted(file)

                //region Refresh other apps
                // Implicit broadcasts will be ignored for devices running API
                // level >= 24, so if you only target 24+ you can remove this statement
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    context.sendBroadcast(
                        Intent(android.hardware.Camera.ACTION_NEW_PICTURE, Uri.fromFile(file))
                    )
                }

                // If the folder selected is an external media directory, this is unnecessary
                // but otherwise other apps will not be able to access our images unless we
                // scan them using [MediaScannerConnection]
                val mimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(file.extension)
                MediaScannerConnection.scanFile(
                    context, arrayOf(file.absolutePath), arrayOf(mimeType), null
                )
                //endregion
            }

            override fun onError(
                imageCaptureError: ImageCapture.ImageCaptureError,
                message: String,
                cause: Throwable?
            ) {
                cameraFragment.capturePhotoError()
            }
        })
    }

    override fun switchCamera() {
        this.camera?.switchCamera()
    }

//endregion

}