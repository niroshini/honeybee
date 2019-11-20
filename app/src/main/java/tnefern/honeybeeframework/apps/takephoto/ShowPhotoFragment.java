package tnefern.honeybeeframework.apps.takephoto;


import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
//import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
//import android.hardware.Camera;
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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

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
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.Job;
import tnefern.honeybeeframework.common.JobParams;
import tnefern.honeybeeframework.common.JobPool;

import static tnefern.honeybeeframework.R.layout.fragment_take_photos_dele_layout;
@RequiresApi(api = Build.VERSION_CODES.M)
public class ShowPhotoFragment extends Fragment {
    private View view;
    private Camera mCamera;
    private CameraPreview mPreview;

    String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA};
    private static final int PERMISSION_ALL = 105;
    private static String TAG = "ShowPhotoFragment";
    private static final String IMAGE_PREFIX = "TP";
    private static final String IMAGE_FOLDER = "TakenPhotos";
    private int photo_count = 0;
    private static final int PHOTO_LIMIT=10;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(fragment_take_photos_dele_layout, container, false);

        if(!hasAllPermissions(getActivity(), PERMISSIONS)) {
            getActivity().requestPermissions( PERMISSIONS, PERMISSION_ALL);
        }

        if(!checkCameraHardware(getActivity())){
            Log.d(TAG,"No Camera");
        }else{
            Log.d(TAG,"Camera exists");
        }

//        framelayout preview = (framelayout)view.findviewbyid(r.id.camera_preview);
//        preview.addview(mcamerapreview);
//        im = (ImageView)view.findViewById(R.id.imageView);
        // Create an instance of Camera
        mCamera = getCameraInstance();
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this.getActivity(), mCamera);
        FrameLayout preview = (FrameLayout) view.findViewById(R.id.camera_preview);
        preview.setLayoutParams(new LinearLayout.LayoutParams(400,400));
        preview.addView(mPreview);

        Button captureButton = (Button) view.findViewById(R.id.button_capture);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JobPool.getInstance().setOpportunisticJobTrue();
                mCamera.takePicture(null, null, mPicture);



//                handler.post(photoRunnable);
//                isTakingPhotos = true;
//                captureButton.setEnabled(false);
            }
        });
        return view;
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
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

    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e){
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            final File folder = getActivity().getExternalFilesDir(IMAGE_FOLDER);
            if (!folder.mkdirs()) {
                Log.e(TAG, "Directory not created");
            }
            final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());
            final File pictureFile = new File(folder,IMAGE_PREFIX+timeStamp+"_"+photo_count+".JPG");
            try {
                pictureFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }



            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Log.d(TAG,"Photo "+photo_count+" saved at: "+pictureFile.getAbsolutePath());

                JobParams jp = new JobParams(
                        CommonConstants.READ_STRING_MODE);
                Job j = new Job(pictureFile.getAbsolutePath(),CommonConstants.OPPORTUNISTIC_JOB,CommonConstants.READ_FILES_MODE,
                        CommonConstants.READ_STRING_MODE);
                Log.e(TAG,"Opportunistic Photo: "+j.toString());
                JobPool.getInstance().addJob(j);
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
            photo_count++;
            mCamera.stopPreview();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();


            if(photo_count<PHOTO_LIMIT){
                mCamera.takePicture(null, null, mPicture);
            }else{
                JobPool.getInstance().finaliseOpportunisticJob();
            }

        }
    };

    @Override
    public void onPause() {
        super.onPause();
        releaseCamera();

    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

//    private void setOrientation(CameraCharacteristics characteristics, CaptureRequest.Builder captureBuilder){
//
//        try {
//            int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
//            Log.d(TAG, "Preview Dev rotation: "+rotation);//temi is 3
//
//
//            int sensorOrientation =  characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
//            Log.d(TAG, "Preview SENSOR_ORIENTATION: "+sensorOrientation);
//            int surfaceRotation = ORIENTATIONS.get(rotation);
//            Log.d(TAG, "Preview surfaceRotation: "+surfaceRotation);
//            int jpegOrientation =
//                    (surfaceRotation + sensorOrientation + 270) % 360;
//            Log.d(TAG, "Preview jpegOrientation: "+jpegOrientation);
////            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
//
//            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
/*
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
    private boolean isTakingPhotos = false;

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
                handler.postDelayed(this, 5*1000); //10*1000 is your interval (in this case 10 seconds)
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

//    public void setCameraPreview(CameraPreview pCp){
//
////        this.mCameraPreview = pCp;
//    }
//
//    public void setCamera(Camera pCam){
//
////        this.mCamera = pCam;
//    }

    private void openCamera(){
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
            if(manager!=null){
                Log.e(TAG, "openCamera: is camera open");
            }
            try {
                cameraId = manager.getCameraIdList()[0];
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                if(characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY){
                     Log.d(TAG,"Legacy");
                }
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
                    Log.e(TAG, "onCaptureCompleted");
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

                takePicture();

//                handler.post(photoRunnable);
//                isTakingPhotos = true;
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

//    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
//        @Override
//        public void onPictureTaken(byte[] data, Camera camera) {
//            File pictureFile = getOutputMediaFile();
//            if (pictureFile == null) {
//                return;
//            }
//            try {
//                FileOutputStream fos = new FileOutputStream(pictureFile);
//                fos.write(data);
//                fos.close();
//            } catch (FileNotFoundException e) {
//
//            } catch (IOException e) {
//            }
//        }
//
//    };

//    private static File getOutputMediaFile() {
//        File mediaStorageDir = new File(
//                Environment
//                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
//                "MyCameraApp");
//        if (!mediaStorageDir.exists()) {
//            if (!mediaStorageDir.mkdirs()) {
//                Log.d("MyCameraApp", "failed to create directory");
//                return null;
//            }
//        }
//        // Create a media file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
//                .format(new Date());
//        File mediaFile;
//        mediaFile = new File(mediaStorageDir.getPath() + File.separator
//                + "IMG_" + timeStamp + ".jpg");
//
//        return mediaFile;
//    }

    private void setOrientation(CameraCharacteristics characteristics, CaptureRequest.Builder captureBuilder){

        try {
            int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            Log.d(TAG, "Preview Dev rotation: "+rotation);//temi is 3


        int sensorOrientation =  characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Log.d(TAG, "Preview SENSOR_ORIENTATION: "+sensorOrientation);
        int surfaceRotation = ORIENTATIONS.get(rotation);
        Log.d(TAG, "Preview surfaceRotation: "+surfaceRotation);
        int jpegOrientation =
                (surfaceRotation + sensorOrientation + 270) % 360;
        Log.d(TAG, "Preview jpegOrientation: "+jpegOrientation);
//            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    protected void createCameraPreview() {
        try {
            Log.d(TAG,"createCameraPreview");
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
                Surface surface = new Surface(texture);
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequestBuilder.addTarget(surface);





            CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = null;
            characteristics = manager.getCameraCharacteristics(cameraDevice.getId());

            setOrientation(characteristics, captureRequestBuilder);

                cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        Log.d(TAG,"createCameraPreview: onConfigured");
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
                        Log.d(TAG,"createCameraPreview: FAILED");
                    }
                }, null);
//            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        Log.d(TAG,"updatePreview");
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//        captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 180);
        Log.d(TAG,"captureRequestBuilder.set");
            try {
                cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
                Log.d(TAG,"cameraCaptureSessions.set");
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
//        }

    }
    private void closeCamera() {
        Log.d(TAG,"closeCamera");
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
        Log.d(TAG, "takePicture");
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
//            int width = 300;
//            int height = 200;
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
//            int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
//            Log.d(TAG, "Dev rotation: "+rotation);//temi is 3
//            int sensorOrientation =  characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
//            Log.d(TAG, "SENSOR_ORIENTATION: "+sensorOrientation);
//            int surfaceRotation = ORIENTATIONS.get(rotation);
//            Log.d(TAG, "surfaceRotation: "+surfaceRotation);
//            int jpegOrientation =
//                    (surfaceRotation + sensorOrientation + 270) % 360;
//            Log.d(TAG, "jpegOrientation: "+jpegOrientation);
////            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
//            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation);
            setOrientation(characteristics, captureBuilder);


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
                    Log.d(TAG, "onImageAvailable");
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
                        throw new IllegalStateException("Unable to write image to buffer.", e);
                    }
                    finally {
                        if (image != null) {
                            image.close();
                        }
                        if(reader != null) {
                            reader.close();
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
                    Log.d(TAG,"onCaptureCompleted ");
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        Log.d(TAG,"onConfigured ");
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.d(TAG,"onConfigureFailed ");
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
            mBackgroundThread.quitSafely();
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

//            if(isTakingPhotos){
//                handler.post(photoRunnable);
//            }
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
//        if(isTakingPhotos){
//            handler.removeCallbacks(photoRunnable);
//        }

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

    //TODO when adding taken photos to the job pool, check if>0 bytes.