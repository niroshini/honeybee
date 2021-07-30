package tnefern.honeybeeframework.apps.mandelbrot;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.view.Display;
import android.view.View;
import android.widget.Button;

public class DrawMandel extends View {
	private ShapeDrawable mDrawable = null;
//	private ArrayList<ShapeDrawable> shapes = null;
	private ShapeDrawable[] shapes = null;
	// private Button exitButton = null;
	private Activity parentAct = null;

	private int max_iterations = 0;
	private int mandelSet[][] = null;
	private int cols = 0;
	private int rows = 0;
	private int height = 0;
	private int width = 0;
	int tileWidth = 0;
	int tileHeight = 0;

	

	public DrawMandel(Context context, int pIters, int pNumRows, int[][] pFullSet) {
		super(context);
		init();
//		int x = 10;
//		int y = 10;
//		int width = 300;
//		int height = 50;

		this.parentAct = (Activity) context;
		cols = pNumRows;//
		rows = pNumRows;//
		this.max_iterations = pIters;
		this.mandelSet = pFullSet;
		// drawMandel();
		// mDrawable = new ShapeDrawable(new OvalShape());
		// mDrawable.getPaint().setColor(0xff74AC23);
		// mDrawable.setBounds(x, y, x + width, y + height);
	}

	// public DrawMandel(Context context, int pMaxIter, int[][] pSet) {
	// this(context);
	// this.mandelSet = pSet;
	// this.max_iterations = pMaxIter;
	// }

	private void init() {
//		this.mandelSet = Offloader.getFullSet();
//		this.max_iterations = Offloader.getIterations();
	}

	protected void onDraw(Canvas canvas) {
		
//		if (shapes != null && shapes.length>0) {
//			for(int i =0;i<shapes.length;i++){
//				shapes[i].draw(canvas);
//			}
//			
//		}
		ShapeDrawable drawable = null;
		int i, j;
		
//		shapes = new ShapeDrawable[cols*rows];
//		int l =0;
//		System.out.println("tileWidth= " + tileWidth + "  tileHeight="
//				+ tileHeight);
		for (i = 0; i < this.mandelSet.length; i++) {
			for (j = 0; j < this.mandelSet[i].length; j++) {
//				int color = selectColor(this.mandelSet[i][j]);
				int color = Color.rgb(this.mandelSet[i][j], this.mandelSet[i][j], this.mandelSet[i][j]);
//				color2.
//				int color =this.mandelSet[i][j];
				drawable = new ShapeDrawable(new RectShape());
				drawable.getPaint().setColor(color);
				drawable.setBounds(j * tileWidth, i * tileHeight, j * tileWidth
						+ tileWidth, i * tileHeight + tileHeight);
//				shapes[l] = drawable;
//				l++;
				drawable.draw(canvas);
			}
		}
		
	}
/*
 *  Color color = new Color(gray, gray, gray);
                pic.set(i, N-1-j, color);
 */
	public void setParentAct(Activity parentAct) {
		this.parentAct = parentAct;
	}

	public Activity getParentAct() {
		return parentAct;
	}

	// @Override
	// protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
	// {
	// super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	// width = MeasureSpec.getSize(widthMeasureSpec);
	// height = MeasureSpec.getSize(heightMeasureSpec);
	// }

	@Override
	public void onSizeChanged(int pWidth, int pHeight, int pOldWidth,
			int pOldHeight) {
		super.onSizeChanged(pWidth, pHeight, pOldWidth, pOldHeight);
		width = this.getWidth();
		height = this.getHeight();
//		this.drawMandel();
		int xdim = width;
		int ydim = height;
//		System.out.println("width= " + xdim + "  height=" + ydim);
		if (xdim < cols || ydim < rows) {
			xdim = cols;
			ydim = rows;
			System.out.println("Resetting... width= " + xdim + "  height="
					+ ydim);
		}

		tileWidth = xdim / cols;
		tileHeight = ydim / rows;
	}

	public void drawMandelTest() {
		int xdim = width;
		int ydim = height;
		System.out.println("width= " + xdim + "  height=" + ydim);
		// if(xdim<cols || ydim<rows){
		// xdim = cols;
		// ydim = rows;
		// System.out.println("Resetting... width= " + xdim + "  height=" +
		// ydim);
		// }
		int c = 2;
		int r = 2;
		int tileWidth = xdim / c;
		int tileHeight = ydim / r;
		int test = 0;
		shapes = new ShapeDrawable[c*r];
		int k =0;
		for (int i = 0; i < r; i++) {
			// System.out.println("i= " + i);
			for (int j = 0; j < c; j++) {
//				shapes = new ArrayList<ShapeDrawable>();
				
				System.out.println("tileWidth= " + tileWidth + "  tileHeight="
						+ tileHeight);
				// int color = 0;
				// switch(test){
				// case 0:
				int color = selectColor2(test);
				// }

				// g.fillRect(i, j, 1, 1);
				ShapeDrawable drawable = new ShapeDrawable(new RectShape());
				drawable.getPaint().setColor(color);
				drawable.setBounds(j * tileWidth, i * tileHeight, j * tileWidth
						+ tileWidth, i * tileHeight + tileHeight);
				System.out.println("Bounds:  i=" + i + "  j=" + j
						+ "  j*tileWidth=" + j * tileWidth + "  i*tileHeight="
						+ i * tileHeight);
//				shapes.add(drawable);
				shapes[k] = drawable;
				test++;
				k++;
			}
		}

	}

	public void drawMandel() {

		int i, j;
		int xdim = width;
		int ydim = height;
//		System.out.println("width= " + xdim + "  height=" + ydim);
		if (xdim < cols || ydim < rows) {
			xdim = cols;
			ydim = rows;
			System.out.println("Resetting... width= " + xdim + "  height="
					+ ydim);
		}

		int tileWidth = xdim / cols;
		int tileHeight = ydim / rows;
		shapes = new ShapeDrawable[cols*rows];
		int l =0;
//		System.out.println("tileWidth= " + tileWidth + "  tileHeight="
//				+ tileHeight);
		for (i = 0; i < this.mandelSet.length; i++) {
			for (j = 0; j < this.mandelSet[i].length; j++) {
				int color = selectColor(this.mandelSet[i][j]);
				ShapeDrawable drawable = new ShapeDrawable(new RectShape());
				drawable.getPaint().setColor(color);
				drawable.setBounds(j * tileWidth, i * tileHeight, j * tileWidth
						+ tileWidth, i * tileHeight + tileHeight);
				shapes[l] = drawable;
				l++;
			}
		}

	}

	protected int selectColor2(int num_iterations) {

		// if (num_iterations > max_iterations)
		// return Color.BLACK;
		// else if (num_iterations > 9 * max_iterations / 10)
		// return Color.DKGRAY;
		// else if (num_iterations > 8 * max_iterations / 10)
		// return Color.GRAY;
		// else if (num_iterations > 7 * max_iterations / 10)
		// return Color.MAGENTA;
		// else if (num_iterations > 6 * max_iterations / 10)
		// return Color.CYAN;
		// else if (num_iterations > 5 * max_iterations / 10)
		// return Color.BLUE;
		// else if (num_iterations > 4 * max_iterations / 10)
		// return Color.GREEN;
		// else if (num_iterations > 3 * max_iterations / 10)
		// return Color.YELLOW;
		// else if (num_iterations > 2 * max_iterations / 10)
		// return Color.LTGRAY;
		// else if (num_iterations > 1 * max_iterations / 10)
//		return Color.RED;
		// else
		// return Color.WHITE;
		
		switch(num_iterations){
		case 0:
			return Color.RED;
		case 1:
			return Color.BLUE;
		case 2:
			return Color.YELLOW;
		case 3:
			return Color.CYAN;
		}
		return Color.WHITE;

	}

	protected int selectColor(int num_iterations) {

		if (num_iterations > max_iterations)
			return Color.BLACK;
		else if (num_iterations > 9 * max_iterations / 10)
			return Color.DKGRAY;
		else if (num_iterations > 8 * max_iterations / 10)
			return Color.GRAY;
		else if (num_iterations > 7 * max_iterations / 10)
			return Color.MAGENTA;
		else if (num_iterations > 6 * max_iterations / 10)
			return Color.CYAN;
		else if (num_iterations > 5 * max_iterations / 10)
			return Color.BLUE;
		else if (num_iterations > 4 * max_iterations / 10)
			return Color.GREEN;
		else if (num_iterations > 3 * max_iterations / 10)
			return Color.YELLOW;
		else if (num_iterations > 2 * max_iterations / 10)
			return Color.LTGRAY;
		else if (num_iterations > 1 * max_iterations / 10)
			return Color.RED;
		else
			return Color.WHITE;

	}

	
}
