package tnefern.honeybeeframework.apps.mandelbrot;

import java.util.ArrayList;

import tnefern.honeybeeframework.common.AppInfo;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.delegator.AppRequest;
import tnefern.honeybeeframework.delegator.QueenBee;

public class MandelbrotRequest implements AppRequest {

	private int numberOfRows = 300;// gives a numberOfRows*numberOfRows matrix
	private ArrayList<AppInfo> rowList = null;
	private int iter = 3000;
	private double xc = -0.5;// -0.5
	private double yc = 0.0;
	private double size = 2.0;
	private QueenBee mandelQueen = null;
	private StringBuffer commonString = null;

	public MandelbrotRequest(int pNumRowsCols, int pIter, double pX,
			double pY, double pSize) {
		this.numberOfRows = pNumRowsCols;
		this.iter = pIter;
		this.xc = pX;
		this.yc = pY;
		this.size = pSize;
		generateRowList();
	}

	public MandelbrotRequest() {
		generateRowList();
	}
	
	public int getNumberOfRows() {
		return numberOfRows;
	}

	public int getIter() {
		return iter;
	}

	
	public double getXc() {
		return xc;
	}

	public double getYc() {
		return yc;
	}

	public double getSize() {
		return size;
	}

	private void generateRowList() {
		commonString = new StringBuffer();
		commonString.append(xc);
		commonString.append(":");
		commonString.append(yc);
		commonString.append(":");
		commonString.append(size);
		commonString.append(":");
		commonString.append(iter);
		commonString.append(":");
		commonString.append(numberOfRows);
		commonString.append(":");

		rowList = new ArrayList<AppInfo>();
		for (int i = 0; i < numberOfRows; i++) {
			StringBuffer sBuf = new StringBuffer(commonString);
			sBuf.append(i);
			MandelInfo minfo = new MandelInfo(sBuf.toString(), String.valueOf(i));
			rowList.add(minfo);
		}
	}

	@Override
	public String[] getDistributedString() {
		String[] strArr = new String[numberOfRows];
		for (int i = 0; i < numberOfRows; i++) {
			strArr[i] = rowList.get(i).getStringInfo();
		}
		return strArr;
	}

	@Override
	public int getNumberOfJobs() {
		return numberOfRows;
	}

	@Override
	public ArrayList<AppInfo> getAppInfo() {
		return rowList;
	}

	@Override
	public int getMode() {
		return CommonConstants.READ_STRING_MODE;
	}

	@Override
	public QueenBee getQueenBee() {
		return mandelQueen;
	}

	@Override
	public void setQueenBee(QueenBee pBee) {
		this.mandelQueen = pBee;
	}

}
