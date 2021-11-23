package com.johanpmeert.bitcoinqrscannerforbtcbchbsvbtg;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.camera.CameraSourceConfig;
import com.google.mlkit.vision.camera.CameraXSource;
import com.google.mlkit.vision.camera.DetectionTaskCallback;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.Objects;

public class Qrscanner extends AppCompatActivity {

    CameraXSource cameraXSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) { // check to see if we can access camera
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        while (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            try {
                Thread.sleep(500);
                Log.e("Sleeping...", "0.5s");
            } catch (InterruptedException e) {
                Log.e("thread exception", e.getMessage());
            }
            // wait for permission granted
        }
        setContentView(R.layout.activity_qrscanner);
        PreviewView preview = findViewById(R.id.view);
        BarcodeScannerOptions barcodeScannerOptions = new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build();
        BarcodeScanner barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions);
        CameraSourceConfig cameraSourceConfig = new CameraSourceConfig.Builder(this, barcodeScanner, new DetectionTaskCallback<List<Barcode>>() {
            @Override
            public void onDetectionTaskReceived(@NonNull Task<List<Barcode>> task) {
                if (preview.getBitmap() != null) {
                    task = barcodeScanner.process(InputImage.fromBitmap(preview.getBitmap(), 0));
                    task.addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                        @Override
                        public void onSuccess(@NonNull List<Barcode> barcodes) {
                            if (barcodes.size() != 0) {
                                Intent intent = new Intent();
                                intent.putExtra("QR", barcodes.get(0).getDisplayValue());
                                Qrscanner.this.setResult(Activity.RESULT_OK, intent);
                                cameraXSource.stop();
                                Qrscanner.this.finish();
                            }
                        }
                    });
                }
            }
        }).setFacing(CameraSourceConfig.CAMERA_FACING_BACK).build();
        cameraXSource = new CameraXSource(cameraSourceConfig, preview);
        cameraXSource.start();
    }
}
