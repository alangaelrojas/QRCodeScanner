package com.apps.aggr.qrcodescanner

import android.Manifest
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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.util.isEmpty
import com.apps.aggr.qrcodescanner.databinding.ActivityMainBinding
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector

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
                            "Intente enviar el mensaje nuevamente por favor",
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
                            "Intente llamar nuevamente por favor",
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
        val layout = binding.lyItems
        when (barcode.valueFormat) {
            Barcode.EMAIL -> {
                barcode.email?.let {
                    layout.removeAllViews()
                    val view = LayoutInflater.from(applicationContext)
                        .inflate(R.layout.item_email, null)
                    val tvContacto = view.findViewById<TextView>(R.id.tv_contacto)
                    val tvAsunto = view.findViewById<TextView>(R.id.tv_asunto)
                    val tvMensaje =
                        view.findViewById<TextView>(R.id.tv_cuerpoMensaje)
                    val btnEnviar = view.findViewById<Button>(R.id.btn_enviarEmail)

                    tvContacto.text = it.address
                    tvAsunto.text = it.subject
                    tvMensaje.text = it.body

                    btnEnviar.setOnClickListener {
                        sendEmail(
                            arrayOf(tvContacto.text.toString()),
                            tvAsunto.text.toString(),
                            tvMensaje.text.toString()
                        )
                    }

                    layout.addView(view)
                }
            }
            Barcode.URL -> {
                barcode.url?.let {
                    layout.removeAllViews()
                    val view = LayoutInflater.from(applicationContext)
                        .inflate(R.layout.item_url, null)
                    val tvUrl = view.findViewById<TextView>(R.id.tv_url)
                    val abrirLink = view.findViewById<Button>(R.id.btn_abrirUrl)

                    tvUrl.text = it.url
                    val url: String = it.url

                    abrirLink.setOnClickListener {
                        val i = Intent(Intent.ACTION_VIEW)
                        i.data = Uri.parse(url)
                        startActivity(i)
                    }
                    layout.addView(view)
                }
            }
            Barcode.PHONE -> {
                barcode.phone?.let {
                    layout.removeAllViews()
                    val view = LayoutInflater.from(applicationContext)
                        .inflate(R.layout.item_phone, null)
                    val tvPhone = view.findViewById<TextView>(R.id.tv_phone)
                    val btnCall = view.findViewById<Button>(R.id.btn_llamar)

                    tvPhone.text = it.number
                    val phone = "+52 ${it.number}"
                    btnCall.setOnClickListener {
                        val i = Intent(Intent.ACTION_CALL)
                        i.data = Uri.parse("tel:$phone");
                        if (ActivityCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.CALL_PHONE
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                this@MainActivity,
                                arrayOf(Manifest.permission.CALL_PHONE),
                                RequestSendSMSPermissionID
                            )
                        } else {
                            startActivity(i);
                        }
                    }
                    layout.addView(view)
                }
            }
            Barcode.SMS -> {
                barcode.sms?.let {
                    layout.removeAllViews()
                    val view = LayoutInflater.from(applicationContext)
                        .inflate(R.layout.item_sms, null)
                    val tvContacto = view.findViewById<TextView>(R.id.tv_contacto)
                    val tvMensaje = view.findViewById<TextView>(R.id.tv_mensaje)
                    val btnEnviar = view.findViewById<Button>(R.id.btn_enviarSms)

                    tvContacto.text = it.phoneNumber
                    tvMensaje.text = it.message

                    btnEnviar.setOnClickListener {
                        sendSMS(
                            tvContacto.text.toString(),
                            tvMensaje.text.toString()
                        )
                    }

                    layout.addView(view)
                }
            }
            Barcode.WIFI -> {
                barcode.wifi?.let { wifi ->
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        layout.removeAllViews()
                        val view = LayoutInflater.from(applicationContext)
                            .inflate(R.layout.item_text, null)
                        val tvText = view.findViewById<TextView>(R.id.tv_text)
                        val copyText = view.findViewById<Button>(R.id.btn_copyText)

                        tvText.text = wifi.ssid + "\n" + wifi.password

                        copyText.setOnClickListener {
                            copyTextToClipboard(wifi.password)
                        }
                        layout.addView(view)
                        return
                    }
                    addWifi(wifi)
                }
            }
            else -> {
                barcode.displayValue?.let { text ->
                    layout.removeAllViews()
                    val view = LayoutInflater.from(applicationContext)
                        .inflate(R.layout.item_text, null)
                    val tvText = view.findViewById<TextView>(R.id.tv_text)
                    val copyText = view.findViewById<Button>(R.id.btn_copyText)

                    tvText.text = text

                    copyText.setOnClickListener {
                        copyTextToClipboard(text)
                    }
                    layout.addView(view)
                }
            }
        }
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

    private fun sendSMS(phoneNo: String, msg: String) {
        if (isPermissionNotGranted(Manifest.permission.SEND_SMS)) {
            requestPermissions(Manifest.permission.SEND_SMS)
            return
        }

        val smsManager: SmsManager = SmsManager.getDefault()
        val pendingIntent: PendingIntent
        val sent = "SMS_SENT"

        pendingIntent = PendingIntent.getBroadcast(this, 0, Intent(sent), 0)
        smsManager.sendTextMessage(phoneNo, null, msg, pendingIntent, null);
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

    private fun requestPermissions(vararg permissions: String) {
        ActivityCompat.requestPermissions(
            this@MainActivity,
            permissions,
            RequestSendSMSPermissionID
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

    private fun startCamera() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(Manifest.permission.CAMERA)
            return
        }
        cameraSource.start(binding.cameraPreview.holder)
    }

    private fun copyTextToClipboard(textToCopy: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", textToCopy)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val RequestCameraPermissionID = 1001
        private const val RequestSendSMSPermissionID = 1002
        private const val RequestCallPhonePermissionID = 1003
    }

}