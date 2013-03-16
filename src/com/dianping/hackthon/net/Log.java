package com.dianping.hackthon.net;

public class Log {
	public static void i(String tag, String msg) {
		if (HttpBase.DEBUG) {
			android.util.Log.i(tag, msg);
		}
	}
	
	public static void i(String msg) {
		if (HttpBase.DEBUG) {
			android.util.Log.i("dianping-hackthon", msg);
		}
	}
}
