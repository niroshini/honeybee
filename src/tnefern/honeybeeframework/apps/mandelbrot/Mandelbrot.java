package tnefern.honeybeeframework.apps.mandelbrot;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.util.Date;

import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.FileFactory;

import android.util.Log;

/**
 * 
 * @author tnfernando
 */
public class Mandelbrot {

	private int resultSet[][] = null;
	private int testResultSet[][] = null;
	private int N = -1;// 512
	private int iter = -1;
	private int nodes = -1;
	private double xc = -0.5;// -0.5
	private double yc = 0.0;
	private double size = 2.0;

	public Mandelbrot(int pIter, int pNodes, int pN) {
		this.iter = pIter;
		this.nodes = pNodes;

		this.N = pN;
	}

	public Mandelbrot(int pIter, int pN, double pXc, double pYc, double pSize) {
		this.iter = pIter;
		this.N = pN;
		this.xc = pXc;
		this.yc = pYc;
		this.size = pSize;
	}

	public int getRowEach() {
		// System.out.println("getRowEach : N= " + N + "  nodes= " + nodes);
		return (int) N / nodes;
	}

	public int getN() {
		return N;
	}

	public String[] getDistributedString() {
		String[] strArr = new String[nodes];

		for (int i = 0; i < nodes; i++) {
			StringBuffer sBuf = new StringBuffer();
			// double y = yc + (size / nodes * i);
			// double x = xc + (size / nodes * i);
			sBuf.append(xc);
			sBuf.append(":");
			sBuf.append(yc);
			sBuf.append(":");
			sBuf.append(size);
			sBuf.append(":");
			sBuf.append(iter);
			sBuf.append(":");
			sBuf.append(N);
			sBuf.append(":");
			sBuf.append(nodes);
			sBuf.append(":");
			sBuf.append(i);
//			sBuf.append(CommonConstants.PARTITION_BREAK);// *
			strArr[i] = sBuf.toString();
		}
		return strArr;
	}

	public String getOffloadingString() {
		String strParam = "";

		StringBuffer sBuf = new StringBuffer();
		sBuf.append(xc);
		sBuf.append(":");
		sBuf.append(yc);
		sBuf.append(":");
		sBuf.append(size);
		sBuf.append(":");
		sBuf.append(iter);
		sBuf.append(":");
		sBuf.append(N);
		sBuf.append("*");
		strParam = sBuf.toString();
		return strParam;
	}

	public void distribute() {
		testResultSet = new int[N][N];
		for (int i = 0; i < nodes; i++) {
			// double x = xc + (size / nodes * i);
			DistributedMandelbrot disObj = new DistributedMandelbrot(xc, yc,
					size, iter, N, nodes, i);
			disObj.generateDistributed();
			int[][] partialSet = disObj.getPartialSet();

			for (int j = 0; j < N / nodes; j++) {
				for (int k = 0; k < N; k++) {
					testResultSet[j + N / nodes * i][k] = partialSet[j][k];
				}

			}
		}
	}

	public byte[] generateOffloadedSet() throws IOException {

		ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
		DataOutputStream datastream = new DataOutputStream(bytestream);

		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				double x0 = xc - size / 2 + size * i / N;
				double y0 = yc - size / 2 + size * j / N;

				Complex z0 = new Complex(x0, y0);
				int gray = iter - mand(z0, iter);
				datastream.writeInt(gray);
			}
		}
		datastream.close();
		datastream = null;
		bytestream.close();
		return bytestream.toByteArray();
	}

	public void print() {
		System.out.println("resultSet :");
		for (int i = 0; i < this.getResultSet().length; i++) {
			for (int j = 0; j < this.getResultSet()[i].length; j++) {
				System.out.print(this.getResultSet()[i][j] + " , ");
			}
			System.out.println();
		}

		System.out.println();
		System.out.println();
		System.out.println("testResultSet :");
		for (int i = 0; i < this.testResultSet.length; i++) {
			for (int j = 0; j < this.testResultSet[i].length; j++) {
				System.out.print(this.testResultSet[i][j] + " , ");
			}
			System.out.println();
		}
	}

	public boolean compareWithDistributed(int distSet[][]) {
		boolean compare = true;
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				if (distSet[i][j] != getResultSet()[i][j]) {
					System.out.println("FALSE at - i =" + i + " j=" + j
							+ " resultSet=" + getResultSet()[i][j]
							+ " testResultSet=" + distSet[i][j]);
					String s = DateFormat.getDateTimeInstance().format(new Date())+ "FALSE at - i =" + i + " j=" + j
					+ " resultSet=" + getResultSet()[i][j]
					        							+ " testResultSet=" + distSet[i][j]+"\n";
					try {
						FileFactory.getInstance().writeFile(CommonConstants.DEBUG_FILE_PATH, s);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					compare = false;
				}
			}
		}
		Log.d("results"," compare set : N = "+N+" iters= "+iter);
		return compare;
	}

	// public boolean compareWithDistributed(int distSet[][], Form pForm) {
	// boolean compare = true;
	// for (int i = 0; i < N; i++) {
	// for (int j = 0; j < N; j++) {
	// if (distSet[i][j] != getResultSet()[i][j]) {
	// System.out.println("FALSE at - i =" + i + " j=" + j + " resultSet=" +
	// getResultSet()[i][j] + " testResultSet=" + distSet[i][j]);
	// // pForm.append("FALSE at - i =" + i + " j=" + j + " resultSet=" +
	// getResultSet()[i][j] + " testResultSet=" + distSet[i][j] + "\n");
	// compare = false;
	// }
	// }
	// }
	// return compare;
	// }

	public boolean compareWithDistributedPartial(int distSet[][], int pIndex,
			int pRowEach) {
		boolean compare = true;
		int ind = pIndex * pRowEach;
		System.out.println("comparison starts at " + ind);
		for (int i = 0; i < pRowEach; i++) {
			for (int j = 0; j < N; j++) {
				if (distSet[ind + i][j] != resultSet[ind + i][j]) {
					System.out.println("FALSE at - i =" + (ind + i) + " j=" + j
							+ " resultSet=" + resultSet[(ind + i)][j]
							+ " testResultSet=" + distSet[(ind + i)][j]);
					compare = false;
				}
			}
		}
		return compare;
	}

	public void printDist(int distSet[][]) {
		System.out.println("resultSet :");
		for (int i = 0; i < this.getResultSet().length; i++) {
			for (int j = 0; j < this.getResultSet()[i].length; j++) {
				System.out.print(this.getResultSet()[i][j] + " , ");
			}
			System.out.println();
		}

		System.out.println();
		System.out.println();
		System.out.println("DistSet :");
		for (int i = 0; i < distSet.length; i++) {
			for (int j = 0; j < distSet[i].length; j++) {
				System.out.print(distSet[i][j] + " , ");
			}
			System.out.println();
		}
	}

	public boolean compareSets() {
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				if (testResultSet[i][j] != getResultSet()[i][j]) {
					System.out.println("FALSE at - i =" + i + " j=" + j
							+ " resultSet=" + getResultSet()[i][j]
							+ " testResultSet=" + testResultSet[i][j]);
					return false;
				}
			}
		}
		return true;
	}

	public int[][] getResultSet() {
		return this.resultSet;
	}

	public void generateSet() {
		double x0;
		double y0;
		// double xt = (xc - (size / 2));
		// double yt = (yc - (size / 2));
		// double sN = size / N;
		resultSet = new int[N][N];
		Complex z0 = new Complex();
		for (int i = 0; i < N; i++) {
			// Log.d("Mandel", " at "+i);
			for (int j = 0; j < N; j++) {

				x0 = xc - size / 2 + size * i / N;
				y0 = yc - size / 2 + size * j / N;
				// x0 = xt + sN * i;
				// y0 = yt + sN * j;
				z0.setRe(x0);
				z0.setIm(y0);
				resultSet[i][j] = iter - mand(z0, iter);
			}
		}
	}

	
	
	/*
	 * for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                double x0 = xc - size/2 + size*i/N;
                double y0 = yc - size/2 + size*j/N;
                Complex z0 = new Complex(x0, y0);
                int gray = max - mand(z0, max);
                Color color = new Color(gray, gray, gray);
                pic.set(i, N-1-j, color);
            }
        }
	 */
	
	
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

	
	/*
	 * public static int mand(Complex z0, int d) { 
   Complex z = z0; 
   for (int t = 0; t < d; t++) { 
       if (z.abs() >= 2.0) return t; 
       z = z.times(z).plus(z0); 
   }   
   return d; 
} 
	 */
	/**
	 * @param iter
	 *            the iter to set
	 */
	public void setIter(int iter) {
		this.iter = iter;
	}

	class DistributedMandelbrot {

		double dXc = 0.0;
		double dYc = 0.0;
		double dSize = 0.0;
		int dIter = -1;
		int dN = -1;
		int dNodes = -1;
		int set[][] = null;
		int dIndex = -1;

		DistributedMandelbrot(double pXc, double pYc, double pSize, int pIter,
				int pN, int pNodes, int pIndex) {
			dXc = pXc;
			dYc = pYc;
			dSize = pSize;
			dIter = pIter;
			dN = pN;
			dNodes = pNodes;
			dIndex = pIndex;
		}

		public void generateDistributed() {

			int rowEach = (int) (dN / dNodes);
			int ind = dIndex * rowEach;
			int limit = ind + rowEach;
			set = new int[rowEach][dN];
			for (int i = ind; i < limit; i++) {
				for (int j = 0; j < dN; j++) {

					double x0 = dXc - dSize / 2 + dSize * i / dN;
					double y0 = dYc - dSize / 2 + dSize * j / dN;
					Complex z0 = new Complex(x0, y0);
					int gray = dIter - mand(z0, dIter);
					set[i - ind][j] = gray;
				}
			}

		}

		public int[][] getPartialSet() {
			return set;
		}
	}

	

	public static void main(String[] args) {
		// Mandelbrot mandel = new Mandelbrot(2000, 2, 200);
		// mandel.generateSet();
		// mandel.distribute();
		// System.out.println(mandel.compareSets());
		
		// mandel.print();
	}
}
