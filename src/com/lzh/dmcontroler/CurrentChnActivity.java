package com.lzh.dmcontroler;

import java.lang.ref.SoftReference;
import java.text.CollationElementIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.lzh.dmcontroler.E2Service.E2EpgNowNextListHandler;
import com.lzh.dmcontroler.E2Service.E2EventListHandler;
import com.lzh.dmcontroler.E2Service.E2ServiceListHandler;
import com.lzh.dmcontroler.helpers.DeviceDetector;
import com.lzh.dmcontroler.helpers.DreamSaxParser;
import com.lzh.dmcontroler.helpers.Event;
import com.lzh.dmcontroler.helpers.ExtendedHashMap;
import com.lzh.dmcontroler.helpers.Profile;
import com.lzh.dmcontroler.helpers.SimpleHttpClient;
import com.lzh.dmcontroler.helpers.URIStore;
import com.lzh.dmremote.R;

import android.app.Activity;
import android.content.ClipData.Item;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Images;
import android.text.TextPaint;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class CurrentChnActivity extends Activity {
	

	private static int m_nSnapShotRefTime = 10;
	private static int m_nEPGRefTime = 20;
	private String sTag = "CurrentChnActivity";
	private int m_ScreenWidth = 0;
	private int m_ScreenHight = 0;
	
	//String m_IP;
	long m_lLastPressExitBtnTime;
	int	m_nExitTag;
	
	private String m_strGetEpgErrMsg = "";
	private String m_strIP = "";
	private int m_nPort = 0;
	private Context mParaentContext;
	private SimpleHttpClient m_httpClient;
	private byte[] m_bImageData;
	final Handler m_UpdateImageHandler = new Handler();
	final Handler m_UpdateEPGHandler = new Handler();
	
	private ProgressBar m_processBar;
	
	private Intent	m_parentIntent;
	
	private Handler h_SnapShotHandler = new Handler();
	
	private Runnable h_SnapRunnable = new Runnable() {
		@Override
		public void run () {
			refSnapShot();
			
			h_SnapShotHandler.postDelayed(this, 1000 * m_nSnapShotRefTime);
			
		}
	};
	
	private static class cEPGDEF {
		private String strProgName;
		private String strProgInfo;
		private String strExtInfo;
	}
	
	private List<CurrentChnActivity.cEPGDEF> m_listEpgItem = new ArrayList<CurrentChnActivity.cEPGDEF>();
	private CurrentChnActivity.cEPGDEF m_CurrentProgEpg = new CurrentChnActivity.cEPGDEF();
	private ArrayList<ExtendedHashMap> m_CrtEpg;
	
	
	
	/*
	public CurrentChnActivity(Context context) {
		mParaentContext = context;
		
	}
	*/
	public void SetScreen(int nWidth, int nHight) {
		m_ScreenHight = nHight;
		m_ScreenWidth = nWidth;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		//Log.d(sTag, "OnCreate");
		setContentView(R.layout.current_chn);
		//m_parentIntent = getParentActivityIntent();
		
		setTitle(getResources().getString(R.string.crt_chn_title));
		
		Bundle b = getIntent().getExtras();
		if (null != b) {
			m_strIP = b.getString(IntentExtDataDef.IP);
			m_nPort = b.getInt(IntentExtDataDef.Port, 80);
			m_ScreenHight = b.getInt(IntentExtDataDef.ScreenHight, 1280);
			m_ScreenWidth = b.getInt(IntentExtDataDef.ScreenWidth, 720);
			
		}
		
		m_processBar = (ProgressBar) findViewById(R.id.progressbar);
		m_processBar.setVisibility(View.VISIBLE);
		ShowElement(false);
		
		//SetScreen(720, 1280);
		//SetScreen(1920, 1080);
		//SetIPAndPortAndConnect("pr0zel.vicp.net", 39728);
		//SetIPAndPortAndConnect("192.168.1.40", 80);
		//LoadCurrentEvr();
		SetIPAndPortAndConnect(m_strIP, m_nPort);
		refSnapShot();
		refEPG();
		//m_hRefSnapShot.postDelayed(m_runableRefSnapShot, 1000 * m_nSnapShotRefTime);
		m_hEPGRef.postDelayed(m_runableEpgRef, 1000 * m_nEPGRefTime);
		h_SnapShotHandler.postDelayed(h_SnapRunnable, 1000 * m_nSnapShotRefTime);
	}
	
	
	private void LoadCurrentEvr() {
		// load current everlation form shared data area
		SharedPreferences sharedata = getSharedPreferences("data", 0);
		
		m_strIP = sharedata.getString(IntentExtDataDef.IP, "");
		m_ScreenWidth = sharedata.getInt(IntentExtDataDef.ScreenWidth, 720);
		m_ScreenHight = sharedata.getInt(IntentExtDataDef.ScreenHight, 1280);
	}
	
	public void SetIPAndPortAndConnect(String strIP, int nPort) {
		
		if (0 == strIP.length() || 0 == nPort) {
			return;
		}
		
		m_strIP = strIP;
		m_nPort = nPort;
		
		
		Profile p = new Profile("Default", m_strIP, "", m_nPort, 8001, 80,
				false, "", "", false, false, false, false, false);
		m_httpClient = new SimpleHttpClient(p);
		
	}
	
	public void SetIntent(Intent paraentIntent) {
		m_parentIntent = paraentIntent;
	}
	
	private void refSnapShot() {
		String strHight = String.format("%d", m_ScreenWidth);
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("r", strHight));
		params.add(new BasicNameValuePair("format", "jpg"));
		
		(new getSnapShot()).execute(params);
		
	}
	
	
	private void refEPG() {
		String strHight = String.format("%d", m_ScreenWidth);
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("r", strHight));
		params.add(new BasicNameValuePair("format", "jpg"));
		(new getEPG()).execute(params);
		
	}
	// get snapshot and show it 
	private void finishSnapShot() {
		//
		if (null == m_bImageData)
			return;
		if (0 == m_bImageData.length)
			return;
		
		ImageView imgview = (ImageView)findViewById(R.id.imageViewSnapShot);
		Bitmap bmp = BitmapFactory.decodeByteArray(m_bImageData, 0, m_bImageData.length);
		imgview.setImageBitmap(bmp);
		imgview.invalidate();		
	}
	
	private void finishDisplayEPG() {
		//ArrayList<ExtendedHashMap> list
		//textViewCurEPG
		//textViewChnName
		
		//m_CrtEpg
		
		if (0 != m_strGetEpgErrMsg.length()) {
			//TextView tvk = (TextView) findViewById(R.id.textViewCurPlaying);
			TextView tvk = (TextView) findViewById(R.id.textViewChnName);
			tvk.setText(m_strGetEpgErrMsg);
			ShowElement(true);
			m_processBar.setVisibility(View.GONE);
			return;
		} //else {
			//TextView tvk = (TextView) findViewById(R.id.textViewChnName);
			//TextPaint tp = tvk.getPaint();
			//tp.setFakeBoldText(true);
		//}
		
		if (null == m_CrtEpg || 0 == m_CrtEpg.size()) {
			ShowElement(true);
			return;
		}
		ExtendedHashMap	map = m_CrtEpg.get(0);
		if (null == map) {
			ShowElement(true);
			return;
		}
		
		String ssTitle = map.getString(Event.KEY_EVENT_TITLE);
		String ssChnName = map.getString(Event.KEY_SERVICE_NAME);
		String ssPri = map.getString(Event.KEY_SERVICE_PROVIDER_NAME);
		String strEpg = map.getString(Event.KEY_EVENT_DESCRIPTION) + map.getString(Event.KEY_EVENT_DESCRIPTION_EXTENDED);
		String strDescription = map.getString(Event.KEY_EVENT_DESCRIPTION);
		String strDescriptionEx = map.getString(Event.KEY_EVENT_DESCRIPTION_EXTENDED);
		
		String strDispChannelName = "";
		String strDispTitle = "";
		String strDispDescription = "";
		/*
		if ("null".equalsIgnoreCase(map.getString(Event.KEY_EVENT_DESCRIPTION)) && 
				false == "null".equalsIgnoreCase(map.getString(Event.KEY_EVENT_DESCRIPTION_EXTENDED)))
			strEpg = map.getString(Event.KEY_EVENT_DESCRIPTION_EXTENDED);
		else {
			//
			strEpg = map.getString(Event.KEY_EVENT_DESCRIPTION) + map.getString(Event.KEY_EVENT_DESCRIPTION_EXTENDED);
		}
		*/
		strDispChannelName = " " + ssChnName;
		strDispTitle = " " + ssTitle;
		strDispDescription = " ";
		
		if ("tvb".equalsIgnoreCase(ssPri)) {
			// tvb
			if ("nullnull".equals(strEpg)) 
				strDispDescription += getResources().getString(R.string.crt_chn_pag_no_eventdescription);
			else {
				strDispDescription += getResources().getString(R.string.crt_chn_pag_per_eventdescription);
				strDispDescription += strEpg;
			}
		}
		else {
			//
			if ("null".equals(strEpg)) {
				strDispDescription += getResources().getString(R.string.crt_chn_pag_no_eventdescription);
			}
			else {
				strDispDescription += getResources().getString(R.string.crt_chn_pag_per_eventdescription) + " ";
				if (false == "null".equalsIgnoreCase(strDescription)
						&& null != strDescription
						&& 0 != strDescription.length()) {
					strDispDescription += strDescription;
					strDispDescription += "\n ";
				}
				
				strDispDescription += strDescriptionEx;
			}
		}
		
		
		TextView t1 = (TextView) findViewById(R.id.textViewChnName);
		t1.setText(strDispChannelName);
		TextView t2 = (TextView) findViewById(R.id.textViewCurEPG);
		t2.setText(strDispDescription);
		t2.setMovementMethod(ScrollingMovementMethod.getInstance());
		//t2.setVerticalScrollBarEnabled(true);
		TextView t3 = (TextView) findViewById(R.id.textViewCurPlaying);
		t3.setText(strDispTitle);
		
		m_processBar.setVisibility(View.GONE);
		ShowElement(true);
		
	}
	
	private void ShowElement(Boolean bShow) {
		TextView t1 = (TextView) findViewById(R.id.textViewChnName);
		TextView t2 = (TextView) findViewById(R.id.textViewCurPlaying);
		TextView t3 = (TextView) findViewById(R.id.textViewCurEPG);
		
		ImageView imgView = (ImageView) findViewById(R.id.imageViewSnapShot);
		
		if (false == bShow) {
			t1.setAlpha(0);
			t2.setAlpha(0);
			t3.setAlpha(0);
			imgView.setVisibility(View.GONE);
		}
		else {
			t1.setAlpha(1);
			t2.setAlpha(1);
			t3.setAlpha(1);
			imgView.setVisibility(View.VISIBLE);
		}
		
	}
		
	
	private class getEPG extends
	AsyncTask<ArrayList<NameValuePair>, String, Boolean> {
		//protected ArrayList<ExtendedHashMap> mCurEventList;
		private DreamSaxParser m_ChnListSaxParser;
		
		public getEPG() {
			super();
			//E2ServiceListHandler handle = new E2ServiceListHandler();
			E2EventListHandler handle = new E2EventListHandler();
			m_CrtEpg = new ArrayList<ExtendedHashMap>();
			handle.setList(m_CrtEpg);
			m_ChnListSaxParser = new DreamSaxParser(handle);
			
		}

		@Override
		protected Boolean doInBackground(ArrayList<NameValuePair>... params) {
			// TODO Auto-generated method stub
			Boolean bFetch = false;
			String currentProgString = "";
			m_strGetEpgErrMsg = "";
			//String strErrMsg = getResources().getString(R.string.crt_chn_pag_connect_error);
			//TextView tv1 = (TextView) findViewById(R.id.textViewCurPlaying);
			bFetch = m_httpClient.fetchPageContent(URIStore.CURRENT);
			//m_UpdateEPGHandler.post(mUpdateEPG);
			if (false == bFetch) {
				//tv1.setText(strErrMsg);
				//ShowElement(true);
				//Toast.makeText(getApplicationContext(), strErrMsg, 3000).show();
				m_strGetEpgErrMsg = getResources().getString(R.string.crt_chn_pag_connect_error);
				m_UpdateEPGHandler.post(mUpdateEPG);
				return false;
			}
			currentProgString = m_httpClient.getPageContentString();
			if (true == m_ChnListSaxParser.parse(currentProgString))
				m_UpdateEPGHandler.post(mUpdateEPG);
			else {
				//tv1.setText(strErrMsg);
				//ShowElement(true);
				//Toast.makeText(getApplicationContext(), strErrMsg, 3000).show();
				m_strGetEpgErrMsg = getResources().getString(R.string.crt_chn_pag_connect_error);
				m_UpdateEPGHandler.post(mUpdateEPG);
				return false;
			}
			
			return true;
		}
	
	}
	
	final Runnable mUpdateImageRunnable = new Runnable() {
		//
		 public void run() {
			 finishSnapShot();
		 }
	};
	
	final Runnable mUpdateEPG = new Runnable() {
		public void run() {
			finishDisplayEPG();
		}
	};

	private class getSnapShot extends
	AsyncTask<ArrayList<NameValuePair>, String, Boolean> {

		@Override
		protected Boolean doInBackground(ArrayList<NameValuePair>... params) {
			// TODO Auto-generated method stub
			byte[] bImageData;
			Boolean bFetch = false;
			bFetch = m_httpClient.fetchPageContent(URIStore.SCREENSHOT, params);
			
			if (true == bFetch) {
				bImageData = m_httpClient.getBytes();
				
				if (0 != bImageData.length) {
					m_bImageData = new byte[bImageData.length];
					System.arraycopy(bImageData, 0, m_bImageData, 0, bImageData.length);
				}
			}
			else {
				m_bImageData = null;
			}
			
			m_UpdateImageHandler.post(mUpdateImageRunnable);
			
			return null;
		}
	
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		h_SnapShotHandler.removeCallbacks(h_SnapRunnable);
		m_hEPGRef.removeCallbacks(m_runableEpgRef);
		super.onDestroy();
	}
	
	Handler m_hEPGRef = new Handler();
	Runnable m_runableEpgRef = new Runnable(){
		@Override
		public void run() {
			refEPG();
			m_hEPGRef.postDelayed(this, 1000 * m_nEPGRefTime);
		}
	};



	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		//m_hRefSnapShot.postDelayed(m_runableRefSnapShot, 100);
		//Log.d("CurrentChnAct", "onNewIntent");
		super.onNewIntent(intent);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		//Log.d("CurrentChnActi", "OnResume");
		super.onResume();
	}

}