package com.example.cameraxtest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.view.CameraView;
import androidx.camera.view.video.OutputFileResults;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.MimeTypeMap;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private ImageButton imageButtonStartStopVideoRecording;
    private ImageButton imageButtonSwitchCameraFrontBack;
    private ImageButton imageButtonTakePhoto;
    private Executor executor;
    private CameraView cameraView;
    private Chronometer chronometer;
    private LinearLayout linearLayoutShowChronometerTimer;
    private ImageView imageViewCircleShowVideoRecodingProcess;
    private Animation animationCircleShowVideoRecodingProcess;
    private final int REQUEST_CODE_PERMISSIONS = 111;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.RECORD_AUDIO"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
    }

    private void checkPermissions() {
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        initialize();
        cameraView.setFlash(ImageCapture.FLASH_MODE_AUTO);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        cameraView.bindToLifecycle(MainActivity.this);
        setOnClickListenerButtonTakePhoto();
        setOnClickListenerButtonStartStopVideoRecording();
        setOnClickListenerButtonSwitchCameraFrontBack();
    }

    private void setOnClickListenerButtonSwitchCameraFrontBack() {
        imageButtonSwitchCameraFrontBack.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UnsafeExperimentalUsageError")
            @Override
            public void onClick(View v) {
                if (cameraView.isRecording()) {
                    return;
                }
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
                }
                if (cameraView.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)) {
                    cameraView.toggleCamera();
                }
            }
        });
    }

    private void cameraStopVideoRecording() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        imageButtonStartStopVideoRecording.setBackgroundResource(R.drawable.circle_background_normal);
        imageButtonStartStopVideoRecording.setImageResource(R.drawable.ic_video);
        chronometer.stop();
        linearLayoutShowChronometerTimer.setVisibility(View.GONE);
        chronometer.setVisibility(View.GONE);
        imageViewCircleShowVideoRecodingProcess.setVisibility(View.GONE);
        imageViewCircleShowVideoRecodingProcess.clearAnimation();
    }

    private void setOnClickListenerButtonStartStopVideoRecording() {
        imageButtonStartStopVideoRecording.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UnsafeExperimentalUsageError")
            @Override
            public void onClick(View v) {
                if (cameraView.isRecording()) {
                    cameraView.stopRecording();
                    cameraStopVideoRecording();
                } else {
                    int screenOrientation = getResources().getConfiguration().orientation;
                    switch (screenOrientation) {
                        case Configuration.ORIENTATION_PORTRAIT:
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                            break;
                        case Configuration.ORIENTATION_LANDSCAPE:
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                            break;
                        default:
                            break;
                    }
                    linearLayoutShowChronometerTimer.setVisibility(View.VISIBLE);
                    imageViewCircleShowVideoRecodingProcess.setVisibility(View.VISIBLE);
                    imageViewCircleShowVideoRecodingProcess.startAnimation(animationCircleShowVideoRecodingProcess);
                    chronometer.setVisibility(View.VISIBLE);
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.start();
                    imageButtonStartStopVideoRecording.setBackgroundResource(R.drawable.circle_background_clicked_video);
                    imageButtonStartStopVideoRecording.setImageResource(R.drawable.ic_stop);
                    SimpleDateFormat simpleDateFormat
                            = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
                    File file = new File(getFolderPathToSavePhotoVideo(), simpleDateFormat.format(new Date()) + ".mp4");
                    cameraView.setCaptureMode(CameraView.CaptureMode.VIDEO);

                    cameraView.startRecording(file, executor, new androidx.camera.view.video.OnVideoSavedCallback() {
                        @Override
                        public void onVideoSaved(@NonNull OutputFileResults outputFileResults) {
                            addPhotoVideoToFolder(file, 1);
                        }

                        @Override
                        public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                            if (cause != null) {
                                Log.d("Error: ", Log.getStackTraceString(cause));
                            }
                        }
                    });
                }
            }
        });
    }

    private void setOnClickListenerButtonTakePhoto() {
        imageButtonTakePhoto.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UnsafeExperimentalUsageError")
            @Override
            public void onClick(View v) {
                if (cameraView.isRecording()) {
                    return;
                }
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
                File file = new File(getFolderPathToSavePhotoVideo(), simpleDateFormat.format(new Date()) + ".jpg");
                cameraView.setCaptureMode(CameraView.CaptureMode.IMAGE);
                ImageCapture.OutputFileOptions imageCaptureOutputFileOptions
                        = new ImageCapture.OutputFileOptions.Builder(file).build();

                cameraView.takePicture(imageCaptureOutputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                addPhotoVideoToFolder(file, 0);
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.d("Error: ", Log.getStackTraceString(exception));
                    }
                });
            }
        });
    }

    private void addPhotoVideoToFolder(File file, int mediaType) {
        if (!file.exists()) {
            return;
        }

        int pathSeparator = String.valueOf(file).lastIndexOf('/');
        int extensionSeparator = String.valueOf(file).lastIndexOf('.');

        String fileName;
        if (pathSeparator >= 0) {
            fileName = String.valueOf(file).substring(pathSeparator + 1);
        } else {
            fileName = String.valueOf(file);
        }

        String extension;
        if (extensionSeparator >= 0) {
            extension = String.valueOf(file).substring(extensionSeparator + 1);
        } else {
            extension = "";
        }

        String mimeType;
        if (extension.length() > 0) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ENGLISH));
        } else {
            mimeType = null;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.TITLE, fileName);
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);
        if (mimeType != null && mimeType.length() > 0) {
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

            Uri externalContentUri;
            if (mediaType == 0) {
                externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if (mediaType == 1) {
                externalContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else {
                externalContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }

            if (Build.VERSION.SDK_INT >= 29) {
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Camera");
                contentValues.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis());
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, true);

                Uri uri = getContentResolver().insert(externalContentUri, contentValues);
                if (uri != null) {
                    try {
                        if (isSuccessfulWriteFileToOutputStream(file, getContentResolver().openOutputStream(uri))) {
                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, false);
                            getContentResolver().update(uri, contentValues, null, null);
                        }
                    } catch (FileNotFoundException e) {
                        Log.d("Exception: ", Log.getStackTraceString(e));
                        getContentResolver().delete(uri, null, null);
                    }
                }
                file.delete();
            } else {
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(file));
                sendBroadcast(intent);
            }
        }
    }

    private boolean isSuccessfulWriteFileToOutputStream(File file, OutputStream outputStream) {
        try {
            InputStream inputStream = new FileInputStream(file);
            try {
                byte[] bytes = new byte[1024];
                int length;

                while ((length = inputStream.read(bytes)) > 0) {
                    outputStream.write(bytes, 0, length);
                }

                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.d("Exception: ", Log.getStackTraceString(e));
                }

            } catch (IOException e) {
                Log.d("Exception: ", Log.getStackTraceString(e));
            }
        } catch (FileNotFoundException e) {
            Log.d("Exception: ", Log.getStackTraceString(e));
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                Log.d("Exception: ", Log.getStackTraceString(e));
            }
        }
        return true;
    }

    private String getFolderPathToSavePhotoVideo() {
        String folderPath;

        if (Build.VERSION.SDK_INT >= 29) {
            folderPath = Objects.requireNonNull(getExternalFilesDir(Environment.DIRECTORY_PICTURES)).toString();
        } else {
            folderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/My_Camera";
        }

        File fileDir = new File(folderPath);

        if (!fileDir.exists() && !fileDir.mkdirs()) {
            Log.d("fileDir: ", "NOT exists");
        }
        return folderPath;
    }

    private void initialize() {
        executor = Executors.newSingleThreadExecutor();
        imageButtonStartStopVideoRecording = findViewById(R.id.image_button_recording_video);
        imageButtonSwitchCameraFrontBack = findViewById(R.id.image_button_switch_camera_front_back);
        imageButtonTakePhoto = findViewById(R.id.image_button_take_photo);
        cameraView = findViewById(R.id.camera_view);
        linearLayoutShowChronometerTimer = findViewById(R.id.linear_layout);
        chronometer = findViewById(R.id.chronometer);
        imageViewCircleShowVideoRecodingProcess = findViewById(R.id.image_view_circle_show_video_recoding_process);
        animationCircleShowVideoRecodingProcess = AnimationUtils.loadAnimation(this, R.anim.anim_circle_video);
    }

    private boolean allPermissionsGranted() {
        for (String required_permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, required_permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    @Override
    protected void onStop() {
        super.onStop();
        if (allPermissionsGranted()) {
            if (cameraView.isRecording()) {
                cameraView.stopRecording();
                cameraStopVideoRecording();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "NOT all permissions granted by user", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }
}