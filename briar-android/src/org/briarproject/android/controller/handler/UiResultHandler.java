package org.briarproject.android.controller.handler;

import android.app.Activity;

public abstract class UiResultHandler<R> implements ResultHandler<R> {

	private final Activity activity;

	public UiResultHandler(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void onResult(final R result) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				onResultUi(result);
			}
		});
	}

	public abstract void onResultUi(R result);
}