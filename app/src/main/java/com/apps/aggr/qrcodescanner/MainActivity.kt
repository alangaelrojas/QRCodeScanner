package com.apps.aggr.qrcodescanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.util.isEmpty
import com.apps.aggr.qrcodescanner.databinding.*
import com.apps.aggr.qrcodescanner.utils.animateUp
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    private lateinit var barcodeDetector: BarcodeDetector
    private lateinit var cameraSource: CameraSource
    private lateinit var binding: ActivityMainBinding

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RequestCameraPermissionID -> {
                if (isPermissionGranted(grantResults)) {
                    startCamera()
                }
            }
            RequestSendSMSPermissionID -> {
                if (isPermissionGranted(grantResults)) {
                    if (isPermissionNotGranted(Manifest.permission.SEND_SMS)
                    ) {
                        return
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.try_send_message_again),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            RequestCallPhonePermissionID -> {
                if (isPermissionGranted(grantResults)) {
                    if (isPermissionNotGranted(Manifest.permission.CALL_PHONE)
                    ) {
                        return
                    } else {
                        Toast.makeText(
                            this,
                            getText(R.string.try_call_again),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(binding.root)

        barcodeDetector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()

        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(1280, 960)
            .setAutoFocusEnabled(true)
            .build()

        val callback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                startCamera()
            }

            override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {}
            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                cameraSource.stop()
            }
        }

        val processor = object : Detector.Processor<Barcode> {
            override fun release() {}

            override fun receiveDetections(detections: Detections<Barcode>) {
                val qrCodes = detections.detectedItems
                if (qrCodes.isEmpty()) {
                    return
                }
                binding.txtResult.post {
                    val barcode: Barcode = qrCodes.valueAt(0)
                    performActionWithBarcode(barcode)
                }
            }
        }

        binding.cameraPreview.holder.addCallback(callback)
        barcodeDetector.setProcessor(processor)
    }

    private fun performActionWithBarcode(barcode: Barcode) {
        when (barcode.valueFormat) {
            Barcode.EMAIL -> processBarcodeAsEmail(barcode.email)
            Barcode.URL -> processBarcodeAsUrl(barcode.url)
            Barcode.PHONE -> processBarcodeAsPhone(barcode.phone)
            Barcode.SMS -> processBarcodeAsSms(barcode.sms)
            Barcode.WIFI -> processBarcodeAsWifi(barcode.wifi)
            else -> processBarcodeAsText(barcode.displayValue)
        }
    }

    //region BarcodeOperations
    private fun processBarcodeAsEmail(email: Barcode.Email) {
        val layout = binding.lyItems
        val inflater = LayoutInflater.from(applicationContext)
        layout.removeAllViews()
        val binding = ItemEmailBinding.inflate(inflater)
        val address = email.address
        val subject = email.subject
        val bodyMessage = email.body
        binding.tvResultEmails.text = address
        binding.tvResultSubject.text = subject
        binding.tvResultMessage.text = bodyMessage
        binding.btnSendEmail.setOnClickListener {
            sendEmail(
                arrayOf(address),
                subject,
                bodyMessage
            )
        }
        layout.addView(binding.root)
        layout.animateUp(this)
    }

    private fun processBarcodeAsUrl(url: Barcode.UrlBookmark) {
        val layout = binding.lyItems
        val inflater = LayoutInflater.from(applicationContext)
        layout.removeAllViews()
        val binding = ItemUrlBinding.inflate(inflater)
        binding.tvResultUrl.text = url.url
        binding.btnOpenUrl.setOnClickListener {
            openUrl(url)
        }
        layout.addView(binding.root)
        layout.animateUp(this)
    }

    private fun processBarcodeAsPhone(phone: Barcode.Phone) {
        val layout = binding.lyItems
        val inflater = LayoutInflater.from(applicationContext)
        layout.removeAllViews()
        val binding = ItemPhoneBinding.inflate(inflater)
        binding.tvResultPhone.text = phone.number
        val phoneNumber = "+52 ${phone.number}"
        binding.btnCall.setOnClickListener {
            makeCall(phoneNumber)
        }
        layout.addView(binding.root)
        layout.animateUp(this)
    }

    private fun processBarcodeAsWifi(wifi: Barcode.WiFi) {
        val layout = binding.lyItems
        val inflater = LayoutInflater.from(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            addWifi(wifi)
            return
        }
        layout.removeAllViews()
        val binding = ItemTextBinding.inflate(inflater)
        binding.tvResultText.text = wifi.ssid + "\n" + wifi.password
        binding.btnCopyText.setOnClickListener {
            copyTextToClipboard(wifi.password)
        }
        layout.addView(binding.root)
    }

    private fun processBarcodeAsText(text: String) {
        val layout = binding.lyItems
        val inflater = LayoutInflater.from(applicationContext)
        layout.removeAllViews()
        val binding = ItemTextBinding.inflate(inflater)
        binding.tvResultText.text = text
        binding.btnCopyText.setOnClickListener {
            copyTextToClipboard(text)
        }
        layout.addView(binding.root)
        layout.animateUp(this)
    }

    private fun processBarcodeAsSms(sms: Barcode.Sms) {
        val layout = binding.lyItems
        val inflater = LayoutInflater.from(applicationContext)
        layout.removeAllViews()
        val binding = ItemSmsBinding.inflate(inflater)

        val phoneNumber = sms.phoneNumber
        val message = sms.message

        binding.tvResultContact.text = phoneNumber
        binding.tvResultMessage.text = message

        binding.btnSendSms.setOnClickListener {
            sendSMS(phoneNumber, message)
        }

        layout.addView(binding.root)
        layout.animateUp(this)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun addWifi(barcodeWifi: Barcode.WiFi) {
        val ssid: String = barcodeWifi.ssid
        val pass: String = barcodeWifi.password
        val encryptionType: Int = barcodeWifi.encryptionType

        val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(pass)
            .build()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(wifiNetworkSpecifier)
            .build()

        val connectivityManager =
            this.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, NetworkCallback())

    }

    private fun makeCall(phoneNumber: String) {
        if (isPermissionNotGranted(Manifest.permission.CALL_PHONE)) {
            requestPermissions(RequestCallPhonePermissionID, Manifest.permission.CALL_PHONE)
            return
        }

        val i = Intent(Intent.ACTION_CALL)
        i.data = Uri.parse("tel:$phoneNumber")
        startActivity(i);
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        if (isPermissionNotGranted(Manifest.permission.SEND_SMS)) {
            requestPermissions(RequestSendSMSPermissionID, Manifest.permission.SEND_SMS)
            return
        }

        val smsManager: SmsManager = SmsManager.getDefault()
        val pendingIntent: PendingIntent
        val sent = "SMS_SENT"

        pendingIntent = PendingIntent.getBroadcast(this, 0, Intent(sent), 0)
        smsManager.sendTextMessage(phoneNumber, null, message, pendingIntent, null);
    }

    private fun sendEmail(emails: Array<String>, subject: String, body: String) {
        val mailer = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, emails)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        startActivity(Intent.createChooser(mailer, "Send email..."))
    }

    private fun copyTextToClipboard(textToCopy: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", textToCopy)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_LONG).show()
    }

    private fun openUrl(url: Barcode.UrlBookmark) {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url.url)
        startActivity(i)
    }
    //endregion

    //region Permissions
    private fun requestPermissions(requestCode: Int, vararg permissions: String) {
        ActivityCompat.requestPermissions(
            this@MainActivity,
            permissions,
            requestCode
        )
    }

    private fun isPermissionNotGranted(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            applicationContext,
            permission
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun isPermissionGranted(grantResults: IntArray): Boolean =
        grantResults[0] == PackageManager.PERMISSION_GRANTED
    //endregion

    private fun startCamera() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(RequestCameraPermissionID, Manifest.permission.CAMERA)
            return
        }
        cameraSource.start(binding.cameraPreview.holder)
    }

    companion object {
        private const val RequestCameraPermissionID = 1001
        private const val RequestSendSMSPermissionID = 1002
        private const val RequestCallPhonePermissionID = 1003
    }

}