package com.dianping.hackthon;

import java.util.UUID;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.dianping.hackthon.ShowFragment.ImageTask;
import com.dianping.hackthon.net.HttpBase;
import com.dianping.hackthon.net.SuccessMsg;
import com.dianping.taiji.image.ReverseWaveFilter;
import com.dianping.taiji.image.TaijiReverseTransformFilter;
import com.jabistudio.androidjhlabs.filter.util.AndroidUtils;

public class ShowFragment1 extends Fragment implements OnSeekBarChangeListener {
	public static final String TAG = ShowFragment.class.getSimpleName();
	private int[] mColors;
	private SeekBar seekbar;
	private TextView text;
	private ImageView mOriginalImageView;
	private ProgressDialog mProgressDialog;
	private boolean filterInUIThread = true;;
	private String token = UUID.randomUUID().toString();

	// to override
	public static ShowFragment newInstance(Activity activity) {
		ShowFragment f = new ShowFragment();
		FragmentTransaction transaction = activity.getFragmentManager()
				.beginTransaction();
		transaction.add(android.R.id.content, f, TAG);
		transaction.addToBackStack(null);
		transaction.commit();
		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.show_fragment, container, false);
		seekbar = (SeekBar) v.findViewById(R.id.seekbar);
		seekbar.setProgress(seekbar.getMax() / 2);
		seekbar.setOnSeekBarChangeListener(this);
		
		text = (TextView) v.findViewById(R.id.text);
		mOriginalImageView = (ImageView) v.findViewById(R.id.image);

		return v;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (fromUser) {
			text.setText("progress: " + progress);
			this.progress = progress - 50;
			if (filterInUIThread) {
				filterInUIThread();
			}
		}
	}

	private int progress;

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		if (!filterInUIThread) {
			filterInOtherThread();
		}

	}
	TaijiReverseTransformFilter filter = new TaijiReverseTransformFilter();
	Drawable drawable;

	private void filterInUIThread() {
		if (this.drawable == null) {
			Log.i(TAG, "new drawable");
			this.drawable = mOriginalImageView.getDrawable();
		}
		int width = this.drawable.getIntrinsicWidth();
		int height = this.drawable.getIntrinsicHeight();

		if (mColors == null) {
			Log.i(TAG, "new mColors");
			mColors = AndroidUtils.drawableToIntArray(this.drawable);
		}
		
		setModifyView(filter.filter(mColors, width, height, this.progress), getValue(width), getValue(height));
	}

	private void filterInOtherThread() {
		final int width = mOriginalImageView.getDrawable().getIntrinsicWidth();
		final int height = mOriginalImageView.getDrawable()
				.getIntrinsicHeight();

		mColors = AndroidUtils.drawableToIntArray(mOriginalImageView
				.getDrawable());
		mProgressDialog = ProgressDialog.show(getActivity(), "", "Wait......");

		Thread thread = new Thread() {
			public void run() {

				ReverseWaveFilter reverseWaveFilter = new ReverseWaveFilter(
						progress, (float) height / 360);

				mColors = reverseWaveFilter.filter(mColors, width, height);
				getActivity().runOnUiThread(new Runnable() {
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

	private int getValue(int value) {
		if (value <= 0) {
			value = 1;
		}
		return value;
	}

	class ImageTask extends AsyncTask<Void, Void, Bitmap> {
		HttpBase api = new HttpBase();

		@Override
		protected Bitmap doInBackground(Void... params) {
			Bitmap bitmap = null;
			byte[] bytes = api.getRaw(HttpBase.API_DAIMON + "image?token="
					+ 123);
			if (bytes == null) {
				return null;
			}
			bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			return bitmap;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			if (result != null) {
				mOriginalImageView.setImageBitmap(result);
			} else {
				if (getActivity() != null) {
					Toast.makeText(getActivity(), api.errorMsg(),
							Toast.LENGTH_SHORT).show();
				}
			}
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

	}

	class VerifyTask extends AsyncTask<Void, Void, SuccessMsg> {
		HttpBase api = new HttpBase();

		@Override
		protected void onPreExecute() {
			if (getActivity() != null) {
				Toast.makeText(getActivity(), "开始校验", Toast.LENGTH_SHORT);
			}
		}

		@Override
		protected SuccessMsg doInBackground(Void... params) {
			String code = "";
			SuccessMsg msg = api.postRaw(HttpBase.API_DAIMON, "code", code);
			return msg;
		}

		@Override
		protected void onCancelled() {
			api.abort();
		}

		@Override
		protected void onPostExecute(SuccessMsg result) {
			if (getActivity() != null) {
				Toast.makeText(getActivity(),
						result == null ? api.errorMsg() : "校验成功",
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void onDestroy() {
		if (mFilterBitmap != null) {
			mFilterBitmap.recycle();
			mFilterBitmap = null;
		}
		super.onDestroy();
	}

	protected Bitmap mFilterBitmap;

	public void getImage() {
		new ImageTask().execute();
	}

}
