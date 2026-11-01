
package com.akeel.laalteen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var lanternImage: ImageView
    private lateinit var powerButton: View
    private lateinit var cameraManager: CameraManager

    private var cameraId: String? = null
    private var isFlashOn = false

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                turnOnFlashlight()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required for the flashlight",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        lanternImage = findViewById(R.id.lanternImage)
        powerButton = findViewById(R.id.powerButton)

        // Initialize CameraManager
        cameraManager =
            getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Find camera with flashlight
        cameraId = findFlashCamera()

        // Initial state
        showFlashlightOff()

        // Power button click
        powerButton.setOnClickListener {
            toggleFlashlight()
        }
    }

    private fun findFlashCamera(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->

                val characteristics =
                    cameraManager.getCameraCharacteristics(id)

                val hasFlash =
                    characteristics.get(
                        CameraCharacteristics.FLASH_INFO_AVAILABLE
                    ) == true

                val isBackCamera =
                    characteristics.get(
                        CameraCharacteristics.LENS_FACING
                    ) == CameraCharacteristics.LENS_FACING_BACK

                hasFlash && isBackCamera
            }
        } catch (e: CameraAccessException) {
            null
        }
    }

    private fun toggleFlashlight() {

        if (isFlashOn) {
            turnOffFlashlight()
        } else {

            if (cameraId == null) {
                Toast.makeText(
                    this,
                    "Flashlight is not available on this device",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
                return
            }

            turnOnFlashlight()
        }
    }

    private fun turnOnFlashlight() {
        try {
            cameraManager.setTorchMode(cameraId!!, true)

            isFlashOn = true
            showFlashlightOn()

        } catch (e: Exception) {
            isFlashOn = false
            showFlashlightOff()

            Toast.makeText(
                this,
                "Unable to turn on flashlight",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun turnOffFlashlight() {
        try {
            cameraManager.setTorchMode(cameraId!!, false)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Unable to turn off flashlight",
                Toast.LENGTH_SHORT
            ).show()
        } finally {
            isFlashOn = false
            showFlashlightOff()
        }
    }

    private fun showFlashlightOn() {
        lanternImage.setImageResource(R.drawable.laal_teen_on)
    }

    private fun showFlashlightOff() {
        lanternImage.setImageResource(R.drawable.laal_teen_off)
    }

    override fun onResume() {
        super.onResume()

        // Hide system bars for full-screen experience
        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onStop() {
        super.onStop()

        // Turn off physical flashlight when app goes to background
        if (isFlashOn) {
            turnOffFlashlight()
        }
    }
}
