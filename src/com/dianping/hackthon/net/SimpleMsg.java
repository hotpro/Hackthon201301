package com.dianping.hackthon.net;

import android.os.Parcel;
import android.os.Parcelable;

public class SimpleMsg implements Parcelable {
	protected String title;
	protected String content;
	protected int icon;
	protected int flag;

	public SimpleMsg(String title, String content, int icon, int flag) {
		this.title = title;
		this.content = content;
		this.icon = icon;
		this.flag = flag;
	}

	public String title() {
		return title;
	}

	public String content() {
		return content;
	}

	public int icon() {
		return icon;
	}

	public int flag() {
		return flag;
	}

	@Override
	public String toString() {
		return title + " : " + content;
	}

	protected SimpleMsg() {
	}

	//
	// Parcelable
	//

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(title);
		out.writeString(content);
		out.writeInt(icon);
		out.writeInt(flag);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<SimpleMsg> CREATOR = new Parcelable.Creator<SimpleMsg>() {
		public SimpleMsg createFromParcel(Parcel in) {
			return new SimpleMsg(in);
		}

		public SimpleMsg[] newArray(int size) {
			return new SimpleMsg[size];
		}
	};

	protected SimpleMsg(Parcel in) {
		title = in.readString();
		content = in.readString();
		icon = in.readInt();
		flag = in.readInt();
	}
}
