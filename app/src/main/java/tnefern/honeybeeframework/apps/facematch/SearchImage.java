package tnefern.honeybeeframework.apps.facematch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.util.Log;

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
//		if(!pFile.contains("Flower")){
			try {
//				BitmapScaler scaler = new BitmapScaler(new File(pFile), 1000);
//				myBitmap = scaler.getScaled();
				
				myBitmap = BitmapFactory.decodeFile(pFile, bitmapFatoryOptions);
				width = myBitmap.getWidth();
				height = myBitmap.getHeight();
				detectedFaces = new FaceDetector.Face[NUMBER_OF_FACES];
				faceDetector = new FaceDetector(width, height, NUMBER_OF_FACES);
				NUMBER_OF_FACE_DETECTED = faceDetector.findFaces(myBitmap,
						detectedFaces);
//				System.out.println(pFile+" : "+"Number of faces = " + NUMBER_OF_FACE_DETECTED);
				myBitmap.recycle();
				myBitmap = null;
				faceDetector = null;
				System.gc();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//		}
		
//		imageView.setImageBitmap(scaler.getScaled());
		
//		myBitmap = BitmapFactory.decodeFile(pFile, bitmapFatoryOptions);
//		width = myBitmap.getWidth();
//		height = myBitmap.getHeight();
//		Log.d("SearchImage", "original width = "+width+" o height = "+height);//w = 1024
		
		
		
		

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
