package tnefern.honeybeeframework.apps.mandelbrot;

import java.util.Comparator;

import android.util.Log;

import tnefern.honeybeeframework.common.CompletedJob;

public class MandelResultComparator implements Comparator<CompletedJob> {

	@Override
	public int compare(CompletedJob lhs, CompletedJob rhs) {
//		MandelRowResult lm = null;
//		MandelRowResult rm = null;
//		if (lhs.data == null) {
//			lm = new MandelRowResult();
//			lm.valueArray = lhs.intArrayValue;
//			lm.fromIndex = lhs.intValue;
//		} else {
//			lm = (MandelRowResult) lhs.data;
//		}
//
//		if (rhs.data == null) {
//			rm = new MandelRowResult();
//			rm.valueArray = rhs.intArrayValue;
//			rm.fromIndex = rhs.intValue;
//		} else {
//			rm = (MandelRowResult) rhs.data;
//		}

		// return rm.fromIndex - lm.fromIndex;
		return lhs.intValue - rhs.intValue;
	}

}
