package com.example.canteengo.activities.student

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.canteengo.databinding.ActivityOrderSuccessBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

class OrderSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderSuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val token = intent.getStringExtra("token") ?: "A1001"
        val qrString = intent.getStringExtra("qrString") ?: "CANTEENGO_TOKEN_A1001"
        val orderId = intent.getStringExtra("orderId") ?: ""

        setupUI(token, qrString, orderId)
    }

    private fun setupUI(token: String, qrString: String, orderId: String) {
        binding.tvToken.text = "#$token"

        // Generate QR Code
        try {
            val qrBitmap = generateQRCode(qrString, 400, 400)
            binding.ivQrCode.setImageBitmap(qrBitmap)
        } catch (e: Exception) {
            // QR generation failed, show placeholder
        }

        binding.btnTrackOrder.setOnClickListener {
            val intent = Intent(this, OrderTrackingActivity::class.java)
            intent.putExtra("orderId", orderId)
            startActivity(intent)
        }

        binding.btnBackToHome.setOnClickListener {
            val intent = Intent(this, StudentHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun generateQRCode(text: String, width: Int, height: Int): Bitmap {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            width,
            height
        )

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bitmap
    }

    override fun onBackPressed() {
        // Go back to home instead of previous screen
        val intent = Intent(this, StudentHomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}

