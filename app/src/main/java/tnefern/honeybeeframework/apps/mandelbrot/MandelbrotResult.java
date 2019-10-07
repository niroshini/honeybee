package tnefern.honeybeeframework.apps.mandelbrot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.util.Log;

import tnefern.honeybeeframework.apps.facematch.FaceResult;
import tnefern.honeybeeframework.common.CompletedJob;
import tnefern.honeybeeframework.common.ConnectionFactory;
import tnefern.honeybeeframework.common.JobPool;
import tnefern.honeybeeframework.delegator.ResultFactory;
import tnefern.honeybeeframework.delegator.WorkerInfo;

public class MandelbrotResult extends ResultFactory {

	int numRows = -1;
	int iterations = -1;
	
	private static MandelbrotResult manResultInstance;
	private HashMap<String, Object> resultMap;

	private MandelbrotResult() {
		resultMap = new HashMap<String, Object>();

	}

	
	public static MandelbrotResult getInstance() {
		if (manResultInstance == null) {
			manResultInstance = new MandelbrotResult();
		}
		return manResultInstance;
	}

	public static MandelbrotResult getInstance(int pN, int pIter) {
		if (manResultInstance == null) {
			manResultInstance = new MandelbrotResult();
		}
		manResultInstance.iterations = pIter;
		manResultInstance.numRows = pN;
		return manResultInstance;
	}

	@Override
	public boolean checkResults(ArrayList<CompletedJob> pdone) {
		boolean res1 = JobPool.getInstance()
				.checkResults(this.resultMap, pdone);
		Mandelbrot m = new Mandelbrot(iterations, 1, numRows);

		Log.d("MandelbrotResult", "TEST1 = " + res1);
		long tt1 = System.currentTimeMillis();
				
		m.generateSet();
		long tt2 = System.currentTimeMillis();
		
		Log.d("MandelbrotResult", "serial time = " + (tt2-tt1));
		
		// sort the pdone list first by index ascending
		Collections.sort(pdone, new MandelResultComparator());

		Iterator<CompletedJob> iter = pdone.iterator();
		int[][] valArr = new int[pdone.size()][];
		int i = 0;
//		MandelRowResult manres = null;
		while (iter.hasNext()) {
			CompletedJob cj = iter.next();
//			if(cj.data == null){
//				manres = new MandelRowResult();
//				manres.fromIndex = cj.intValue
//			}
//			MandelRowResult manres = (MandelRowResult) cj.data;
//			valArr[i] = manres.valueArray;
			valArr[i] = cj.intArrayValue;
			i++;
		}
		return m.compareWithDistributed(valArr);
	}

	@Override
	public void addToMap(String pId, Object pResult) {
		this.resultMap.put(pId, (int[]) pResult);

	}

	// public HashMap<String,Object>getResults(){
	// return this.resultMap;
	// }

	public int[][] getFinalResultArray(int pMaxN) {
//		Iterator<Entry<String, Object>> it = this.resultMap.entrySet()
//				.iterator();
		int[][]results = new int[pMaxN][pMaxN];
//		ArrayList<Integer>intList = new ArrayList<Integer>();
//		while (it.hasNext()) {
//			Entry<String, Object> pairs = it.next();
//			String key = pairs.getKey();
//			intList.add(Integer.parseInt(key));
//		}
//		Collections.sort(intList);
		for(int i =0;i<pMaxN;i++){
			results[i] = (int[]) this.resultMap.get(String.valueOf(i));
		}
		
		return results;
	}
}
