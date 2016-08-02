package net.odiak.camerapreview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE = 100
    private val PERMISSION = Manifest.permission.CAMERA

    private var camera: Camera? = null
    private var isPreviewing = false

    private var previewWidth = 0
    private var previewHeight = 0
    private var cameraRotation = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermissionCompat(PERMISSION) != PERMISSION_GRANTED) {
            requestPermissions(arrayOf(PERMISSION), REQUEST_CODE)
        } else if (packageManager.checkPermission(PERMISSION, packageName) == PERMISSION_GRANTED) {
            onCameraReady()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE) {
            val granted = permissions.zip(grantResults.toTypedArray()).any { pair ->
                val (permission, result) = pair
                permission == PERMISSION && result == PERMISSION_GRANTED
            }
            if (granted) {
                onCameraReady()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        camera?.let {
            if (!isPreviewing) {
                it.startPreview()
                isPreviewing = true
            }
        }
    }

    override fun onPause() {
        super.onPause()

        camera?.let {
            if (isPreviewing) {
                it.stopPreview()
                isPreviewing = false
            }
        }
    }

    private fun onCameraReady() {
        val textureView = findViewById(R.id.preview) as TextureView

        val container = findViewById(R.id.container) as ViewGroup

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(texture: SurfaceTexture?, p1: Int, p2: Int) {
                println("@@ available")
                val cameraId = 0
                camera = Camera.open(cameraId)

                camera?.let {
                    cameraRotation = calculateCameraRotation(cameraId, it)
                    it.setDisplayOrientation(cameraRotation)
                    it.setPreviewTexture(texture)
                    println(it.parameters.supportedPictureSizes.map { it.toStr() })
                    println(it.parameters.supportedPreviewSizes.map { it.toStr() })

                    val previewSize = it.parameters.supportedPreviewSizes[0]!!
                    previewWidth = previewSize.width
                    previewHeight = previewSize.height

                    startPreview(previewWidth, previewHeight, textureView, container)
                }
            }

            override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture?,
                                                     width: Int, height: Int) {

                println("@@ sizeChanged: $width,$height")

                startPreview(previewWidth, previewHeight, textureView, container)
            }

            override fun onSurfaceTextureUpdated(texture: SurfaceTexture?) {
            }

            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture?): Boolean {
                camera?.release()
                camera = null
                isPreviewing = false

                return true
            }
        }
        textureView.visibility = View.VISIBLE
    }

    private fun startPreview(previewWidthRotated: Int, previewHeightRotated: Int, textureView: TextureView,
                             parent: ViewGroup) {
        if (isPreviewing) {
            camera?.stopPreview()
            isPreviewing = false
        }

        camera?.let {
            val p = it.parameters
            p.setPreviewSize(previewWidthRotated, previewHeightRotated)
            it.parameters = p

            it.startPreview()
            isPreviewing = true
        }

        val previewWidth: Int
        val previewHeight: Int
        if (cameraRotation == 90 || cameraRotation == 270) {
            previewWidth = previewHeightRotated
            previewHeight = previewWidthRotated
        } else {
            previewWidth = previewWidthRotated
            previewHeight = previewHeightRotated
        }

        val parentWidth = parent.width
        val parentHeight = parent.height
        if (parentWidth == 0 || parentHeight == 0) return

        val previewRatio = previewWidth.toFloat() / previewHeight
        val parentRatio = parentWidth.toFloat() / parentHeight

        val lp = textureView.layoutParams ?: return
        val prevWidth = lp.width
        val prevHeight = lp.height

        if (previewRatio < parentRatio) {
            lp.width = parentWidth
            lp.height = parentWidth * previewHeight / previewWidth
        } else {
            lp.height = parentHeight
            lp.width = parentHeight * previewWidth / previewHeight
        }
        if (lp.width == prevWidth && lp.height == prevHeight) return

        textureView.requestLayout()
    }

    private fun calculateCameraRotation(cameraId: Int, camera: Camera): Int {
        val cameraInfo = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, cameraInfo)

        val screenRotation = this.windowManager?.defaultDisplay?.rotation ?: 0
        val screenRotationDegrees = when (screenRotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        return if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            val rotation = (cameraInfo.orientation + screenRotationDegrees) % 360
            (360 - rotation) % 360
        } else {
            (cameraInfo.orientation - screenRotationDegrees + 360) % 360
        }
    }

    private fun Camera.Size.toStr() = "${width}x${height}"

    private fun Context.checkSelfPermissionCompat(permission: String): Int {
        return ContextCompat.checkSelfPermission(this, permission)
    }
}
