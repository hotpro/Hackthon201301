package com.dianping.hackthon;

import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.dianping.hackthon.LockPatternView.Cell;
import com.dianping.hackthon.LockPatternView.DisplayMode;
import com.dianping.hackthon.LockPatternView.OnPatternListener;
import com.dianping.hackthon.net.HttpBase;
import com.dianping.hackthon.net.SuccessMsg;

public class VerifyFragment extends Fragment implements OnClickListener {
	public static final String TAG = VerifyFragment.class.getSimpleName();

	// to override
	public static VerifyFragment newInstance(Activity activity) {
		VerifyFragment f = new VerifyFragment();
		FragmentTransaction transaction = activity.getFragmentManager()
				.beginTransaction();
		transaction.add(android.R.id.content, f, TAG);
		transaction.addToBackStack(null);
		transaction.commit();
		return f;
	}
	
	private Button btnSubmit;

	private Button btn_check_pwd;

	// private OnPatternListener onPatternListener;

	private Button btn_reset_pwd;

	private Button btn_set_pwd;

	private LockPatternUtils lockPatternUtils;

	private LockPatternView lockPatternView;

	private boolean opFLag = true;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		lockPatternUtils = new LockPatternUtils(getActivity());
		lockPatternView.setOnPatternListener(new OnPatternListener() {

			public void onPatternCellAdded(List<Cell> pattern) {

			}

			public void onPatternCleared() {

			}

			public void onPatternDetected(List<Cell> pattern) {
				if (opFLag) {
					int result = lockPatternUtils.checkPattern(pattern);
					switch (result) {
					case 0:
						lockPatternView.setDisplayMode(DisplayMode.Wrong);
						Toast.makeText(getActivity(), "密码错误", Toast.LENGTH_LONG)
								.show();
						break;
					case 1:
						Toast.makeText(getActivity(), "密码正确", Toast.LENGTH_LONG)
								.show();
						break;

					default:
						lockPatternView.clearPattern();
						Toast.makeText(getActivity(), "请设置密码",
								Toast.LENGTH_LONG).show();
						break;
					}

				} else {
					lockPatternUtils.saveLockPattern(pattern);
					Toast.makeText(getActivity(), "密码已经设置", Toast.LENGTH_LONG)
							.show();
					lockPatternView.clearPattern();
				}

			}

			public void onPatternStart() {

			}
		});
	}

	public void onClick(View v) {
		if (v == btn_reset_pwd) {
			lockPatternView.clearPattern();
			lockPatternUtils.clearLock();
			Toast.makeText(getActivity(), "重置密码！", Toast.LENGTH_SHORT).show();
		} else if (v == btn_check_pwd) {
			opFLag = true;
		} else if (v == btn_set_pwd) {
			opFLag = false;
		} else if (v == btnSubmit) {
			new VerifyTask().execute();
			
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
				Toast.makeText(getActivity(), result ==  null ? api.errorMsg() : "校验成功", Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.verify_fragment, container, false);

		lockPatternView = (LockPatternView) v.findViewById(R.id.lpv_lock);
		btnSubmit = (Button)v.findViewById(R.id.btn_submit);
		btn_reset_pwd = (Button) v.findViewById(R.id.btn_reset_pwd);
		btn_set_pwd = (Button) v.findViewById(R.id.btn_set_pwd);
		btn_check_pwd = (Button) v.findViewById(R.id.btn_check_pwd);
		btnSubmit.setOnClickListener(this);
		btn_reset_pwd.setOnClickListener(this);
		btn_set_pwd.setOnClickListener(this);
		btn_check_pwd.setOnClickListener(this);

		return v;
	}

}
