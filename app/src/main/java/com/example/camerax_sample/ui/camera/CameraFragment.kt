package com.example.camerax_sample.ui.camera


import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.camerax_sample.R
import com.example.camerax_sample.ui.MainActivity
import com.example.camerax_sample.ui.gallery.EXTENSION_WHITELIST
import com.example.camerax_sample.ui.permission.PermissionsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * A simple [Fragment] subclass.
 */
class CameraFragment : Fragment(), CameraContract.View {

    // Presenter
    override lateinit var presenter: CameraContract.Presenter

    // View
    private lateinit var rootView: ConstraintLayout
    private lateinit var viewFinder: TextureView

    // Other
    private lateinit var outputDirectory: File

    //region CameraFragment Lifecycle

    override fun onAttach(context: Context) {
        super.onAttach(context)
        presenter = CameraPresenter(this)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.start()

        // Mark this as a retain fragment, so the lifecycle does not get restarted on config change
        retainInstance = true

        // Determine the output directory
        outputDirectory = MainActivity.getOutputDirectory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_camera, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView = view as ConstraintLayout
        viewFinder = rootView.findViewById(R.id.view_finder)

        // Wait viewFinder
        viewFinder.post {
            applyCameraUi()
            presenter.createCamera(this, viewFinder)

            // In the background, load latest photo taken (if any) for gallery thumbnail
            lifecycleScope.launch(Dispatchers.IO) {
                outputDirectory
                    .listFiles { file -> EXTENSION_WHITELIST.contains(file.extension.toUpperCase()) }
                    .sorted().reversed().firstOrNull()?.let { setGalleryThumbnail(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handleCameraPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.end()
    }
    //endregion

    //region CameraMethods

    private fun handleCameraPermissions() {
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.camera_container)
                .navigate(CameraFragmentDirections.actionCameraFragmentToPermissionFragment())
        }
    }

    private fun applyCameraUi() {

        // Xoa ui neu co thay doi
        rootView.findViewById<ConstraintLayout>(R.id.camera_ui_container)
            ?.let { rootView.removeView(it) }

        // Add camera ui to root view.
        val controls = View.inflate(requireContext(), R.layout.camera_ui_container, rootView)

        // Capture photo
        controls.findViewById<ImageButton>(R.id.camera_capture_button).setOnClickListener {
            presenter.capturePhoto(requireContext())
        }

        // Switch camera
        controls.findViewById<ImageButton>(R.id.camera_switch_button).setOnClickListener {
            presenter.switchCamera()
        }

        // Photo view
        controls.findViewById<ImageButton>(R.id.photo_view_button).setOnClickListener {
            Navigation
                .findNavController(requireActivity(), R.id.fragment_container)
                .navigate(CameraFragmentDirections.actionCameraFragmentToGalleryFragment(outputDirectory.absolutePath))

        }
    }

    override fun capturePhotoCompleted(photoFile: File) {
        setGalleryThumbnail(photoFile)
    }

    override fun capturePhotoError() {}

    //endregion

    private fun setGalleryThumbnail(file: File) {
        // Reference of the view that holds the gallery thumbnail
        val thumbnail = rootView.findViewById<ImageButton>(R.id.photo_view_button)

        // Run the operations in the view's thread
        thumbnail.post {

            // Remove thumbnail padding
            thumbnail.setPadding(resources.getDimension(R.dimen.stroke_small).toInt())

            // Load thumbnail into circular button using Glide
            Glide.with(thumbnail)
                .load(file)
                .apply(RequestOptions.circleCropTransform())
                .into(thumbnail)
        }
    }


}
