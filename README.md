# QRCodeScanner
Using camera and google vision services to scan some types of code (codebar, qr)

## Como usarlo

barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE) <-- Aqui escoges el tipo de escaneo sera el que estara buscando la libreria
                .build();
                
cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .build();
