package tnefern.honeybeeframework.apps.mandelbrot;

import android.content.Intent;
import android.os.Bundle;
import tnefern.honeybeeframework.apps.facematch.FaceConstants;
import tnefern.honeybeeframework.apps.facematch.FinishedFaceMatchDelegatorActivity;
import tnefern.honeybeeframework.common.CommonConstants;
import tnefern.honeybeeframework.common.JobPool;
import tnefern.honeybeeframework.delegator.AppRequest;
import tnefern.honeybeeframework.delegator.DelegatorActivity;

public class MandelbrotDelegatorActivity extends DelegatorActivity {

	private MandelbrotRequest manRequest = null;
	

	@Override
	public void initJobs() {
		manRequest = new MandelbrotRequest();
		manRequest.setQueenBee(new MandelbrotQueenBee(this));
		JobPool.getInstance().setStealMode(CommonConstants.READ_STRING_MODE);
	}

	@Override
	public AppRequest getAppRequest() {
		return manRequest;
	}

	@Override
	public void onJobDone() {
		super.onJobDone();
		Intent deleIntent = new Intent(this,
				FinishedMandelbrotDelegatorActivity.class);
		deleIntent.putExtra(MandelConstants.NUMBER_OF_ROWS,
				manRequest.getNumberOfRows());
		deleIntent.putExtra(MandelConstants.NUMBER_OF_ITERATIONS,
				manRequest.getIter());
		this.startActivityForResult(deleIntent,
				MandelConstants.FINISHED_DELEGATOR_MANDEL);

	}
}
