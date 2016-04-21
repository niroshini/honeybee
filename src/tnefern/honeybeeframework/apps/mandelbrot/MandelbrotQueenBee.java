package tnefern.honeybeeframework.apps.mandelbrot;


import android.app.Activity;
import android.util.Log;
import tnefern.honeybeeframework.apps.facematch.FaceResult;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.CompletedJob;
import tnefern.honeybeeframework.common.FileFactory;
import tnefern.honeybeeframework.common.Job;
import tnefern.honeybeeframework.common.JobInitializer;
import tnefern.honeybeeframework.common.JobPool;
import tnefern.honeybeeframework.delegator.QueenBee;
import tnefern.honeybeeframework.delegator.ResultFactory;

public class MandelbrotQueenBee extends QueenBee {
	private int numberOfRows = -1;// gives a numberOfRows*numberOfRows matrix
	private int iter = -1;
	private double xc = -1;// -0.5
	private double yc = -1;
	private double size = -1;
	private int index = 0;

	public MandelbrotQueenBee(Activity pAct) {
		super(pAct);
	}

	
	public void setStealMode() {
		JobInitializer.getInstance(getParentContext()).setStealMode(
				CommonConstants.READ_STRING_MODE);
		JobPool.getInstance().setStealMode(CommonConstants.READ_STRING_MODE);
	}
	
	@Override
	public CompletedJob doAppSpecificJob(Object pParam) {
		if(pParam!=null){
			if(pParam instanceof Job){
				Job job = (Job) pParam;
				String param = job.jobParams;
				String[] mandelAttr = FileFactory.getInstance().tokenize(
						param, ":", 6);
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
				CompletedJob cj = new CompletedJob(CommonConstants.READ_INT_ARRAY_MODE,
						String.valueOf(index), index, null);
				cj.id = String.valueOf(index);
				cj.intArrayValue =generateOneRow();
				MandelbrotResult.getInstance().addToMap(String.valueOf(index),
						cj.intArrayValue);
				JobPool.getInstance().incrementDoneJobCount();
				MandelbrotResult.getInstance().incrementDeleDoneJobs();
				return cj;
			}
		}
		return null;
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

	@Override
	public ResultFactory getResultFactory() {
		return MandelbrotResult.getInstance(numberOfRows, iter);
	}

}
