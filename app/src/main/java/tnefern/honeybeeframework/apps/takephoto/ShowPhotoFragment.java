package tnefern.honeybeeframework.apps.takephoto;


import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import tnefern.honeybeeframework.R;

import static tnefern.honeybeeframework.R.layout.fragment_take_photos_dele_layout;
@RequiresApi(api = Build.VERSION_CODES.M)
public class ShowPhotoFragment extends Fragment {
    View view;
//    CameraPreview mCameraPreview;
//    private Camera mCamera;

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;


    ImageView im;
    private ImageReader imageReader;
    private TextureView textureView;
    private CameraDevice.StateCallback stateCallback;
    private CameraCaptureSession.CaptureCallback captureCallbackListener;
    private Size imageDimension;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private boolean istakingPhotos;

    private static final String IMAGE_PREFIX = "TP";
    private static final String IMAGE_FOLDER = "TakenPhotos";

    private final Handler handler = new Handler(); //This should be declared before OnCreate
    private Runnable photoRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                takePicture(); //The result will be in onPictureTaken
            }
                catch (Exception e) {
                e.printStackTrace();
                //Handle Exception!
            }
                finally{
                //also call the same runnable to call it at regular interval
                handler.postDelayed(this, 3*1000); //10*1000 is your interval (in this case 10 seconds)
            }

        }
    };

    private static String TAG = "ShowPhotoFragment";
    String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA};
    private static final int PERMISSION_ALL = 105;
    private  static final int REQUEST_CAMERA_RESULT = 106;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int MY_CAMERA_REQUEST_CODE = 222;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private TextureView.SurfaceTextureListener textureListener= new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };





//    public ShowPhotoFragment(){
//
//    }

    public void setCameraPreview(CameraPreview pCp){

//        this.mCameraPreview = pCp;
    }

    public void setCamera(Camera pCam){

//        this.mCamera = pCam;
    }

    private void openCamera(){
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
            if(manager!=null){
                Log.e(TAG, "openCamera: is camera open");
            }
            try {
                cameraId = manager.getCameraIdList()[0];
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                assert map != null;
                imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
                // Add permission for camera and let user grant the permission
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (getActivity().checkSelfPermission( Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && getActivity().checkSelfPermission( Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        getActivity().requestPermissions( new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                        return;
                    }
//                }
                manager.openCamera(cameraId, stateCallback, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.e(TAG, "openCamera X");
//        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(fragment_take_photos_dele_layout, container, false);

//        framelayout preview = (framelayout)view.findviewbyid(r.id.camera_preview);
//        preview.addview(mcamerapreview);
//        im = (ImageView)view.findViewById(R.id.imageView);

        if(!hasAllPermissions(getActivity(), PERMISSIONS)) {
            getActivity().requestPermissions( PERMISSIONS, PERMISSION_ALL);
        }
        textureView = (TextureView)view.findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            stateCallback= new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    //This is called when the camera is open
                    Log.e(TAG, "onOpened");
                    cameraDevice = camera;
                    createCameraPreview();
                }
                @Override
                public void onDisconnected(CameraDevice camera) {
                    cameraDevice.close();
                }
                @Override
                public void onError(CameraDevice camera, int error) {
                    cameraDevice.close();
                    cameraDevice = null;
                }
            };

            captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
//                    Toast.makeText(AndroidCameraApi.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
//        }

        Button captureButton = (Button) view.findViewById(R.id.button_capture);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mCamera.takePicture(null, null, mPicture);
//                openCamera();
//                dispatchTakePictureIntent();

//                takePicture();

                handler.post(photoRunnable);
                istakingPhotos = true;
//                captureButton.setEnabled(false);
            }
        });
        return view;
    }

    private void dispatchTakePictureIntent() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (getActivity().checkSelfPermission(Manifest.permission.CAMERA)
//                    != PackageManager.PERMISSION_GRANTED) {
//                requestPermissions(new String[]{Manifest.permission.CAMERA},
//                        MY_CAMERA_REQUEST_CODE);
//            }
//        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            Log.e(TAG,"frag dispatchTakePictureIntent");
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    public void setImageBitmap(Bitmap imageBitmap){
        im.setImageBitmap(imageBitmap);
        Log.e(TAG,"frag setImageBitmap");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_REQUEST_CODE) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

//                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    Log.e(TAG,"frag camera permission granted");
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }

            } else {
                Log.e(TAG,"frag camera permission NOT granted");
            }

//                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();

            }

        }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            im.setImageBitmap(imageBitmap);
        }
        Log.e(TAG,"hello");
//        super.onActivityResult(requestCode, resultCode, data);
    }

    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getOutputMediaFile();
            if (pictureFile == null) {
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {

            } catch (IOException e) {
            }
        }

    };

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "MyCameraApp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
                Surface surface = new Surface(texture);
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequestBuilder.addTarget(surface);

                cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        //The camera is already closed
                        if (null == cameraDevice) {
                            return;
                        }
                        // When the session is ready, we start displaying the preview.
                        cameraCaptureSessions = cameraCaptureSession;
                        updatePreview();
                    }
                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
//                        Toast.makeText(AndroidCameraApi.this, "Configuration change", Toast.LENGTH_SHORT).show();
                    }
                }, null);
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            try {
                cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
            } catch (Exception e) {
                e.printStackTrace();
            }
//        }

    }
    private void closeCamera() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (null != cameraDevice) {

                cameraDevice.close();

                cameraDevice = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
//        }
    }


//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File folder = getActivity().getExternalFilesDir(IMAGE_FOLDER);
            if (!folder.mkdirs()) {
                Log.e(TAG, "Directory not created");
            }
            final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());
            final File file = new File(folder,IMAGE_PREFIX+timeStamp+".JPG");
            file.createNewFile();
            //final File file = new File(Environment.getExternalStorageDirectory()+"/pic.jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            Log.d(TAG,"Photo Saved: "+file.getAbsolutePath());
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
//                    Toast.makeText(AndroidCameraApi.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBackgroundThread.quitSafely();
        }
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();

            if(istakingPhotos){
                handler.post(photoRunnable);
            }
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        if(istakingPhotos){
            handler.removeCallbacks(photoRunnable);
        }

        stopBackgroundThread();
        super.onPause();
    }

    public  boolean hasAllPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (getActivity().checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
    }



    /*
    10-31 17:49:17.468 3079-3156/tnefern.honeybeeframework E/Surface: queueBuffer: error queuing buffer to SurfaceTexture, -19
10-31 17:49:17.468 3079-3156/tnefern.honeybeeframework E/Legacy-CameraDevice-JNI: produceFrame: Failed to queue buffer, error No such device (-19).
10-31 17:49:17.468 3079-3156/tnefern.honeybeeframework E/Legacy-CameraDevice-JNI: LegacyCameraDevice_nativeProduceFrame: Error while producing frame No such device (-19).

    --------- beginning of crash
10-31 17:49:17.469 3079-3156/tnefern.honeybeeframework E/AndroidRuntime: FATAL EXCEPTION: Thread-155
    Process: tnefern.honeybeeframework, PID: 3079
    java.lang.UnsupportedOperationException: Unknown error -19
        at android.hardware.camera2.legacy.LegacyExceptionUtils.throwOnError(LegacyExceptionUtils.java:69)
        at android.hardware.camera2.legacy.LegacyCameraDevice.produceFrame(LegacyCameraDevice.java:636)
        at android.hardware.camera2.legacy.RequestThreadManager$2.onPictureTaken(RequestThreadManager.java:235)
        at android.hardware.Camera$EventHandler.handleMessage(Camera.java:1092)
        at android.os.Handler.dispatchMessage(Handler.java:102)
        at android.os.Looper.loop(Looper.java:148)
        at android.hardware.camera2.legacy.CameraDeviceUserShim$CameraLooper.run(CameraDeviceUserShim.java:136)
        at java.lang.Thread.run(Thread.java:818)

     **/