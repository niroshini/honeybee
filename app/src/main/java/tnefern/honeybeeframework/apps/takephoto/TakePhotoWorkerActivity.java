package tnefern.honeybeeframework.apps.takephoto;

import tnefern.honeybeeframework.worker.WorkerActivity;
import tnefern.honeybeeframework.worker.WorkerBee;

public class TakePhotoWorkerActivity extends WorkerActivity{

	@Override
	public WorkerBee getWorkerBee() {
		return new TakePhotoWorkerBee(this, null, null, 2, false);
	}

}
