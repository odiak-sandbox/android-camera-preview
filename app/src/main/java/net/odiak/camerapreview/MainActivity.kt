package net.odiak.camerapreview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE = 100
    private val PERMISSION = Manifest.permission.CAMERA

    private var camera: Camera? = null
    private var isPreviewing = false

    private val pictureWidth = 4032
    private val pictureHeight = 3024

    private val previewWidth = 1920
    private val previewHeight = 1080

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val textView = findViewById(R.id.textView) as TextView
        textView.text = "${pictureWidth}x${pictureHeight}, ${previewWidth}x${previewHeight}"

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
        val surfaceView = findViewById(R.id.preview) as SurfaceView
        surfaceView.visibility = View.VISIBLE

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder?) {
                camera = Camera.open()

                val rotation = windowManager.defaultDisplay.rotation
                val cameraInfo = Camera.CameraInfo()
                Camera.getCameraInfo(0, cameraInfo)

                camera?.let {
//                    it.setDisplayOrientation((cameraInfo.orientation - rotation * 90 + 360) % 360)
                    it.setDisplayOrientation(90)
                    it.setPreviewDisplay(holder)
                    println(it.parameters.supportedPictureSizes.map { it.toStr() })
                    println(it.parameters.supportedPreviewSizes.map { it.toStr() })
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int,
                                        height: Int) {
                println("@@ surfaceChanged: $width,$height")

                if (isPreviewing) {
                    camera?.stopPreview()
                    camera?.setPreviewDisplay(holder)
                }

                camera?.let {
                    val p = it.parameters
                    val previewSize = findBestPreviewSize(p.supportedPreviewSizes, width, height)
//                    println(previewSize.toStr())
//                    println(p.pictureSize.toStr())
//                    p.setPreviewSize(previewSize.width, previewSize.height)
                    p.setPictureSize(pictureWidth, pictureHeight)
                    p.setPreviewSize(previewWidth, previewHeight)
                    it.parameters = p

                    it.startPreview()
                    isPreviewing = true

                    val w = previewWidth
                    val h = previewHeight
                    val newHeight = h * width / w
                    if (newHeight != height) {
                        val lp = surfaceView.layoutParams
                        lp?.height = newHeight
                        surfaceView.requestLayout()
                    }
                }
            }

            override fun surfaceDestroyed(p0: SurfaceHolder?) {
                camera?.release()
                camera = null
                isPreviewing = false
            }
        })
    }

    private fun findBestPreviewSize(previewSizes: List<Camera.Size>, width: Int, height: Int): Camera.Size {
        val ratio = width / height.toFloat()
        val bestSize = previewSizes.withIndex().minBy { p ->
            val (i, size) = p
            Math.abs(size.width / size.height.toFloat() - ratio)
        }?.value
        return bestSize ?: error("no size")
    }

    private fun Camera.Size.toStr() = "${width}x${height}"

    private fun Context.checkSelfPermissionCompat(permission: String): Int {
        return ContextCompat.checkSelfPermission(this, permission)
    }
}
