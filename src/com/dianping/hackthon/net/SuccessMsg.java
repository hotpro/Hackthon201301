package com.dianping.hackthon.net;

import android.os.Parcel;
import android.os.Parcelable;

public class SuccessMsg extends SimpleMsg {

	public SuccessMsg(String title, String content, int icon, int flag) {
		super(title, content, icon, flag);
	}

	protected SuccessMsg() {
	}

	//
	// Parcelable
	//

	public static final Parcelable.Creator<SuccessMsg> CREATOR = new Parcelable.Creator<SuccessMsg>() {
		public SuccessMsg createFromParcel(Parcel in) {
			return new SuccessMsg(in);
		}

		public SuccessMsg[] newArray(int size) {
			return new SuccessMsg[size];
		}
	};

	protected SuccessMsg(Parcel in) {
		super(in);
	}
}
