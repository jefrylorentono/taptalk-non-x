package com.moselo.HomingPigeon.View.Fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.moselo.HomingPigeon.Helper.DefaultConstant;
import com.moselo.HomingPigeon.Helper.QRDetection;
import com.moselo.HomingPigeon.Helper.Utils;
import com.moselo.HomingPigeon.R;
import com.moselo.HomingPigeon.View.Activity.BarcodeScannerActivity;

import java.io.IOException;

import static com.moselo.HomingPigeon.Helper.DefaultConstant.PermissionRequest.CAMERA_PERMISSION;

public class BarcodeScannerFragment extends Fragment {

    private SurfaceView svScanner;
    private Button btnShowQRCode;

    private CameraSource cameraSource;
    private BarcodeDetector barcodeDetector;

    public BarcodeScannerFragment() {
        // Required empty public constructor
    }

    public static BarcodeScannerFragment newInstance() {
        BarcodeScannerFragment fragment = new BarcodeScannerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_barcode_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        barcodeDetector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        barcodeDetector.setProcessor(new QRDetection());

        cameraSource = new CameraSource.Builder(getContext(), barcodeDetector)
                .setAutoFocusEnabled(true)
                .build();

        initView(view);
    }

    private void initView(View view) {
        svScanner = view.findViewById(R.id.sv_scanner);
        btnShowQRCode = view.findViewById(R.id.btn_show_qr_code);

        svScanner.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
                    return;
                }
                try {
                    cameraSource.start(svScanner.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        btnShowQRCode.setOnClickListener(v -> {
            try {
                ((BarcodeScannerActivity) getActivity()).showQR();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
