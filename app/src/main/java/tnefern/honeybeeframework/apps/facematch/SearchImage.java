package tnefern.honeybeeframework.apps.facematch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;

public class SearchImage {
	Bitmap myBitmap = null;
	int width = 0;
	int height = 0;
	Face[] detectedFaces = null;
	FaceDetector faceDetector = null;
	int NUMBER_OF_FACES = 10;
	int NUMBER_OF_FACE_DETECTED = 0;
	BitmapFactory.Options bitmapFatoryOptions = null;

	public SearchImage() {
		bitmapFatoryOptions = new BitmapFactory.Options();
		bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
//		bitmapFatoryOptions.inJustDecodeBounds = true;

	}

//	public int search(Resources pR) {
//		BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
//		bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
//		myBitmap = BitmapFactory.decodeResource(pR,
//				R.drawable.im2, bitmapFatoryOptions);
//		width = myBitmap.getWidth();
//		height = myBitmap.getHeight();
//		detectedFaces = new FaceDetector.Face[NUMBER_OF_FACES];
//		faceDetector = new FaceDetector(width, height, NUMBER_OF_FACES);
//		NUMBER_OF_FACE_DETECTED = faceDetector.findFaces(myBitmap,
//				detectedFaces);
//		System.out.println("Number of faces: " + NUMBER_OF_FACE_DETECTED);
//		myBitmap = null;
//		return NUMBER_OF_FACE_DETECTED;
//	}

	
	private int detectFace(File pFile) {
		
		myBitmap = BitmapFactory.decodeFile(pFile.getPath(), bitmapFatoryOptions);
		width = myBitmap.getWidth();
		height = myBitmap.getHeight();
		detectedFaces = new FaceDetector.Face[NUMBER_OF_FACES];
		faceDetector = new FaceDetector(width, height, NUMBER_OF_FACES);
		NUMBER_OF_FACE_DETECTED = faceDetector.findFaces(myBitmap,
				detectedFaces);
//		System.out.println("Number of faces: " + NUMBER_OF_FACE_DETECTED);
		myBitmap = null;
		faceDetector = null;
		return NUMBER_OF_FACE_DETECTED;
	}
	
	
	private int detectFace(String pFile) {
		try {
			FaceDetectorOptions highAccuracyOptions = new FaceDetectorOptions.Builder()
					.setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
					.setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
					.setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
					.build();

			myBitmap = BitmapFactory.decodeFile(pFile, bitmapFatoryOptions);
			InputImage image = InputImage.fromBitmap(myBitmap, 0);

			com.google.mlkit.vision.face.FaceDetector detector = FaceDetection.getClient(highAccuracyOptions);

			Task<List<com.google.mlkit.vision.face.Face>> result = null;

			for (int i = 0; i < 3; i++) {
				result = detector.process(image);
			}
			

			List<com.google.mlkit.vision.face.Face> faces = Tasks.await(result);
			NUMBER_OF_FACE_DETECTED = faces.size();

			myBitmap.recycle();
			myBitmap = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return NUMBER_OF_FACE_DETECTED;
	}
	
	public int[] search(File[] pFiles) {
		if (pFiles == null) return null; 
		int[]results = new int[pFiles.length];
        //For each file in the directory... 
        for(int i =0;i<pFiles.length;i++){
        	results [i] = this.detectFace(pFiles[i]);
        }
		return results;
	}
	
	public int search(String pName) {
		if (pName == null) return -1; 
        //For each file in the directory... 
        int result = detectFace(pName);
		return result;
	}
}
