package com.apps.aggr.qrcodescanner

import android.Manifest
import android.app.PendingIntent
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
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.apps.aggr.qrcodescanner.databinding.ActivityMainBinding
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.IOException


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var barcodeDetector: BarcodeDetector
    private lateinit var cameraSource: CameraSource
    private lateinit var binding: ActivityMainBinding

    private val RequestCameraPermissionID = 1001
    private val RequestSendSMSPermissionID = 1002
    private val RequestCallPhonePermissionID = 1003
    private var hasVibrate = true


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            RequestCameraPermissionID -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                    try {
                        cameraSource.start(binding.cameraPreview.holder)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            RequestSendSMSPermissionID -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                        return
                    } else {
                        Toast.makeText(this, "Intente enviar el mensaje nuevamente por favor", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            RequestCallPhonePermissionID -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        return
                    } else {
                        Toast.makeText(this, "Intente llamar nuevamente por favor", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cameraPreview.setOnClickListener(this)

        barcodeDetector = BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build()

        cameraSource = CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1280, 960)
                .setAutoFocusEnabled(true)
                .build()

        //Add Event
        binding.cameraPreview.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    //Request permission
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.CAMERA), RequestCameraPermissionID)
                    return
                }
                try {
                    cameraSource.start(binding.cameraPreview.holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {}
            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                cameraSource.stop()
            }
        })
        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {}

            @RequiresApi(Build.VERSION_CODES.O)
            override fun receiveDetections(detections: Detections<Barcode>) {
                val qrcodes = detections.detectedItems
                if (qrcodes.size() != 0 && hasVibrate) {
                    binding.txtResult.post {
                        val vibrator: Vibrator = applicationContext.getSystemService(VIBRATOR_SERVICE) as Vibrator
                        vibrator.vibrate(VibrationEffect.createOneShot(500, 1))
                        hasVibrate = false

                        val barcode: Barcode = qrcodes.valueAt(0)

                        val layout = binding.lyItems

                        when(barcode.valueFormat){
                            Barcode.EMAIL -> {
                                barcode.email?.let {
                                    layout.removeAllViews()
                                    val view = LayoutInflater.from(applicationContext).inflate(R.layout.item_email, null)
                                    val tvContacto = view.findViewById<TextView>(R.id.tv_contacto)
                                    val tvAsunto = view.findViewById<TextView>(R.id.tv_asunto)
                                    val tvMensaje = view.findViewById<TextView>(R.id.tv_cuerpoMensaje)
                                    val btnEnviar = view.findViewById<Button>(R.id.btn_enviarEmail)

                                    tvContacto.text = it.address
                                    tvAsunto.text = it.subject
                                    tvMensaje.text = it.body

                                    btnEnviar.setOnClickListener {
                                        sendEmail(arrayOf(tvContacto.text.toString()), tvAsunto.text.toString() ,tvMensaje.text.toString())
                                    }

                                    layout.addView(view)
                                }
                            }
                            Barcode.URL -> {
                                barcode.url?.let {
                                    layout.removeAllViews()
                                    val view = LayoutInflater.from(applicationContext).inflate(R.layout.item_url, null)
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
                                    val view = LayoutInflater.from(applicationContext).inflate(R.layout.item_phone, null)
                                    val tvPhone = view.findViewById<TextView>(R.id.tv_phone)
                                    val btnCall = view.findViewById<Button>(R.id.btn_llamar)

                                    tvPhone.text = it.number
                                    val phone = "+52 ${it.number}"
                                    btnCall.setOnClickListener {
                                        val i = Intent(Intent.ACTION_CALL)
                                        i.data = Uri.parse("tel:$phone");
                                        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.CALL_PHONE), RequestSendSMSPermissionID)
                                        }
                                        else {
                                            startActivity(i);
                                        }
                                    }
                                    layout.addView(view)
                                }
                            }
                            Barcode.SMS -> {
                                barcode.sms?.let {
                                    layout.removeAllViews()
                                    val view = LayoutInflater.from(applicationContext).inflate(R.layout.item_sms, null)
                                    val tvContacto = view.findViewById<TextView>(R.id.tv_contacto)
                                    val tvMensaje = view.findViewById<TextView>(R.id.tv_mensaje)
                                    val btnEnviar = view.findViewById<Button>(R.id.btn_enviarSms)

                                    tvContacto.text = it.phoneNumber
                                    tvMensaje.text = it.message

                                    btnEnviar.setOnClickListener {
                                        sendSMS(tvContacto.text.toString(), tvMensaje.text.toString())
                                    }

                                    layout.addView(view)
                                }
                            }
                            Barcode.WIFI -> {
                                barcode.wifi?.let {
                                    val encryption = it.encryptionType
                                    addWifi(it.ssid, it.password, encryption)
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onClick(view: View?) {
        when(view?.id){
            R.id.cameraPreview -> {
                hasVibrate = true
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addWifi(ssid: String, pass: String, encryptionType: Int){

        val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(pass)
                .build()

        val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build()

        val connectivityManager = this.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, NetworkCallback())

    }

    private fun sendSMS(phoneNo: String, msg: String) {
        try {
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.SEND_SMS), RequestSendSMSPermissionID)
                return
            }
            val smsManager: SmsManager = SmsManager.getDefault()

            val sentPI: PendingIntent
            val SENT = "SMS_SENT"

            sentPI = PendingIntent.getBroadcast(this, 0, Intent(SENT), 0)
            smsManager.sendTextMessage(phoneNo, null, msg, sentPI, null);

        } catch (ex: Exception) {
            Toast.makeText(applicationContext, ex.message.toString(), Toast.LENGTH_LONG).show()
            ex.printStackTrace()
        }
    }

    private fun sendEmail(emails: Array<String>, subject: String, body: String){
        val mailer = Intent(Intent.ACTION_SEND)
        mailer.type = "message/rfc822"
        mailer.putExtra(Intent.EXTRA_EMAIL, emails)
        mailer.putExtra(Intent.EXTRA_SUBJECT, subject)
        mailer.putExtra(Intent.EXTRA_TEXT, body)
        startActivity(Intent.createChooser(mailer, "Send email..."))
    }

}