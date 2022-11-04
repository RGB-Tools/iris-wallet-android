package com.iriswallet.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.iriswallet.databinding.ActivityScanBinding
import com.journeyapps.barcodescanner.CaptureManager
import java.lang.reflect.Field

const val ABORT = 101

class ScanActivity : AppCompatActivity() {

    private var _binding: ActivityScanBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var capture: CaptureManager
    private var flashOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        capture = CaptureManager(this, binding.zxingBarcodeScanner)
        if (!hasFlash()) binding.switchFlashlightBtn.visibility = View.GONE
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.setShowMissingCameraPermissionDialog(true)
        capture.decode()
        val viewFinder = binding.zxingBarcodeScanner.viewFinder
        val scannerAlphaField: Field
        try {
            scannerAlphaField = viewFinder.javaClass.getDeclaredField("SCANNER_ALPHA")
            scannerAlphaField.isAccessible = true
            scannerAlphaField[viewFinder] = IntArray(1)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        binding.switchFlashlightBtn.setOnClickListener { switchFlashlight() }
    }

    override fun onResume() {
        super.onResume()
        capture.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return binding.zxingBarcodeScanner.onKeyDown(keyCode, event) ||
            super.onKeyDown(keyCode, event)
    }

    private fun switchFlashlight() {
        if (!flashOn) {
            binding.zxingBarcodeScanner.setTorchOn()
            flashOn = true
            binding.switchFlashlightBtn.imageAlpha = 100
        } else {
            binding.zxingBarcodeScanner.setTorchOff()
            flashOn = false
            binding.switchFlashlightBtn.imageAlpha = 255
        }
    }

    private fun hasFlash(): Boolean {
        return applicationContext.packageManager.hasSystemFeature(
            PackageManager.FEATURE_CAMERA_FLASH
        )
    }
    override fun onSupportNavigateUp(): Boolean {
        setResult(ABORT)
        finish()
        return true
    }
}
