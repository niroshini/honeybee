package tnefern.honeybeeframework.apps.mandelbrot;


import tnefern.honeybeeframework.R;
import tnefern.honeybeeframework.stats.TimeMeter;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FinishedMandelbrotDelegatorActivity extends Activity {

	private int numRows = -1;
	private int iterations = -1;
	private int[][]results = null;
	DrawMandel mCustomDrawableView;
	View mandelResultView = null;
	TextView mandelText = null;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_delegator_layout);
		setContentView(R.layout.activity_mandel_finished_dele_layout);
		mandelResultView = findViewById(R.id.viewMandelImage);
		mandelText = (TextView) findViewById(R.id.lblMandelResultText);
		
		
		numRows = getIntent().getIntExtra(MandelConstants.NUMBER_OF_ROWS, 0);
		iterations = getIntent().getIntExtra(MandelConstants.NUMBER_OF_ITERATIONS, 0);
		results = MandelbrotResult.getInstance().getFinalResultArray(numRows);
		mCustomDrawableView = new DrawMandel(this, iterations, numRows, results);
		
//		mandelResultView = mCustomDrawableView;
		//setContentView(mCustomDrawableView);
//		setContentView(mCustomDrawableView, params)
	    
	    LinearLayout layout = new LinearLayout(this);
        // Define the LinearLayout's characteristics
        layout.setGravity(Gravity.CENTER);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Set generic layout parameters
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        TextView tx = new TextView(this);
        tx.setText(MandelbrotResult.getInstance().getCompareString()+" \n Iterations: "+iterations+ " No. of rows: "+numRows);
        layout.addView(tx, params); // Modify this

//        /yView custom = new myView(this);
        layout.addView(mCustomDrawableView, params); // Of course, this too

        setContentView(layout);
		
	}
	
//	private void getResults(){
//		MandelbrotResult.getInstance().getResults();
//	}
}
