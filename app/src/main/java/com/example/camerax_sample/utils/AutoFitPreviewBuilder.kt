/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.camerax_sample.utils

import android.content.Context
import android.graphics.Matrix
import android.hardware.display.DisplayManager
import android.util.Size
import android.view.*
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.roundToInt

/**
 * Tu dong resizes va phan ung xoay de thay doi cau hinh
 */
class AutoFitPreviewBuilder private constructor(
    config: PreviewConfig, viewFinderRef: WeakReference<TextureView>
) {

    /** Public instance of preview use-case which can be used by consumers of this adapter */
    val preview: Preview = Preview(config)

    //region Internal variable
    /** Internal variable used to keep track of the use case's output rotation */
    private var bufferRotation: Int = 0
    private var viewFinderRotation: Int? = null

    /** Internal variable used to keep track of the use-case's output dimension */
    private var bufferDimens: Size = Size(0, 0)
    private var viewFinderDimens: Size = Size(0, 0)

    /** Internal variable used to keep track of the view's display */
    private var viewFinderDisplay: Int = -1

    /** Internal reference of the [DisplayManager] */
    private lateinit var displayManager: DisplayManager

    // Lang nghe thiet bi xoay
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            val textureView = viewFinderRef.get() ?: return
            if (displayId == viewFinderDisplay) {
                val display = displayManager.getDisplay(displayId)
                val rotation = getDisplaySurfaceRotation(display)
                updateTransform(textureView, rotation, bufferDimens, viewFinderDimens)
            }
        }

        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
    }

    //endregion

    //region Init class

    init {

        //region Moi lan preview update thi tinh toan lai layout.
        preview.onPreviewOutputUpdateListener = Preview.OnPreviewOutputUpdateListener {
            val mTextureView = viewFinderRef.get() ?: return@OnPreviewOutputUpdateListener

            // De cap nhat la surface texture chung ta xoa no va add lai
            val parent = mTextureView.parent as ViewGroup
            parent.removeView(mTextureView)
            parent.addView(mTextureView, 0)

            // Roi cap nhat texture ben trong no
            mTextureView.surfaceTexture = it.surfaceTexture

            // Ap dung nhung bien doi co lien quan
            bufferRotation = it.rotationDegrees
            val rotation = getDisplaySurfaceRotation(mTextureView.display)
            updateTransform(mTextureView, rotation, it.textureSize, viewFinderDimens)
        }
        //endregion

        //region Moi lan texture view thay doi thi tinh toan lai layout.
        val textureView = viewFinderRef.get()
            ?: throw IllegalArgumentException("Tham chieu khong hop le de su dung viewFinder")

        // get info display and rotaion from texture view
        viewFinderDisplay = textureView.display.displayId
        viewFinderRotation = getDisplaySurfaceRotation(textureView.display) ?: 0
        textureView.addOnLayoutChangeListener { view, left, top, right, bottom, _, _, _, _ ->
            val mTextureView = view as TextureView
            val newViewFinderDimens = Size(right - left, bottom - top)
            val rotation = getDisplaySurfaceRotation(mTextureView.display)
            updateTransform(mTextureView, rotation, bufferDimens, newViewFinderDimens)
        }
        //endregion

        //region Moi lan xoay thiet bi thi tinh toan lai layout.
        displayManager =
            textureView.context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        // khong lang nghe khi thoat man hinh
        textureView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View?) =
                displayManager.registerDisplayListener(displayListener, null)

            override fun onViewDetachedFromWindow(view: View?) =
                displayManager.unregisterDisplayListener(displayListener)
        })
        //endregion

    }

    companion object {
        fun build(config: PreviewConfig, viewFinder: TextureView) =
            AutoFitPreviewBuilder(config, WeakReference(viewFinder)).preview
    }

    //endregion


    // Ho tro lay goc cua man hinh
    private fun getDisplaySurfaceRotation(display: Display?) = when (display?.rotation) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> null
    }

    private fun updateTransform(
        textureView: TextureView?, rotation: Int?,
        newBufferDimens: Size,
        newViewFinderDimens: Size
    ) {
        val texture = textureView ?: return

        if (rotation == viewFinderRotation &&
            Objects.equals(newBufferDimens, bufferDimens) &&
            Objects.equals(newViewFinderDimens, viewFinderDimens)
        )
            return


        if (rotation == null) return
        else viewFinderRotation = rotation

        if (newBufferDimens.width == 0 || newBufferDimens.height == 0) return
        else bufferDimens = newBufferDimens

        if (newViewFinderDimens.width == 0 || newViewFinderDimens.height == 0) return
        else viewFinderDimens = newViewFinderDimens

        // Tinh toan matrix
        val matrix = Matrix()
        val centerX = viewFinderDimens.width / 2f
        val centerY = viewFinderDimens.height / 2f
        matrix.postRotate(-viewFinderRotation!!.toFloat(), centerX, centerY)

        // Xoay tuong ung voi chieu xoay dien thoai
        val bufferRatio = bufferDimens.height / bufferDimens.width.toFloat()
        val scaledWidth: Int
        val scaledHeight: Int

        if (viewFinderDimens.width > viewFinderDimens.height) {
            scaledHeight = viewFinderDimens.width
            scaledWidth = (viewFinderDimens.width * bufferRatio).roundToInt()
        } else {
            scaledHeight = viewFinderDimens.height
            scaledWidth = (viewFinderDimens.height * bufferRatio).roundToInt()
        }

        val xScale = scaledWidth / viewFinderDimens.width.toFloat()
        val yScale = scaledHeight / viewFinderDimens.height.toFloat()

        // Scale input buffers to fill the view finder
        matrix.preScale(xScale, yScale, centerX, centerY)

        // Finally, apply transformations to our TextureView
        texture.setTransform(matrix)
    }
}