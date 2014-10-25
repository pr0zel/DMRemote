/* © 2010 Stephan Reichholf <stephan at reichholf dot net>
 * 
 * Licensed under the Create-Commons Attribution-Noncommercial-Share Alike 3.0 Unported
 * http://creativecommons.org/licenses/by-nc-sa/3.0/
 */

package com.lzh.dmcontroler.helpers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.ByteArrayBuffer;

import com.lzh.dmcontroler.DreamDroid;

import android.util.Log;

/**
 * @author sreichholf
 * 
 */
public class SimpleHttpClient {
	public static String LOG_TAG = "SimpleHttpClient";

	private Profile mProfile;

	private String mPrefix;
	private String mFilePrefix;
	private String mHostname;
	private String mStreamHostname;
	private String mPort;
	private String mStreamPort;
	private String mFilePort;
	private String mUser;
	private String mPass;
	private byte[] mBytes;
	private String mErrorText;

	private boolean mLogin;
	private boolean mSsl;
	private boolean mStreamLogin;
	private boolean mFileLogin;
	private boolean mFileSsl;
	private boolean mError;
	private boolean mIsLoopProtected;

	/**
	 * @param sp
	 *            SharedPreferences of the Base-Context
	 */
	public SimpleHttpClient() {
		mProfile = null;
		init();
	}

	public SimpleHttpClient(Profile p) {
		mProfile = p;
		init();
	}

	private void init() {
		try {
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, new TrustManager[] { new EasyX509TrustManager(null) }, null);
			HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
		} catch (KeyManagementException e) {
		} catch (NoSuchAlgorithmException e) {
		} catch (KeyStoreException e) {
		}

		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		});

		applyConfig();
	}

	/**
	 * @param user
	 *            Username for http-auth
	 * @param pass
	 *            Password for http-auth
	 */
	public void setCredentials(final String user, final String pass) {
		Authenticator.setDefault(new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(user, pass.toCharArray());
			}
		});
	}

	/**
	 * 
	 */
	public void unsetCrendentials() {
		// mDhc.getCredentialsProvider().setCredentials(AuthScope.ANY, null);
	}

	/**
	 * @param uri
	 * @param parameters
	 * @return
	 */
	public String buildUrl(String uri, List<NameValuePair> parameters) {
		String parms = URLEncodedUtils.format(parameters, HTTP.UTF_8).replace("+", "%20");
		return mPrefix + mHostname + ":" + mPort + uri + parms;
	}

	public String buildUrl(String url, List<NameValuePair>[] parameters) {
		int i=0;
		String parms = "";
		for (i=0; i<parameters.length; i++) {
			parms += URLEncodedUtils.format(parameters[i], HTTP.UTF_8).replace("+", "%20");
		}
		
		return mPrefix + mHostname + ":" + mPort + url + parms;
		
	}
	/**
	 * @param ref
	 * @param title
	 * @return
	 */
	public String buildServiceStreamUrl(String ref, String title) {
		try {
			ref = URLEncoder.encode(ref, HTTP.UTF_8).replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
		}
		String streamLoginString = "";
		if (mStreamLogin)
			streamLoginString = mUser + ":" + mPass + "@";

		String url = "http://" + streamLoginString + mStreamHostname + ":" + mStreamPort + "/" + ref;
		return url;
	}

	/**
	 * @param uri
	 * @param parameters
	 * @return
	 */
	public String buildFileStreamUrl(String uri, List<NameValuePair> parameters) {
		String parms = URLEncodedUtils.format(parameters, HTTP.UTF_8).replace("+", "%20");
		String fileAuthString = "";
		if (mFileLogin)
			fileAuthString = mUser + ":" + mPass + "@";

		String url = mFilePrefix + fileAuthString + mStreamHostname + ":" + mFilePort + uri + parms;
		return url;
	}

	public boolean fetchPageContent(String uri) {
		return fetchPageContent(uri, new ArrayList<NameValuePair>());
	}

	/**
	 * @param uri
	 * @param parameters
	 * @return
	 */
	public boolean fetchPageContent(String uri, List<NameValuePair> parameters) {
		// Set login, ssl, port, host etc;
		applyConfig();

		mErrorText = "";
		mError = false;
		mBytes = new byte[0];
		if (!uri.startsWith("/")) {
			uri = "/".concat(uri);
		}

		HttpURLConnection conn = null;
		try {
			URL url = new URL(buildUrl(uri, parameters));
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(10000);
			if (DreamDroid.featurePostRequest())
				conn.setRequestMethod("POST");

			if (conn.getResponseCode() != 200) {
				if (conn.getResponseCode() == 405 && !mIsLoopProtected) {
					// Method not allowed, the target device either can't handle
					// POST or GET requests (old device or Anti-Hijack enabled)
					DreamDroid.setFeaturePostRequest(!DreamDroid.featurePostRequest());
					conn.disconnect();
					mIsLoopProtected = true;
					return fetchPageContent(uri, parameters);
				}
				mIsLoopProtected = false;
				mErrorText = conn.getResponseMessage();
				mError = true;
				return false;
			}

			BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			int read = 0;
			int bufSize = 512;
			byte[] buffer = new byte[bufSize];
			while ((read = bis.read(buffer)) != -1) {
				baf.append(buffer, 0, read);
			}

			mBytes = baf.toByteArray();
			return true;

		} catch (MalformedURLException e) {
			mError = true;
			mErrorText = e.toString();
		} catch (IOException e) {
			mError = true;
			mErrorText = e.toString();
		} catch (Exception e) {
			mError = true;
			mErrorText = e.toString();
		} finally {
			if (conn != null)
				conn.disconnect();
			if (mError)
				if (mErrorText == null)
					mErrorText = "Error text is null";
			Log.e(LOG_TAG, mErrorText);
		}

		return false;
	}

	public boolean fetchPageContent(String uri, List<NameValuePair>[] parameters) {
		// Set login, ssl, port, host etc;
		applyConfig();

		mErrorText = "";
		mError = false;
		mBytes = new byte[0];
		if (!uri.startsWith("/")) {
			uri = "/".concat(uri);
		}

		HttpURLConnection conn = null;
		try {
			URL url = new URL(buildUrl(uri, parameters));
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(10000);
			if (DreamDroid.featurePostRequest())
				conn.setRequestMethod("POST");

			if (conn.getResponseCode() != 200) {
				if (conn.getResponseCode() == 405 && !mIsLoopProtected) {
					// Method not allowed, the target device either can't handle
					// POST or GET requests (old device or Anti-Hijack enabled)
					DreamDroid.setFeaturePostRequest(!DreamDroid.featurePostRequest());
					conn.disconnect();
					mIsLoopProtected = true;
					return fetchPageContent(uri, parameters);
				}
				mIsLoopProtected = false;
				mErrorText = conn.getResponseMessage();
				mError = true;
				return false;
			}

			BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			int read = 0;
			int bufSize = 512;
			byte[] buffer = new byte[bufSize];
			while ((read = bis.read(buffer)) != -1) {
				baf.append(buffer, 0, read);
			}

			mBytes = baf.toByteArray();
			return true;

		} catch (MalformedURLException e) {
			mError = true;
			mErrorText = e.toString();
		} catch (IOException e) {
			mError = true;
			mErrorText = e.toString();
		} catch (Exception e) {
			mError = true;
			mErrorText = e.toString();
		} finally {
			if (conn != null)
				conn.disconnect();
			if (mError)
				if (mErrorText == null)
					mErrorText = "Error text is null";
			Log.e(LOG_TAG, mErrorText);
		}

		return false;
	}

	/**
	 * @return
	 */
	public String getPageContentString() {
		if ((null == mBytes) || (0 == mBytes.length))
			return "";
		else
			return new String(mBytes);
	}

	public byte[] getBytes() {
		return mBytes;
	}

	/**
	 * @return
	 */
	public String getErrorText() {
		return mErrorText;
	}

	/**
	 * @return
	 */
	public boolean hasError() {
		return mError;
	}

	/**
	 * 
	 */
	public void applyConfig() {
		Profile p = mProfile;
		if (p == null)
			p = DreamDroid.getCurrentProfile();

		mHostname = p.getHost().trim();
		mStreamHostname = p.getStreamHost().trim();
		mPort = p.getPortString();
		mStreamPort = p.getStreamPortString();
		mFilePort = p.getFilePortString();
		mLogin = p.isLogin();
		mSsl = p.isSsl();
		mStreamLogin = p.isStreamLogin();
		mFileLogin = p.isFileLogin();
		mFileSsl = p.isFileSsl();

		if (mSsl) {
			mPrefix = "https://";
		} else {
			mPrefix = "http://";
		}

		if (mLogin) {
			mUser = p.getUser();
			mPass = p.getPass();
			setCredentials(mUser, mPass);
		}

		if (mFileSsl) {
			mFilePrefix = "https://";
		} else {
			mFilePrefix = "http://";
		}
	}

	/**
	 * @param sp
	 * @return
	 */
	public static SimpleHttpClient getInstance() {
		return new SimpleHttpClient();
	}

	public static SimpleHttpClient getInstance(Profile p) {
		return new SimpleHttpClient(p);
	}
}
