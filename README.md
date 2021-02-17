# QR Code Scanner

Uso de la cámara y la libreria de Google Vision para escanear algunos tipos de codigos electronicos (codebar, QR).

Documentación oficial [Google Vision](https://developers.google.com/vision/android/barcodes-overview).

> Nota: Google recomienda usar MLKit con Firebase

En este tutorial usaremos el lector de QR, el cual nos permitira utilizar distintos tipos de QR, por ejemplo:

- URL
- EMAIL
- PHONE
- SMS
- WIFI
- entre otros...

## Caracteristicas adicionales

Los ejemplos de cada uno de los tipos de QR son funcionales y te permitiran ver como es su implementación.


## Primeros pasos

Ir al build.gradle(app) y agregar la siguiente dependencia:

```sh
dependencies{
implementation 'com.google.android.gms:play-services-vision:20.1.2'
}
```

>Importante: utilizar especificamente la version mencionada para evitar cualquier detalle de incompatibilidad

Por ultimo le das al boton 'Sync now' para sincronizar gradle.

Ya en nuestro activity principal (MainActivity.kt)
declararemos las variables:
```java
aqui definiremos la forma en la que barcodeDetector podra detectar las imagénes

barcodeDetector = BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build()
```

Además
```java
tenemos que definir ademas el surfaceView en el cual será representado en la pantalla la imagen de la cámara

cameraSource = CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1280, 960)
                .setAutoFocusEnabled(true)
                .build()
```

con este hecho, ahora podemos comenzar a utilizar el callback en el surfaceView asi como la interfaz Detector.Processor<Barcode>, la cual nos regresara las detecciones, cuando esta las haya reconocido
```java
binding.cameraPreview.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {}
            override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {}
            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {}
        })
        
barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {}
            override fun receiveDetections(detections: Detections<Barcode>) {
                val qrcodes = detections.detectedItems
                if (qrcodes.size() != 0 && hasVibrate) {}
            }
        })
```

## Para terminar
Puedes colaborar al proyecto y agregar mas funcionalidades.
Saludos
