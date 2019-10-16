package com.example.camerax_sample.data.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.util.Rational
import android.view.TextureView
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import com.example.camerax_sample.ui.MainActivity
import com.example.camerax_sample.utils.AutoFitPreviewBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val FILE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
private const val FILE_EXTENSION = ".jpg"

// Can not singleton.
class CameraApp(
    private var fragment: Fragment,
    private var viewFinder: TextureView
) {

    private var displayId = -1
    private var lensFacing = CameraX.LensFacing.BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var outputDirectory: File = MainActivity.getOutputDirectory(fragment.requireContext())

    private var displayManager: DisplayManager
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(_displayId: Int) = fragment.view?.let { view ->
            if (displayId == _displayId) {
                preview?.setTargetRotation(view.display.rotation)
                imageCapture?.setTargetRotation(view.display.rotation)
                imageAnalyzer?.setTargetRotation(view.display.rotation)
            }
        } ?: Unit

        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit

    }

    //region Init class
    init {
        displayId = viewFinder.display.displayId
        displayManager = viewFinder.context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        bindCameraUseCases()
    }
    //endregion

    //region Camera use cases
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        //region Init camera preview
        // Set up the view finder use case to display camera preview
        val viewFinderConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            // We request aspect ratio but no resolution to let CameraX optimize our use cases
            setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        // Use the auto-fit preview builder to automatically handle size and orientation changes
        preview = AutoFitPreviewBuilder.build(viewFinderConfig, viewFinder)
        //endregion

        //region Init image capture
        val imageCaptureConfig = ImageCaptureConfig
            .Builder().apply {
                setLensFacing(lensFacing)
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                setTargetAspectRatio(screenAspectRatio)
                setTargetRotation(viewFinder.display.rotation)
            }
            .build()

        imageCapture = ImageCapture(imageCaptureConfig)
        //endregion

        CameraX.bindToLifecycle(fragment.viewLifecycleOwner, preview, imageCapture)

    }
    //endregion

    //region Camera actions
    fun setLens(): CameraApp {
        println("CameraApp.setLens")
        return this
    }

    @SuppressLint("RestrictedApi")
    fun switchCamera() {
        lensFacing = if (CameraX.LensFacing.FRONT == lensFacing) {
            CameraX.LensFacing.BACK
        } else {
            CameraX.LensFacing.FRONT
        }
        try {
            // Only bind use cases if we can query a camera with this orientation
            CameraX.getCameraWithLensFacing(lensFacing)

            // Unbind all use cases and bind them again with the new lens facing configuration
            CameraX.unbindAll()
            bindCameraUseCases()
        } catch (exc: Exception) {
            // Do nothing
        }
    }

    fun capturePhoto(imageSavedListener: ImageCapture.OnImageSavedListener) {
        imageCapture?.let { imageCapture ->
            // Tao output file de luu hinh.
            val photoFile = createFile()

            // Lay meta data trong image capture.
            val metadata = ImageCapture.Metadata().apply {
                isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
            }

            // Do metadata vao file
            imageCapture.takePicture(photoFile, imageSavedListener, metadata)
        }
    }

    //endregion


    //region File actions
    private fun createFile() =
        File(
            outputDirectory,
            SimpleDateFormat(
                FILE_FORMAT,
                Locale.US
            ).format(System.currentTimeMillis()) + FILE_EXTENSION
        )
    //endregion

    fun registers() {
        displayManager.registerDisplayListener(displayListener, null)
    }
    fun unregisters() {
        displayManager.unregisterDisplayListener(displayListener)
    }


}