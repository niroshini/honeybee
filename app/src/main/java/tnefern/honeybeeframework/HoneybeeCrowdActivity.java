package tnefern.honeybeeframework;

import tnefern.honeybeeframework.apps.facematch.FaceMatchDelegatorActivity;
import tnefern.honeybeeframework.apps.facematch.FaceMatchWorkerActivity;
import tnefern.honeybeeframework.apps.mandelbrot.MandelbrotDelegatorActivity;
import tnefern.honeybeeframework.apps.mandelbrot.MandelbrotWorkerActivity;
import tnefern.honeybeeframework.apps.takephoto.TakePhotoDelegatorActivity;
import tnefern.honeybeeframework.apps.takephoto.TakePhotoWorkerActivity;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.worker.WorkerNotify;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the starting point of any applications implementing Honeybee.
 * Application specific intent should be given inside initDele() method.
 *
 * @author tnefernando
 */
public class HoneybeeCrowdActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    // mode strings
    private static       String[]             modes                    = null;
    private static final int                  SELECT                   = 0;
    private static final int                  FACE_MATCH_APP           = 1;
    private static final int                  TAKE_PHOTOS_APP          = 2;
    private static final int                  MANDELBROT_APP           = 3;
    public static final  int                  REQUEST_CODE_PERMISSIONS = 101;
    private              int                  selectedApp              = -1;
    private              Button               exitButton               = null;
    private              Button               workButton               = null;
    private              Button               deleteButton             = null;
    private              Button               lookforWorkersButton     = null;
    private              Spinner              spinner                  = null;
    private              ArrayAdapter<String> modesArrayAdapter        = null;
    private              LinearLayout         body                     = null;
    private              View                 dView                    = null;
    private              View                 wView                    = null;
    private              RadioGroup           radioGroup               = null;

    public static final int CONNECTION_MODE = CommonConstants.CONNECTION_MODE_WIFIDIRECT;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_honeybee_crowd);

        ActivityCompat.requestPermissions(this,
                                          new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                                       Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                       Manifest.permission.ACCESS_COARSE_LOCATION,
                                                       Manifest.permission.ACCESS_FINE_LOCATION,
                                                       Manifest.permission.ACCESS_WIFI_STATE,
                                                       Manifest.permission.CHANGE_WIFI_STATE,
                                                       Manifest.permission.INTERNET},
                                          REQUEST_CODE_PERMISSIONS);
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

                    Intent takephotoIntent = new Intent(HoneybeeCrowdActivity.this,
                                                        TakePhotoDelegatorActivity.class);

                    // state whether you want the Mandelbrot or
                    // FaceDetection delegator to run

                    switch (selectedApp) {
                        case FACE_MATCH_APP:
                            startActivityForResult(faceIntent, 0);
                            break;
                        case TAKE_PHOTOS_APP:
                            startActivityForResult(takephotoIntent, 0);
                            break;
                        case MANDELBROT_APP:
                            startActivityForResult(mandelIntent, 0);
                            break;
                    }

                }
            });
        }

    }

    private void initWorker() {
//		final String className = pWorkerActivityClass;
        if (workButton == null) {
            workButton = (Button) findViewById(R.id.btnLookforWork);
            workButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (CONNECTION_MODE == CommonConstants.CONNECTION_MODE_WIFIDIRECT) {
                        startWifiDirectService(v);


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
     * @param v
     */
    public void startWifiDirectService(View v) {
        Intent faceIntent = new Intent(v.getContext(),
                                       FaceMatchWorkerActivity.class);

        Intent mandelIntent = new Intent(v.getContext(),
                                         MandelbrotWorkerActivity.class);

        Intent takephotosIntent = new Intent(v.getContext(),
                                             TakePhotoWorkerActivity.class);


        // state whether you want the Mandelbrot or
        // FaceDetection worker to run

        this.startActivityForResult(faceIntent, 0);

        switch (selectedApp) {
            case FACE_MATCH_APP:
                this.startActivityForResult(faceIntent, 0);
                break;
            case TAKE_PHOTOS_APP:
                this.startActivityForResult(takephotosIntent, 0);
                break;
            case MANDELBROT_APP:
                this.startActivityForResult(mandelIntent, 0);
                break;
        }
    }

    private void init() {
        initStringArr();

        exitButton 			= (Button) findViewById(R.id.btnExit);
        body       			= (LinearLayout) findViewById(R.id.linLayout);
        radioGroup 			= (RadioGroup) findViewById(R.id.radioGroupMode);
        spinner    			= (Spinner) findViewById(R.id.spinner);

		ArrayAdapter<CharSequence> spinnerArrayAdapter = ArrayAdapter.createFromResource(this, R.array.modes_array, android.R.layout.simple_spinner_dropdown_item);
		spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

//		spinnerArrayAdapter.se
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setOnItemSelectedListener(this);
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
        int         selectedId = radioGroup.getCheckedRadioButtonId();
        RadioButton btn        = (RadioButton) findViewById(selectedId);
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
            initWorker();
        }

    }

    private void initStringArr() {
        modes                  = new String[5];
        modes[SELECT]          = getResources().getString(R.string.strSeleApp);
        modes[FACE_MATCH_APP]  = "Face detection";
        modes[TAKE_PHOTOS_APP] = "Take Photos";
        modes[MANDELBROT_APP]  = "Mandelbrot";
    }


    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        TextView v1 = ((TextView) adapterView.getChildAt(0));

        if (v1 != null) {
            v1.setTextColor(Color.YELLOW);
            v1.setBackgroundColor(Color.GRAY);
        }

        selectedApp = i;

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.init();
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
