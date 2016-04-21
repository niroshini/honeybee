package tnefern.honeybeeframework.apps.facematch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import android.util.Log;

import tnefern.honeybeeframework.common.CompletedJob;
import tnefern.honeybeeframework.common.JobPool;
import tnefern.honeybeeframework.delegator.ResultFactory;

public class FaceResult extends ResultFactory {

	private static FaceResult faceResultInstance;
	private HashMap<String, Object> resultMap;

	private Collection<File> filesInfolder = null;

	private FaceResult() {
		resultMap = new HashMap<String, Object>();

	}

	public static FaceResult getInstance() {
		if (faceResultInstance == null) {
			faceResultInstance = new FaceResult();
		}
		return faceResultInstance;
	}

	public void addToMap(String pId, Object pResult) {
		this.resultMap.put(pId, Integer.valueOf((Integer) pResult));
	}

	public int getNumberOfFaces(String pName) {
		if (this.resultMap.containsKey(pName)) {
			return ((Integer) this.resultMap.get(pName)).intValue();
		} else {
			return -1;
		}
	}

	public void setFileList(Collection<File> pfilesInfolder) {
		this.filesInfolder = pfilesInfolder;
	}

	public Collection<File> getFilesInFolder() {
		return this.filesInfolder;
	}

	public boolean checkResults(ArrayList<CompletedJob> pdone) {
		// first you must edit the contents of the resultMap so that the
		// fileNames contain the full path. This is so that we can compare the
		// filenames with the ones in allJobs[]
		
		 Set<String> keySet = resultMap.keySet();
		 Iterator<String> keySetIterator = keySet.iterator();
		 while (keySetIterator.hasNext()) {
		    String key = keySetIterator.next();
		 }
		return JobPool.getInstance().checkResults(this.resultMap, pdone);
	}
}
