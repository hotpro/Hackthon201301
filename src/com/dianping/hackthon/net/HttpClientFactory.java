package com.dianping.hackthon.net;

import java.io.IOException;
import java.security.KeyStore;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;

import android.os.Build;

public class HttpClientFactory {
	public static final int MAX_POOL_CAPACITY = 4;

	public static String USER_AGENT = "MApi 1.0 (com.dianping.hackthon 1.0; Android "
			+ Build.VERSION.RELEASE + ")";
	public static int NETWORK_CONNECTION_TIMEOUT = 15000;
	public static int NETWORK_SO_TIMEOUT = 15000;

	private static ConcurrentLinkedQueue<MyHttpClient> clients = new ConcurrentLinkedQueue<MyHttpClient>();

	public static MyHttpClient getHttpClient() {
		MyHttpClient hc = clients.poll();
		if (hc == null) {
			HttpParams params = getDefaultHttpParams();
			SchemeRegistry schreg = getDefaultSchemeRegistry();
			hc = new MyDefaultHttpClient(new SingleClientConnManager(params,
					schreg), params);
		}
		return hc;
	}

	public static interface MyHttpClient extends HttpClient {
		void recycle();
	}

	protected static HttpParams getDefaultHttpParams() {
		BasicHttpParams params = new BasicHttpParams();

		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setUserAgent(params, USER_AGENT);
		HttpConnectionParams.setConnectionTimeout(params,
			NETWORK_CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, NETWORK_SO_TIMEOUT);
		return params;
	}

	protected static SchemeRegistry getDefaultSchemeRegistry() {
		SchemeRegistry schemeRegistry = new SchemeRegistry();

		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			schemeRegistry.register(new Scheme("https", sf, 443));
		} catch (Exception e) {
			schemeRegistry.register(new Scheme("https",
					SSLSocketFactory.getSocketFactory(), 443));
		}

		schemeRegistry.register(new Scheme("http",
				PlainSocketFactory.getSocketFactory(), 80));

		return schemeRegistry;
	}

	static class MyDefaultHttpClient extends DefaultHttpClient implements
			MyHttpClient {

		public MyDefaultHttpClient(ClientConnectionManager conman,
				HttpParams params) {
			// super();
			super(conman, params);
			init();
		}

		protected void init() {
			addRequestInterceptor(new HttpRequestInterceptor() {
				public void process(final HttpRequest request,
						final HttpContext context) throws HttpException,
						IOException {
					request.addHeader("Accept-Language", "zh-CN,zh");
					if (!request.containsHeader("pragma-os")) {
						request.addHeader("pragma-os", USER_AGENT);
					}
				}
			});
		}

		@Override
		public void recycle() {
			if (clients.size() < MAX_POOL_CAPACITY) {
				clients.add(this);
			}
		}
	}

}