package tnefern.honeybeeframework.apps.mandelbrot;

import tnefern.honeybeeframework.worker.WorkerActivity;
import tnefern.honeybeeframework.worker.WorkerBee;

public class MandelbrotWorkerActivity extends WorkerActivity {

	@Override
	public WorkerBee getWorkerBee() {
		return new MandelbrotWorkerBee(this, null, null, -1, false);
	}

}
