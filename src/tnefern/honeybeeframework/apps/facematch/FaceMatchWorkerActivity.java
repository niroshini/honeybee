package tnefern.honeybeeframework.apps.facematch;

import tnefern.honeybeeframework.worker.WorkerActivity;
import tnefern.honeybeeframework.worker.WorkerBee;

public class FaceMatchWorkerActivity extends WorkerActivity{

	@Override
	public WorkerBee getWorkerBee() {
		return new FaceMatchWorkerBee(this, null, null, 2, false);
	}

}
