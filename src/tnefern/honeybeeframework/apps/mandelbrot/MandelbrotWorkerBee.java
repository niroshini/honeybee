package tnefern.honeybeeframework.apps.mandelbrot;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import android.content.Context;
import android.util.Log;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.CompletedJob;
import tnefern.honeybeeframework.common.FileFactory;
import tnefern.honeybeeframework.common.JobParams;
import tnefern.honeybeeframework.common.JobPool;
import tnefern.honeybeeframework.worker.WorkerBee;

public class MandelbrotWorkerBee extends WorkerBee {
	private int numberOfRows = -1;// gives a numberOfRows*numberOfRows matrix
	private int iter = -1;
	private double xc = -1;// -0.5
	private double yc = -1;
	private double size = -1;
	private int index = 0;

	public MandelbrotWorkerBee(Context pAct, String pActivityClass,
			JobParams pMsg, int pIndex, boolean stolen) {
		super(pAct, pActivityClass, pMsg, pIndex, stolen);
	}

	@Override
	public CompletedJob doAppSpecificJob(Object pParam) {
		if (pParam != null) {
			if (pParam instanceof String) {
				String param = (String) pParam;

				String[] mandelAttr = FileFactory.getInstance().tokenize(param,
						":", 6);
				xc = Double.parseDouble(mandelAttr[0]);
				yc = Double.parseDouble(mandelAttr[1]);
				size = Double.parseDouble(mandelAttr[2]);
				iter = Integer.parseInt(mandelAttr[3]);
				numberOfRows = Integer.parseInt(mandelAttr[4]);
				index = Integer.parseInt(mandelAttr[5]);
				String s = " doing work from index " + index;
				Log.d("MandelbrotQueenBee", s);
//				MandelRowResult manres = new MandelRowResult();
//				manres.fromIndex = index;
//				manres.valueArray = generateOneRow();

				CompletedJob cj = new CompletedJob();
				cj.intValue = index;
				cj.intArrayValue = generateOneRow();
				cj.mode = CommonConstants.READ_COMPLETED_JOB_OBJECT_ARRAY_MODE;
				cj.mixedModeArray = new int[] { CommonConstants.READ_INT_MODE,
						CommonConstants.READ_INT_ARRAY_MODE };
				cj.stringValue = param;
				cj.id = String.valueOf(index);

				// CompletedJob cj = new CompletedJob(
				// CommonConstants.READ_MIXED_MODE, param, index,
				// manres);
				MandelbrotResult.getInstance().addToMap(String.valueOf(index),
						cj.intArrayValue);
				JobPool.getInstance().incrementDoneJobCount();
				MandelbrotResult.getInstance().incrementDeleDoneJobs();
				return cj;
			}
		}
		return null;
	}

	private byte[] generateOneRowBytes() {

		ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
		DataOutputStream datastream = new DataOutputStream(bytestream);

		Complex z0 = new Complex();
		double x0 = 0.0;
		double y0 = 0.0;
		int[] results = new int[numberOfRows];
		int i = index;
		// for (int i = index; i < index+numberOfRows; i++) {
		for (int j = 0; j < numberOfRows; j++) {
			x0 = this.xc - size / 2 + size * i / numberOfRows;
			y0 = yc - size / 2 + size * j / numberOfRows;
			z0.setIm(y0);
			z0.setRe(x0);
			results[j] = iter - mand(z0, iter);
			try {
				datastream.writeInt(results[j]);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					datastream.close();
					datastream = null;
					bytestream.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}
		// }
		z0 = null;
		return bytestream.toByteArray();
	}

	private int[] generateOneRow() {

		Complex z0 = new Complex();
		double x0 = 0.0;
		double y0 = 0.0;
		int[] results = new int[numberOfRows];
		int i = index;
		// for (int i = index; i < index+numberOfRows; i++) {
		for (int j = 0; j < numberOfRows; j++) {
			x0 = this.xc - size / 2 + size * i / numberOfRows;
			y0 = yc - size / 2 + size * j / numberOfRows;
			z0.setIm(y0);
			z0.setRe(x0);
			results[j] = iter - mand(z0, iter);
		}
		// }
		z0 = null;
		return results;
	}

	/**
	 * For each pixel, the method main() in Mandelbrot computes the point z0
	 * corresponding to the pixel and computes 255 - mand(z0, 255) to represent
	 * the grayscale value of the pixel.
	 * 
	 * @param z0
	 * @param max
	 * @return
	 */
	// return number of iterations to check if c = a + ib is in Mandelbrot set
	private int mand(Complex z0, int max) {
		Complex z = z0;
		for (int t = 0; t < max; t++) {
			if (z.abs() > 2.0) {
				return t;
			}
			z = z.times(z).plus(z0);
		}
		return max;
	}
}
