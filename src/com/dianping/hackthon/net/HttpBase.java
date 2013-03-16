package com.dianping.hackthon.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import com.dianping.hackthon.net.HttpClientFactory.MyHttpClient;

public class HttpBase {
	private static final String TAG = HttpBase.class.getSimpleName();
	//
	// CHANGE THIS WHEN RELEASE
	//
	public static boolean DEBUG = true;
	public static String API_DAIMON1 = "http://192.168.62.112:8090/";
	public static String API_DAIMON = "http://192.168.32.58:8080/";

	public static final int STATUS_CODE_UNKNOWN = 0;
	public static final int STATUS_CODE_TIMEOUT = 5;
	public static final int STATUS_CODE_MALFORMED = 11;
	public static final int STATUS_CODE_CACHED = 209;
	static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss", Locale.US);
	static final SimpleMsg DEFAULT_MESSAGE = new SimpleMsg("错误", "发生未知错误", 0, 0);

	static Context applicationContext;
	static ConnectivityManager connectivityManager;

	static boolean isCacheInited;
	
	public static String WIFI_HTTP_PROXY = null;
	public static int WIFI_HTTP_PROXY_PORT = 0;

	public static void init(Context context, int cityId) {
		if (isCacheInited)
			return;

		isCacheInited = true;
		applicationContext = context.getApplicationContext();
		connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	protected HttpRequest request;
	protected int statusCode;
	protected String statusLine;
	protected SimpleMsg message;

	/**
	 * return the running request, or null if not running
	 */
	public HttpRequest request() {
		return request;
	}

	public void abort() {
		HttpRequest request = this.request;
		if (request instanceof HttpRequestBase)
			((HttpRequestBase) request).abort();
		this.request = null;
	}

	public int statusCode() {
		return statusCode;
	}

	public SimpleMsg message() {
		return message == null ? DEFAULT_MESSAGE : message;
	}

	public String errorMsg() {
		if (message != null)
			return statusCode()
					+ ": "
					+ (message != null ? message.content() : DEFAULT_MESSAGE
							.content());
		else
			return DEFAULT_MESSAGE.content();
	}

	public static HttpHost globalProxy() {
		if (connectivityManager == null)
			return null;
		NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
		if (activeNetInfo == null)
			return null;
		if (activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
			if (WIFI_HTTP_PROXY == null)
				return null;
			return new HttpHost(WIFI_HTTP_PROXY, WIFI_HTTP_PROXY_PORT);
		}
		if (activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
			String extraInfo = activeNetInfo.getExtraInfo();
			if (extraInfo == null)
				return null;
			extraInfo = extraInfo.toLowerCase();
			if (extraInfo.contains("cmnet"))
				return null;
			if (extraInfo.contains("cmwap"))
				return new HttpHost("10.0.0.172");
			if (extraInfo.contains("3gnet"))
				return null;
			if (extraInfo.contains("3gwap"))
				return new HttpHost("10.0.0.172");
			if (extraInfo.contains("uninet"))
				return null;
			if (extraInfo.contains("uniwap"))
				return new HttpHost("10.0.0.172");
			if (extraInfo.contains("ctnet"))
				return null;
			if (extraInfo.contains("ctwap"))
				return new HttpHost("10.0.0.200");
			if (extraInfo.contains("#777")) {
				try {
					Cursor c = applicationContext
							.getContentResolver()
							.query(Uri
									.parse("content://telephony/carriers/preferapn"),
									new String[] { "proxy", "port" }, null,
									null, null);
					if (c.moveToFirst()) {
						String host = c.getString(0);
						if (host.length() > 3) {
							int port = 0;
							try {
								port = Integer.parseInt(c.getString(1));
							} catch (NumberFormatException e) {
							}
							return new HttpHost(host, port > 0 ? port : 80);
						}
					}
					return null;
				} catch (Exception e) {
					return null;
				}
			}
		}
		return null;
	}

	protected HttpHost getProxy() {
		return globalProxy();
	}

	protected byte[] exec(HttpUriRequest request) {

		MyHttpClient httpClient = HttpClientFactory.getHttpClient();
		byte[] buffer = null;
		this.request = request;
		try {
			HttpHost proxy = getProxy();
			ConnRouteParams.setDefaultProxy(request.getParams(), proxy);
			HttpResponse response = httpClient.execute(request);
			statusCode = response.getStatusLine().getStatusCode();
			statusLine = response.getStatusLine().getReasonPhrase();

			HttpEntity entity = response.getEntity();
			buffer = EntityUtils.toByteArray(entity);
			entity.consumeContent();
		} catch (Exception e) {
			if (e instanceof SocketException
					|| e instanceof SocketTimeoutException
					|| e instanceof UnknownHostException) {
				statusCode = STATUS_CODE_TIMEOUT;

				message = new SimpleMsg("网络连接", e.getLocalizedMessage(), 0, 0);
			} else {
				statusCode = STATUS_CODE_UNKNOWN;
				if (statusCode / 100 == 2 || statusLine == null
						|| statusLine.length() == 0) {
					message = new SimpleMsg("错误", e.getLocalizedMessage(), 0, 0);
				} else {
					message = new SimpleMsg("错误", statusLine, 0, 0);
				}
			}
			return null;
		} finally {
			httpClient.recycle();
			this.request = null;
		}
		return buffer;
	}

	protected String getIp(String url) {
		if (url == null)
			return null;
		if (url.startsWith("http://")) {
			String domain = url.substring(7);
			int i = domain.indexOf('/');
			if (i > 0)
				domain = domain.substring(0, i);
			try {
				byte[] ip = Inet4Address.getByName(domain).getAddress();
				String sip = (0xFF & ip[0]) + "." + (0xFF & ip[1]) + "."
						+ (0xFF & ip[2]) + "." + (0xFF & ip[3]);
				return sip;
			} catch (Exception e) {
			}
		}
		return null;
	}

	protected static String getNetwork() {
		if (connectivityManager == null)
			return "unknown";
		NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
		if (activeNetInfo == null)
			return "unknown";
		switch (activeNetInfo.getType()) {
		case ConnectivityManager.TYPE_WIFI:
			return "wifi";
		case ConnectivityManager.TYPE_MOBILE:
			return "mobile(" + activeNetInfo.getSubtypeName() + ","
					+ activeNetInfo.getExtraInfo() + ")";
		default:
			return activeNetInfo.getTypeName();
		}
	}

	/**
	 * the returned data is decrypted.
	 */
	public byte[] getRaw(String url) {
		Log.i(TAG, "GET: " + url);

		long startMs = SystemClock.elapsedRealtime();
		HttpGet get = new HttpGet(url);
		byte[] bytes = exec(get);
		if (bytes == null) {
			return null;
		}

		long endMs = SystemClock.elapsedRealtime();

		Log.i(TAG, "ELAPSE: " + (endMs - startMs) + "ms" + " | " + bytes.length
				+ "bytes");

		return bytes;
	}

	/**
	 */
	public byte[] postRaw(String url, byte[] bytes) {
		byte[] result = postStream(url, new ByteArrayPostStream(bytes));

		return result;
	}

	public SuccessMsg postRaw(String url, String... pairs) {
		byte[] result = postRaw(url, getPostData(pairs));
		SuccessMsg msg = new SuccessMsg();
		return msg;
	}

	protected byte[] getPostData(String... pairs) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0, n = pairs.length / 2; i < n; i++) {
			String name = pairs[i * 2];
			String value = pairs[i * 2 + 1];
			if (i > 0)
				sb.append('&');
			sb.append(name).append('=');
			if (value != null)
				sb.append(URLEncoder.encode(value));
		}
		byte[] bytes;
		try {
			bytes = sb.toString().getBytes("utf-8");
		} catch (Exception e) {

			e.printStackTrace(System.err);
			return null;
		}
		return bytes;
	}

	/**
	 * the post data is not encrypted, and the returned data is decrypted.
	 */
	public byte[] postStream(String url, InputStream stream) {
		Log.i(TAG, "POST: " + url);

		long startMs = SystemClock.elapsedRealtime();
		HttpPost post = new HttpPost(url);
		long length = -1;
		try {
			length = stream.available();
		} catch (IOException e) {
		}
		if (stream instanceof ByteArrayPostStream) {
			post.setEntity(new ByteArrayEntity(
					((ByteArrayPostStream) stream).buffer));
		} else {
			post.setEntity(new InputStreamEntity(stream, length));
		}
		byte[] rbytes = exec(post);
		long endMs = SystemClock.elapsedRealtime();
		if (rbytes == null) {
			return null;
		}

		Log.i(TAG, "ELAPSE: " + (endMs - startMs) + "ms" + " | "
				+ rbytes.length + "bytes");

		return rbytes;
	}

	/**
	 * use this stream if the post body is a fixed byte array
	 */
	protected static class ByteArrayPostStream extends ByteArrayInputStream {
		public final byte[] buffer;

		public ByteArrayPostStream(byte[] buf) {
			super(buf);
			this.buffer = buf;
		}
	}

	public boolean malformedContent(String url) {
		statusCode = STATUS_CODE_MALFORMED;
		return true;
	}

	public static String escapeSource(String src) {
		StringBuilder sb = new StringBuilder();
		for (char c : src.toCharArray()) {
			if (c >= 'a' && c <= 'z') {
				sb.append(c);
			} else if (c >= 'A' && c <= 'Z') {
				sb.append(c);
			} else if (c >= '0' && c <= '9') {
				sb.append(c);
			} else if (c == '.' || c == '_' || c == '-' || c == '/') {
				sb.append(c);
			} else if (c == ' ') {
				sb.append('_');
			}
		}
		return sb.toString();
	}
}