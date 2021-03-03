/*
 * Copyright (c) 2016. Horizon Video Technologies. All rights reserved.
 */

package com.hvt.petrakeas.simpleapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import android.view.View;
import android.widget.Toast;

import com.hvt.horizonSDK.CameraHelper;
import com.hvt.horizonSDK.HVTCamcorderProfile;
import com.hvt.horizonSDK.HVTCamera;
import com.hvt.horizonSDK.HVTCameraListener;
import com.hvt.horizonSDK.HVTVars;
import com.hvt.horizonSDK.HVTVars.HVTLevelerCropMode;
import com.hvt.horizonSDK.HVTView;
import com.hvt.horizonSDK.Size;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {

    private HVTView mCameraPreview;
    private HVTCamera mHVTCamera;
    private CameraHelper mCameraHelper;

    private AppCompatButton mRecButton;
    private AppCompatButton mPhotoButton;
    private AppCompatButton mCropButton;
    private AppCompatButton mFlipCameraButton;
    private AppCompatButton mFlashCameraButton;

    private File mVideoFile;
    private File mPhotoFile;

    private final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int PERMISSIONS_REQ_CODE = 1;
    private boolean mHasPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create and configure HVTCamera
        mHVTCamera = new HVTCamera(getApplicationContext());
        mHVTCamera.setListener(new MyListener());
        int activityRotation = getWindowManager().getDefaultDisplay().getRotation();
        mHVTCamera.setScreenRotation(activityRotation);

        // Configure and attach HVTView
        mCameraPreview = (HVTView) findViewById(R.id.camera_preview);
        mHVTCamera.attachPreviewView(mCameraPreview);

//        alternateConfiguration();

        checkPermission();
    }

    private void onCreatePermissionGranted() {
        // We'll save photos and videos to the same file for simplicity.
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                .getAbsolutePath() + File.separator + "HorizonSDKSample");
        directory.mkdirs();
        mVideoFile = new File(directory.getAbsolutePath(), "video.mp4");
        mPhotoFile = new File(directory.getAbsolutePath(), "photo.jpeg");

        // Select camera resolution
        try {
            mCameraHelper = new CameraHelper();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Camera could not be opened", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        int facing = mCameraHelper.hasCamera(Camera.CameraInfo.CAMERA_FACING_BACK) ?
                Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
        Size[] sizes = mCameraHelper.getDefaultVideoAndPhotoSize(facing);
        mHVTCamera.setCamera(facing, sizes[0], sizes[1]);

        // Initialize UI
        initializeButtons();
    }

    private void alternateConfiguration() {
        mHVTCamera.setWatermarkEnabled(true);
        mHVTCamera.setOutputMovieSize(new Size(640, 480));
        mHVTCamera.setCameraMode(HVTVars.CameraMode.VIDEO);

        mCameraPreview.setViewType(HVTView.ViewType.LEVELED);
        mCameraPreview.setFillMode(HVTView.FillMode.ASPECT_FIT);
        mCameraPreview.setDoubleTapToChangeFillMode(false);
    }

    private void initializeButtons() {
        mRecButton = (AppCompatButton) findViewById(R.id.rec_button);
        mPhotoButton = (AppCompatButton) findViewById(R.id.photo_button);
        mCropButton = (AppCompatButton) findViewById(R.id.crop_button);
        mFlipCameraButton = (AppCompatButton) findViewById(R.id.flip_camera_button);
        mFlashCameraButton = (AppCompatButton) findViewById(R.id.flash_button);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.equals(mRecButton)) {
                    if (mHVTCamera.isRecording() && !mHVTCamera.isStartingRecording() && !mHVTCamera.isStoppingRecording()) {
                        stopRecording();
                    }
                    else if(mHVTCamera.isRunningIdle()) {
                        startRecording();
                        mRecButton.setText("Stop");
                    }
                }
                else if(v.equals(mPhotoButton)) {
                    // We take either a full resolution photo or a video snapshot, depensing on the
                    // the current state.
                    if (mHVTCamera.isRunningIdle()) {
                        mHVTCamera.capturePhoto(mPhotoFile);
                    }
                    if (mHVTCamera.isRecording() && !mHVTCamera.isStoppingRecording()) {
                        mHVTCamera.captureSnapshot(mPhotoFile);
                    }
                }
                else if(v.equals(mCropButton)) {
                    changeCropMode();
                }
                else if(v.equals(mFlipCameraButton)) {
                    if (mHVTCamera.isRunningIdle()) {
                        flipCamera();
                    }
                }
                else if (v.equals(mFlashCameraButton)) {
                    if (mHVTCamera.isRunning()) {
                        toggleFlash();
                    }
                }
            }
        };

        mRecButton.setOnClickListener(listener);
        mPhotoButton.setOnClickListener(listener);
        mCropButton.setOnClickListener(listener);
        mFlipCameraButton.setOnClickListener(listener);
        mFlashCameraButton.setOnClickListener(listener);

        if (mCameraHelper.getNumberOfCameras() == 1) {
            mFlipCameraButton.setVisibility(View.INVISIBLE);
        }

        int facing = mHVTCamera.getCameraFacing();
        if (!supportsTorchMode(mCameraHelper.getSupportedFlashModes(facing))) {
            mFlashCameraButton.setVisibility(View.INVISIBLE);
        }
    }

    private static boolean supportsTorchMode(List<String> flashModes) {
        if (flashModes == null) {
            return false;
        }
        return flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH);
    }

    private void rotateUI(int degrees) {
        mRecButton.setRotation(degrees);
        mPhotoButton.setRotation(degrees);
        mCropButton.setRotation(degrees);
        mFlipCameraButton.setRotation(degrees);
        mFlashCameraButton.setRotation(degrees);
    }

    private void startRecording() {
        // We create a recording profile for the current camera resolution. We could also change some of
        // the properties, such as the bitrate and audio settings.
        Size size = mHVTCamera.getOutputMovieSize();
        HVTCamcorderProfile recordingProfile = new HVTCamcorderProfile(size.getWidth(), size.getHeight());
        mHVTCamera.startRecording(mVideoFile, recordingProfile);
    }

    private void stopRecording() {
        // We request to stop recording. The actual recording completion may take a while.
        mHVTCamera.stopRecording();
    }

    private void flipCamera() {
        int newCameraFacing = (mHVTCamera.getCameraFacing() == Camera.CameraInfo.CAMERA_FACING_BACK) ?
                Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
        Size[] sizes = mCameraHelper.getDefaultVideoAndPhotoSize(newCameraFacing);
        // We request the other camera. This will result in closing the current camera and opening
        // the other. We will notified of the time the camera actually opened, using the callback.
        mHVTCamera.setCamera(newCameraFacing, sizes[0], sizes[1]);
    }

    private void toggleFlash() {
        String flashMode = mHVTCamera.getFlashMode();
        flashMode = (flashMode.equals(Camera.Parameters.FLASH_MODE_TORCH)) ?
                Camera.Parameters.FLASH_MODE_OFF : Camera.Parameters.FLASH_MODE_TORCH;
        mHVTCamera.setFlashMode(flashMode);

        if (flashMode.equals(Camera.Parameters.FLASH_MODE_TORCH)) {
            mFlashCameraButton.setText("Flash on");
        } else {
            mFlashCameraButton.setText("Flash off");
        }
    }

    private void changeCropMode() {
        String text;
        HVTLevelerCropMode mode = mHVTCamera.getLevelerCropMode();
        if (mode == HVTLevelerCropMode.FLEX) {
            mode = HVTLevelerCropMode.ROTATE;
            text = "Rotate";
        } else if (mode == HVTLevelerCropMode.ROTATE) {
            mode = HVTLevelerCropMode.LOCKED;
            text = "Locked";
        } else {
            mode = HVTLevelerCropMode.FLEX;
            text = "Flex";
        }
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        mHVTCamera.setLevelerCropMode(mode);
    }

    //region Permission handling

    public static String[] getNotGrantedPermissions(Context context, String[] permissions) {
        List<String> notGrandedpermissions = new ArrayList<>();
        for (String permission: permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                notGrandedpermissions.add(permission);
            }
        }
        return notGrandedpermissions.toArray(new String[notGrandedpermissions.size()]);
    }

    public static boolean shouldShowRequestPermissionRationale(Activity activity,
                                                               String[] permissions) {
        for (String permission: permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    private void checkPermission() {
        final String[] notGrandedPermisions = getNotGrantedPermissions(this, PERMISSIONS);
        if (notGrandedPermisions.length != 0) {
            mHasPermission = false;
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(this, notGrandedPermisions)) {
                // Show an explnation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                ActivityCompat.requestPermissions(this,
                        notGrandedPermisions,
                        PERMISSIONS_REQ_CODE);

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        notGrandedPermisions,
                        PERMISSIONS_REQ_CODE);
            }
        }
        else {
            // We've already got permission
            mHasPermission = true;
            onCreatePermissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == PERMISSIONS_REQ_CODE) {
            boolean fail = false;
            if (grantResults.length == permissions.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        fail = true;
                        break;
                    }
                }
            } else {
                fail = true;
            }
            if (!fail) {
                mHasPermission = true;
                onCreatePermissionGranted();
            }
        }
    }

    //endregion

    //region Activity Lifecycle

    @Override
    protected void onResume() {
        super.onResume();

        if (mHasPermission) {
            mHVTCamera.startRunning();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mHasPermission) {
            mHVTCamera.stopRunning();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mHVTCamera.destroy();
        mHVTCamera = null;
    }

    /* If the activity can change orientation, we need to update HVTCamera so that the preview is
     * rendered correctly.
     */
    //TODO: This callback will not be called when switching from landscape to reverseLandscape and
    // vice versa. We should poll screen rotation for changes, if we want to account for that.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mHVTCamera != null) {
            int screenRotation = getWindowManager().getDefaultDisplay().getRotation();
            mHVTCamera.setScreenRotation(screenRotation);
        }
    }

    //endregion

    //region HVTCamera Listener
    private class MyListener implements HVTCameraListener {

        @Override
        public void onFailedToStart() {
            Toast.makeText(MainActivity.this, "Camera could not open. Try again later.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStartedRunning(Camera.Parameters parameters, int i) {

        }

        @Override
        public void onPreviewHasBeenRunning(Camera.Parameters parameters, int i) {
            if (!supportsTorchMode(CameraHelper.getSupportedFlashModes(parameters))) {
                mFlashCameraButton.setVisibility(View.INVISIBLE);
            }
            else {
                mFlashCameraButton.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onWillStopRunning() {

        }

        @Override
        public void onStoppedRunning() {

        }

        @Override
        public void onRecordingHasStarted() {

        }

        @Override
        public void onRecordingWillStop() {

        }

        @Override
        public void onRecordingFinished(File file, boolean success) {
            if (success) {
                Toast.makeText(MainActivity.this, "Recording finished", Toast.LENGTH_SHORT).show();
                // Make the media visible to other apps
                MediaScannerConnection.scanFile(MainActivity.this, new String[]{file.getAbsolutePath()}, null, null);
            }
            else {
                Toast.makeText(MainActivity.this, "Error saving video", Toast.LENGTH_SHORT).show();
            }
            mRecButton.setText("Rec");
        }

        @Override
        public void onPhotoCaptured(File file, boolean success) {
            if (success) {
                Toast.makeText(MainActivity.this, "Photo captured", Toast.LENGTH_SHORT).show();
                // Make the media visible to other apps
                MediaScannerConnection.scanFile(MainActivity.this, new String[]{file.getAbsolutePath()}, null, null);
            }
            else {
                Toast.makeText(MainActivity.this, "Error saving photo", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onSnapshotCaptured(File file) {
            Toast.makeText(MainActivity.this, "Snapshot captured", Toast.LENGTH_SHORT).show();
            // Make the media visible to other apps
            MediaScannerConnection.scanFile(MainActivity.this, new String[]{file.getAbsolutePath()}, null, null);
        }

        @Override
        public void onAngleUpdated(float angle, float scale) {
            rotateUI((int) angle);
        }

        @Override
        public void onSensorNotResponding() {
            Toast.makeText(MainActivity.this, "Motion sensor not responding. Try again later.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onSensorResponded() {

        }
    }

    //endregion
}
