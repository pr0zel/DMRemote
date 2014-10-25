package com.lzh.dmcontroler;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.widget.DrawerLayout.LayoutParams;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.flurry.android.FlurryAgent;
import com.lzh.dmremote.R;

public class ChannelListAct extends Activity {
	
	String m_IP = "";
	int m_nPort = 80;
	long m_lLastPressExitBtnTime;
	int m_nScreenHight = 0;
	int m_nScreenWidth = 0;
	
	PopupWindow m_popUpWindow;
	private static int m_nChnListRefTime = 120;
	
	private class IPAndPort {
		String strIPAddr;
		int nPort;
	}
	
	ChannelList m_chnList;
	//ChannelList2 m_chnList;
	ListView m_ChnListView;
	
	//int	m_nExitTag;
	
	//private ViewFlipper flipper;
	//private GestureDetector detector;
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		setContentView(R.layout.chnlist_list);
		
		
		Bundle bund = getIntent().getExtras();
		if (null != bund) {
			m_IP = bund.getString(IntentExtDataDef.IP);
			m_nPort = bund.getInt(IntentExtDataDef.Port, 80);
			m_nScreenHight = bund.getInt(IntentExtDataDef.ScreenHight, 1280);
			m_nScreenWidth = bund.getInt(IntentExtDataDef.ScreenWidth, 720);
		}
		
		m_chnList = new ChannelList(getBaseContext());
		//m_chnList = new ChannelList2(getBaseContext());
		m_ChnListView = (ListView) findViewById(R.id.listViewChanList);
		
		setTitle(getResources().getString(R.string.chn_disp_title));
		
		m_hRefHandler.postDelayed(m_runableRef, 1000 * m_nChnListRefTime);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		m_hRefHandler.removeCallbacks(m_runableRef);
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		
		InitInterface();
		super.onPostCreate(savedInstanceState);
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	private Handler m_hPopupHandler = new Handler(){ 
		@Override
		public void handleMessage(Message msg) { 
			switch (msg.what) {
			case 0:
				m_popUpWindow.showAtLocation(findViewById(R.id.ChnBtnListMain),
						Gravity.CENTER, 0, 0);
				m_popUpWindow.update();
				break;
				
			}
		}
	};
	
	
	private void InitInterface () {
		
		if (1280 >= m_nScreenHight) {
			m_ChnListView.setDividerHeight(8);
		}
		else {
			m_ChnListView.setDividerHeight(10);
		}
		
		m_ChnListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				
				if (null != m_chnList) {
					m_chnList.ChangeChannel(arg2, true);
					
					Vibrator mVibrator;
					mVibrator = (Vibrator)getSystemService(Service.VIBRATOR_SERVICE);
					mVibrator.vibrate(50);
				}
			}
			
		});
		
		m_ChnListView.setBackgroundColor(Color.parseColor("#3c4f5f"));
		Connect();
		
	}
	
	private void Connect() {
		//
		m_chnList.setIPAndConnect(m_IP, m_nPort);
		m_ChnListView.setAdapter(m_chnList);
		m_ChnListView.refreshDrawableState();
		
	}
	
	public void ConnectByNewIP(String strIP, int nPort) {
		m_IP = strIP;
		m_nPort = nPort;
		
		m_chnList.setIPAndConnect(m_IP, m_nPort);
		m_ChnListView.refreshDrawableState();
	}
	
	private Handler m_hRefHandler = new Handler();
	Runnable m_runableRef = new Runnable() {
		@Override
		public void run() { 
			m_chnList.refChnList();
			
			//2分钟刷新一次
			m_hRefHandler.postDelayed(this, 1000* m_nChnListRefTime);
			
		}
	};

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}
}
