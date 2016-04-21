package tnefern.honeybeeframework;

import tnefern.honeybeeframework.apps.facematch.FaceMatchDelegatorActivity;
import tnefern.honeybeeframework.apps.facematch.FaceMatchWorkerActivity;
import tnefern.honeybeeframework.apps.mandelbrot.MandelbrotDelegatorActivity;
import tnefern.honeybeeframework.apps.mandelbrot.MandelbrotWorkerActivity;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.worker.WorkerNotify;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

/**
 * This is the starting point of any applications implementing Honeybee.
 * Application specific intent should be given inside initDele() method.
 * 
 * @author tnefernando
 * 
 */
public class HoneybeeCrowdActivity extends Activity {

	// mode strings
	private static String[] modes = null;
	private static final int SELECT = 0;
	private static final int PHOTO_APP = 1;
	private static final int WRKR_MODE = 2;

	private Button exitButton = null;
	private Button workButton = null;
	private Button deleteButton = null;
	private Button lookforWorkersButton = null;
	private ArrayAdapter<String> modesArrayAdapter = null;
	private LinearLayout body = null;
	private View dView = null;
	private View wView = null;
	private RadioGroup radioGroup = null;

	public static final int CONNECTION_MODE = CommonConstants.CONNECTION_MODE_WIFIDIRECT;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_honeybee_crowd);
		this.init();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.honeybee_crowd, menu);
		return true;
	}

	/**
	 * The application implemented using Honeybee is given here. It is hardcoded
	 * for now. As can be seen, an intent representing the delegator view of the
	 * application, is created and passed on to startActivityForResult
	 */
	private void initDele() {
		if (lookforWorkersButton == null) {
			lookforWorkersButton = (Button) findViewById(R.id.btnLookforworkers);
			lookforWorkersButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent faceIntent = new Intent(HoneybeeCrowdActivity.this,
							FaceMatchDelegatorActivity.class);

					Intent mandelIntent = new Intent(
							HoneybeeCrowdActivity.this,
							MandelbrotDelegatorActivity.class);

					// state whether you want the Mandelbrot or
					// FaceDetection delegator to run
					startActivityForResult(mandelIntent, 0);
				}
			});
		}

	}

	private void initWorker(String pWorkerActivityClass) {
		final String className = pWorkerActivityClass;
		if (workButton == null) {
			workButton = (Button) findViewById(R.id.btnLookforWork);
			workButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (CONNECTION_MODE == CommonConstants.CONNECTION_MODE_WIFIDIRECT) {
						startWifiDirectService(className, v);
					} else if (CONNECTION_MODE == CommonConstants.CONNECTION_MODE_BLUETOOTH) {
						// if Bluetooth was chosen as method of communication...
					}
				}
			});

			if (deleteButton == null) {
				deleteButton = (Button) findViewById(R.id.btnDeleteJobDataMain);
				deleteButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						WorkerNotify.getInstance().deleteJobData();// niro
					}
				});
			}

		}
	}

	/**
	 * The application implemented using Honeybee is given here. It is hardcoded
	 * for now. As can be seen, an intent representing the worker view of the
	 * application, is created and passed on to startActivityForResult. This is
	 * the 'worker' version of initDele() method.
	 * 
	 * @param pClassName
	 * @param v
	 */
	public void startWifiDirectService(String pClassName, View v) {
		Intent faceIntent = new Intent(v.getContext(),
				FaceMatchWorkerActivity.class);

		Intent mandelIntent = new Intent(v.getContext(),
				MandelbrotWorkerActivity.class);

		// state whether you want the Mandelbrot or
		// FaceDetection worker to run

		this.startActivityForResult(mandelIntent, 0);
	}

	private void init() {
		initStringArr();
		exitButton = (Button) findViewById(R.id.btnExit);

		body = (LinearLayout) findViewById(R.id.linLayout);
		radioGroup = (RadioGroup) findViewById(R.id.radioGroupMode);
		modeSelect();
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				modeSelect();
			}
		});

		exitButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
				System.exit(1);
			}
		});

	}


	private void modeSelect() {
		int selectedId = radioGroup.getCheckedRadioButtonId();
		RadioButton btn = (RadioButton) findViewById(selectedId);
		if (btn.getText().equals(getString(R.string.strDelegator))) {
			if (wView != null) {
				if ((LinearLayout) wView.getParent() != null) {
					((LinearLayout) wView.getParent()).removeView(wView);
				}
			}
			if (dView == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				dView = layoutInflater.inflate(R.layout.delegator_layout, null);
			}
			body.removeViewInLayout(dView);
			body.addView(dView);
			initDele();

		} else {
			if (dView != null) {
				if ((LinearLayout) dView.getParent() != null) {
					((LinearLayout) dView.getParent()).removeView(dView);
				}
			}

			if (wView == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				wView = layoutInflater.inflate(R.layout.worker_layout, null);
			}
			body.removeViewInLayout(wView);
			body.addView(wView);
			initWorker("tnefern.honeybeeframework.apps.facematch.FaceMatchDelegatorActivity");
		}

	}

	private void initStringArr() {
		modes = new String[3];
		modes[SELECT] = getResources().getString(R.string.strSeleApp);
		modes[PHOTO_APP] = getResources().getString(R.string.strPhotoApp);
		modes[WRKR_MODE] = getResources().getString(R.string.strOtherApp);
	}


	public void onDestroy() {
		super.onDestroy();
	}
}
