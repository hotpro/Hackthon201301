package com.dianping.hackthon;

import java.util.UUID;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.dianping.hackthon.net.Log;
import com.dianping.taiji.image.ReverseWaveFilter;
import com.dianping.taiji.image.TaijiReverseTransformFilter;
import com.jabistudio.androidjhlabs.filter.util.AndroidUtils;

public class CaptchaActivity extends NovaActivity implements
		OnSeekBarChangeListener, RequestHandler<HttpRequest, HttpResponse> {

	public static String API_DAIMON = "http://42.96.139.35:8080/";
	public static String API_DAIMON1 = "http://192.168.62.112:8090/";
	public static String API_DAIMON2 = "http://192.168.32.58:8080/";

	public static final String TAG = CaptchaActivity1.class.getSimpleName();

	Handler delayHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				filterRealInUIThread();
			} else if (msg.what == 2) {
				submit();
			} else if (msg.what == 3) {
				dismissDialog();
			} else if (msg.what == 4) {
				setResult(RESULT_OK);
				finish();
			} else if (msg.what == 5) {
				if (demoBack) {
					progress -= demoStep;
				} else {
					progress += demoStep;
				}
				if (progress > demoMax) {
					progress = demoMax;
					demoBack = true;
				}

				if (progress < 0) {
					progress = 0;
				}
				seekbar.setProgress(progress);
				if (progress != 0) {
					sendEmptyMessageDelayed(5, demoDuration);
				}
			} else if (msg.what == 6) {
				showDemo();
			} else if (msg.what == 7) {
				play();
			}
		}
	};
	Drawable drawable;
	TaijiReverseTransformFilter filter = new TaijiReverseTransformFilter();
	private boolean filterInUIThread = true;
	private int[] mColors;
	protected Bitmap mFilterBitmap;
	private NetworkPhotoView mOriginalImageView;
	private ProgressDialog mProgressDialog;;
	private int progress;

	Bitmap scaledBitmap;

	private SeekBar seekbar;

	private TextView text;

	private TextView textHint;

	private String uid = UUID.randomUUID().toString();

	private void filterInOtherThread() {
		final int width = mOriginalImageView.getDrawable().getIntrinsicWidth();
		final int height = mOriginalImageView.getDrawable()
				.getIntrinsicHeight();

		if (mColors == null) {
			mColors = AndroidUtils.drawableToIntArray(mOriginalImageView
					.getDrawable());
		}
		mProgressDialog = ProgressDialog.show(this, "", "Wait......");

		Thread thread = new Thread() {
			public void run() {

				ReverseWaveFilter reverseWaveFilter = new ReverseWaveFilter(
						progress, (float) height / 360);

				mColors = reverseWaveFilter.filter(mColors, width, height);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						setModifyView(mColors, getValue(width),
								getValue(height));
					}
				});
				mProgressDialog.dismiss();
			}
		};
		thread.setDaemon(true);
		thread.start();
	}

	private void filterInUIThread() {
		Log.i(TAG, "filterInUIThread progress: " + this.progress);
		if (!mOriginalImageView.isImageRetrieve()) {
			return;
		}
		if (scaledBitmap == null) {
			Log.i(TAG, "new scaledBitmap");
			scaledBitmap = AndroidUtils.drawableToBitmap(mOriginalImageView
					.getDrawable());
		}

		int width = this.scaledBitmap.getWidth();
		int height = this.scaledBitmap.getHeight();

		if (mColors == null) {
			Log.i(TAG, "new mColors");
			mColors = AndroidUtils.bitmapToIntArray(scaledBitmap);
		}

		setModifyView(filter.filter(mColors, width, height, this.progress),
				getValue(width), getValue(height));
	}

	private void filterRealInUIThread() {
		Log.i(TAG, "filterRealInUIThread progress: " + this.progress);
		if (!mOriginalImageView.isImageRetrieve()) {
			return;
		}
		if (this.drawable == null) {
			Log.i(TAG, "filterRealInUIThread new drawable");
			this.drawable = mOriginalImageView.getDrawable();
		}
		int width = this.drawable.getIntrinsicWidth();
		int height = this.drawable.getIntrinsicHeight();
		int[] colors = AndroidUtils.drawableToIntArray(this.drawable);

		setModifyView(filter.filter(colors, width, height, this.progress),
				getValue(width), getValue(height));
	}

	private int getValue(int value) {
		if (value <= 0) {
			value = 1;
		}
		return value;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_fragment);
		seekbar = (SeekBar) findViewById(R.id.seekbar);
		seekbar.setOnSeekBarChangeListener(this);

		text = (TextView) findViewById(R.id.text);
		textHint = (TextView) findViewById(R.id.text_hint);
		mOriginalImageView = (NetworkPhotoView) findViewById(R.id.image);
		mOriginalImageView.setImage(API_DAIMON + "image?token="
				+ Environment.deviceId() + "&uid=" + uid);
		// delayHandler.sendEmptyMessageDelayed(6, 1000);
		showDemo1();

		setTitleButton("提交", new OnClickListener() {

			@Override
			public void onClick(View v) {
				submit();
			}
		});
	}
	
	public void submit() {
		HttpRequest req = BasicHttpRequest.httpPost(API_DAIMON
				+ "validate?answer=" + progress + "&token="
				+ Environment.deviceId(), "", "");
		httpService().exec(req, CaptchaActivity.this);
	}

	public void showDemo1() {
		showToastDialog("请将图像还原成正常状态");
		delayHandler.sendEmptyMessageDelayed(3, 2000);
	}

	@Override
	public void onDestroy() {
		if (mFilterBitmap != null) {
			mFilterBitmap.recycle();
			mFilterBitmap = null;
		}

		delayHandler.removeMessages(1);
		delayHandler.removeMessages(2);
		delayHandler.removeMessages(3);
		delayHandler.removeMessages(4);
		delayHandler.removeMessages(5);
		delayHandler.removeMessages(6);
		super.onDestroy();
	}

	private int duration = 1000;

	@Override
	public void onRequestFailed(HttpRequest req, HttpResponse resp) {
		Toast.makeText(this, "验证失败！！！！！！- fail", Toast.LENGTH_SHORT).show();
		showToastDialog("验证失败，请重试", false);
		delayHandler.sendEmptyMessageDelayed(3, duration);
	}

	@Override
	public void onRequestFinish(HttpRequest req, HttpResponse resp) {
		if (resp.result() instanceof byte[]) {
			JSONObject json;
			try {
				json = new JSONObject(new String((byte[]) resp.result()));
				Log.i(TAG, "resp: " + json.toString());
				int code = json.optInt("code");
				if (code == 200) {
					// Toast.makeText(this, "验证成功！", Toast.LENGTH_SHORT).show();
					showToastDialog("验证成功！", true);
					delayHandler.sendEmptyMessageDelayed(4, duration);
				} else {
					// Toast.makeText(this, "验证失败！请重试！", Toast.LENGTH_SHORT)
					// .show();
					showToastDialog("验证失败，请重试", false);
					delayHandler.sendEmptyMessageDelayed(3, duration);
					reset();
				}
			} catch (Exception e) {
				e.printStackTrace();
				// Toast.makeText(this, "验证失败！！！－Exception", Toast.LENGTH_SHORT)
				// .show();
				showToastDialog("验证失败，请重试", false);
				delayHandler.sendEmptyMessageDelayed(3, duration);
			}

		}

	}

	@Override
	public void onRequestProgress(HttpRequest req, int count, int total) {

	}

	@Override
	public void onRequestStart(HttpRequest req) {

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		if (this.drawable == null) {
			Log.i(TAG, "new drawable");
			this.drawable = mOriginalImageView.getDrawable();
		}
		int width = this.drawable.getIntrinsicWidth();
		int height = this.drawable.getIntrinsicHeight();
		scaledBitmap = Bitmap.createScaledBitmap(
				AndroidUtils.drawableToBitmap(this.drawable), width / 2,
				height / 2, false);
		textHint.setVisibility(View.GONE);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		text.setText("progress: " + progress);
		this.progress = progress;
		if (filterInUIThread) {
			filterInUIThread();
			delayHandler.removeMessages(1);
			delayHandler.sendEmptyMessageDelayed(1, 200);
		}
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		Log.i(TAG, "onStopTrackingTouch");
//		delayHandler.sendEmptyMessageDelayed(2, 200);
//		Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
		play();
	}
	
	boolean play;
	int number = 0;
	public void play() {
		if (number > 4) {
			titleButton.setBackgroundResource(R.drawable.ic_titlebar_btn_bg);
			return;
		}
		if (play) {
			titleButton.setBackgroundResource(R.drawable.ic_titlebar_btn_bg_d);
			
		} else {
			titleButton.setBackgroundResource(R.drawable.ic_titlebar_btn_bg_u);
		}
		
		play = !play;
		number++;
		delayHandler.sendEmptyMessageDelayed(7, 100);
	}

	private void reset() {
		uid = UUID.randomUUID().toString();
		delayHandler.removeMessages(1);
		mOriginalImageView.setImage(API_DAIMON + "image?token="
				+ Environment.deviceId() + "&uid=" + uid);
		seekbar.setProgress(0);
		drawable = null;
		scaledBitmap = null;
		textHint.setVisibility(View.VISIBLE);
		mColors = null;
	}

	protected void setModifyView(int[] colors, int width, int height) {
		mOriginalImageView.setWillNotDraw(true);

		if (mFilterBitmap != null) {
			mFilterBitmap.recycle();
			mFilterBitmap = null;
		}

		mFilterBitmap = Bitmap.createBitmap(colors, 0, width, width, height,
				Bitmap.Config.ARGB_8888);
		mOriginalImageView.setImageBitmap(mFilterBitmap);

		mOriginalImageView.setWillNotDraw(false);
		mOriginalImageView.postInvalidate();
	}

	protected Dialog managedToast;

	/**
	 * 显示Progress Dialog.
	 * 
	 * @param title
	 * @param cancelListener
	 */
	public void showToastDialog(String title, boolean success) {
		if (isDestoryed)
			return;
		dismissDialog();

		BeautifulToast dlg = new BeautifulToast(this);
		dlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {

				managedToast = null;
			}
		});
		dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_SEARCH)
					return true;
				return false;
			}
		});
		managedToast = dlg;
		dlg.setMessage(title == null ? "载入中..." : title);
		dlg.show();
		dlg.setMessage(title == null ? "载入中..." : title, success);
	}

	public void showToastDialog(String title) {
		if (isDestoryed)
			return;
		dismissDialog();

		BeautifulToast dlg = new BeautifulToast(this);
		dlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {

				managedToast = null;
			}
		});
		dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_SEARCH)
					return true;
				return false;
			}
		});
		managedToast = dlg;
		dlg.setMessage(title == null ? "载入中..." : title);
		dlg.show();
		dlg.btn().setVisibility(View.GONE);

	}

	public void dismissDialog() {
		if (isDestoryed)
			return;
		if (managedDialog != null) {
			if (managedDialog.isShowing())
				managedDialog.dismiss();
			managedDialog = null;
		}

		if (managedToast != null) {
			if (managedToast.isShowing())
				managedToast.dismiss();
			managedToast = null;
		}

	}

	private int demoDuration = 2;
	private int demoStep = 4;
	private int demoMax = 10;
	private boolean demoBack;

	private void showDemo() {
		if (!mOriginalImageView.isImageRetrieve()) {
			delayHandler.sendEmptyMessageDelayed(6, 100);
			return;
		}
		if (this.drawable == null) {
			Log.i(TAG, "new drawable");
			this.drawable = mOriginalImageView.getDrawable();
		}
		int width = this.drawable.getIntrinsicWidth();
		int height = this.drawable.getIntrinsicHeight();
		scaledBitmap = Bitmap.createScaledBitmap(
				AndroidUtils.drawableToBitmap(this.drawable), width / 2,
				height / 2, false);
		delayHandler.sendEmptyMessage(5);
	}

}
