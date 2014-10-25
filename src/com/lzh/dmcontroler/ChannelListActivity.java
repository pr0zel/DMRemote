package com.lzh.dmcontroler;

import java.util.ArrayList;
import java.util.List;
import com.lzh.dmcontroler.helpers.DateTime;
import com.lzh.dmcontroler.helpers.SimpleHttpClient;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.R.bool;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.support.v4.widget.DrawerLayout.LayoutParams;
import android.text.format.Time;
import android.util.Log;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import com.lzh.dmremote.R;


//尝试用下TabHost

public class ChannelListActivity extends Activity implements OnGestureListener{

	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		//Log.d(ChannelListActivity.class.getName(), "OnIntent");
		//Log.d("OnIntent", intent.toString());
		super.onNewIntent(intent);
	}


	chngrid m_chngridChngrid;
	String m_IP = "";
	int m_nPort = 80;
	long m_lLastPressExitBtnTime;
	int m_nScreenHight = 0;
	int m_nScreenWidth = 0;
	
	int	m_nExitTag;
	
	private ViewFlipper flipper;
	private GestureDetector detector;
	private GridView m_ChnListGridView;
	
	Intent m_CurChanActivInt;
	Boolean m_bWifiOn;
	
	PopupWindow m_popUpWindow;
	
	GridView m_gridView;
	chngrid	m_ChnGrid;
	
	private class IPAndPort {
		String strIPAddr;
		int nPort;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		if (false == isConnectedWifi()) {
			m_bWifiOn = false;
		}
		else {
			m_bWifiOn = true;
		}
		
		//SetFirstIP();
		m_nExitTag = 0;
		
		m_CurChanActivInt = null;
		super.onCreate(savedInstanceState);
		//getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		
		
		//getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.defenedicon);
		setTitle(getResources().getString(R.string.chn_disp_title));
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		//requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_channel_list);
		//getWindow().requestFeature(Window.FEATURE_CUSTOM_TITLE);
		
		//Window mWindow = getWindow();
		//mWindow.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.chnlist_titlebar);
		
		detector = new GestureDetector(this);
		flipper = (ViewFlipper) findViewById(R.id.viewFlipper1);
		
		
		m_gridView = (GridView) findViewById(R.id.gridView1);
		m_ChnListGridView = m_gridView;
		//StaggeredGridView view = (StaggeredGridView) findViewById(R.id.gridView1);
		m_chngridChngrid = new chngrid(getBaseContext());
		m_gridView.setBackgroundColor(Color.BLACK);
		
		
		
		
		/*
		view.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				return false;
			}
			
		});
		*/
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.channel_list, menu);
		//final ListView aa = (ListView)findViewById(R.id.listView_CHNLIST);
		
		MenuItem menu1 = menu.add(Menu.NONE, 0, 0, "");
		menu.add(0, 1, 0, getResources().getString(R.string.opt_menu_setip));
		menu.add(0, 2, 0, getResources().getString(R.string.opt_munu_exit));
		menu1.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);	
		
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		
		switch(item.getItemId()) {
			case 1:
				// press setip
				EnterAnNewIP();
				break;
			case 2:
				//press exit
				this.finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void InitInteface() {
		//
		//m_IP = "192.168.1.40";
		//m_nPort = 80;
		final Resources res = getResources();
		final int w = res.getDisplayMetrics().widthPixels;
		final int w2 = res.getDisplayMetrics().heightPixels;
		//view.setNumColumns(2);
		
		SharedPreferences.Editor shareWriteData = getSharedPreferences("data", 0).edit();
		shareWriteData.putInt("ScreenWidth", w);
		shareWriteData.putInt("ScreenHigth", w2);
		m_nScreenHight = w2;
		m_nScreenWidth = w;
		
		SetFirstIP();
		if (false == m_bWifiOn) {
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.connect_wifi_toast)
					, 5000).show();
			return;
		}
		
		
		m_gridView.setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				Vibrator mVibrator01;
				mVibrator01 = (Vibrator)getSystemService(Service.VIBRATOR_SERVICE);
				mVibrator01.vibrate(50);
				m_chngridChngrid.ChangeChannel(arg2, false);
			}
		});
		
		
		m_chngridChngrid.SetScreen(m_nScreenWidth, m_nScreenHight);
		Connect();
	}
	
	private void Connect() {
		
		m_chngridChngrid.SetIPAndPortAndConnect(m_IP, m_nPort);
		m_gridView.setAdapter(m_chngridChngrid);
		m_gridView.refreshDrawableState();
	}
	
	
	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (e1.getX() - e2.getX() > 100) {
			//Log.d(sTag, "Move Left");
			//setContentView(R.layout.activity_channel_list);
			//Intent intCHNLIST = new Intent(this, ChannelListActivity.class);
			//m_CurChanActivInt = 
			//startActivity(intCHNLIST);
			//overridePendingTransition(R.anim.push_left_in, R.anim.left_out);
			//return false;
			if (false == m_bWifiOn) {
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.connect_wifi_toast)
						, 2000).show();
				return false;
			}
			
			if (null == m_CurChanActivInt) {
				//Intent intCURCHN = new Intent(this, CurrentChnActivity.class);
				m_CurChanActivInt = new Intent();
				m_CurChanActivInt.setClass(this, CurrentChnActivity.class);
				m_CurChanActivInt.putExtra("IP", m_IP);
				m_CurChanActivInt.putExtra("Port", m_nPort);
				m_CurChanActivInt.putExtra("ScreenHight", m_nScreenHight);
				m_CurChanActivInt.putExtra("ScreenWidth", m_nScreenWidth);

				startActivity(m_CurChanActivInt);
				//m_CurChanActivInt.getClass().
				//overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
				overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
				this.finish();
			}
			else {
				startActivity(m_CurChanActivInt);
				//overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
				overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
				this.finish();
			}
		}
		else if(e1.getX() - e2.getX() < -100) {
			//Intent intCHNLIST = new Intent(this, ChannelListActivity.class);
			//startActivity(intCHNLIST);
			//overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
			return false;
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		SharedPreferences.Editor sharedata = getSharedPreferences("data", 0).edit();
		sharedata.putString("IP", m_IP);
		sharedata.putInt("Port", m_nPort);
		sharedata.commit();
		super.onStop();
	}
	
	
	private void EnterAnNewIP() {
		Context ctx = this;
		LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View vPopupWindow = inflater.inflate(R.layout.input_ip, null, false);
		m_popUpWindow = new PopupWindow(vPopupWindow, 300, 300, true); 
		m_popUpWindow.setWidth(LayoutParams.WRAP_CONTENT);
		m_popUpWindow.setHeight(LayoutParams.WRAP_CONTENT);
		
		EditText et = (EditText)vPopupWindow.findViewById(R.id.inputIPTextView);
		if (80 == m_nPort)
			et.setText(m_IP);
		else {
			String ssSet = m_IP;
			ssSet += ":";
			ssSet += m_nPort;
			et.setText(ssSet);
		}
		
		Button btnOK = (Button)vPopupWindow.findViewById(R.id.inputIP_BtnOK);
		btnOK.setOnClickListener(new OnClickListener(){ 
			//
			@Override
			public void onClick(View v) {
				//
				EditText etIPEditText = (EditText)vPopupWindow.findViewById(R.id.inputIPTextView);
				//Toast.makeText(getApplicationContext(), etIPEditText.getText().toString(), 5000).show();
				if (0 != etIPEditText.getText().toString().length()) {
					IPAndPort addr = getIPAndPort(etIPEditText.getText().toString());
					m_IP = addr.strIPAddr;
					m_nPort = addr.nPort;
					Connect();
					m_popUpWindow.dismiss();
				}
				else {
					Toast.makeText(getApplicationContext(), 
							getResources().getString(R.string.alert_win_enter_ip_null_msg),
							4000).show();
				}
			}
		});
		
		popupHandler.sendEmptyMessageDelayed(0, 200);
		
	}
	
	private IPAndPort getIPAndPort (String strAddr) {
		IPAndPort ipPort = new IPAndPort();
		
		ipPort.nPort = 0;
		ipPort.strIPAddr = "";
		
		
		if (0 == strAddr.length()) {
			return ipPort;
		}
		
		String ss[] = strAddr.split(":");
		
		if (2 == ss.length) {
			//
			ipPort.strIPAddr = ss[0];
			ipPort.nPort = Integer.parseInt(ss[1], 10);
		}
		else {
			ipPort.strIPAddr = strAddr;
			ipPort.nPort = 80;
		}
		
		return ipPort;
	}
	
	private void SetFirstIP () {
		//
		
		//m_IP = "11111";
		//EditText mIPText;
		//new AlertDialog.Builder(this)
		//.setTitle(getResources().getString(R.string.alert_win_enter_ip_title))
		//.setMessage(getResources().getString(R.string.alert_win_enter_ip_msg))
		//.setView(mIPText = new EditText(this))
		//.setPositiveButton("OK", null)
		//.show();
		
		//PopupWindow p = new PopupWindow();
		
		//Toast.makeText(getApplicationContext(), mIPText.getText().toString(), 222).show();
		
		SharedPreferences sharedata = getSharedPreferences("data", 0);
		if (0 != sharedata.getString("IP", "").length()) {
			//m_IP = "192.168.1.40";		
			m_IP = sharedata.getString("IP", "");
			m_nPort = sharedata.getInt("Port", 80);
			return;
		}
		
		Context ctx = this;
		LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View vPopupWindow = inflater.inflate(R.layout.input_ip, null, false);
		m_popUpWindow = new PopupWindow(vPopupWindow, 300, 300, true); 
		m_popUpWindow.setWidth(LayoutParams.WRAP_CONTENT);
		m_popUpWindow.setHeight(LayoutParams.WRAP_CONTENT);
		
		
		Button btnOK = (Button)vPopupWindow.findViewById(R.id.inputIP_BtnOK);
		btnOK.setOnClickListener(new OnClickListener(){ 
			//
			@Override
			public void onClick(View v) {
				//
				//Log.d("SetFirstIP", "OnClick");
				//EditText etIPEditText = (EditText)findViewById(R.id.inputIPTextView);
				//Toast.makeText(getApplicationContext(), etIPEditText.getText().toString(),
						//5000).show();;
				//Log.d("SetFirstIP", etIPEditText.getText().toString());
				EditText etIPEditText = (EditText)vPopupWindow.findViewById(R.id.inputIPTextView);
				//Toast.makeText(getApplicationContext(), etIPEditText.getText().toString(), 5000).show();
				if (0 != etIPEditText.getText().toString().length()) {
					IPAndPort aa = getIPAndPort(etIPEditText.getText().toString());
					m_IP = aa.strIPAddr;
					m_nPort = aa.nPort;
					Connect();
					m_popUpWindow.dismiss();
				}
				else {
					Toast.makeText(getApplicationContext(), 
							getResources().getString(R.string.alert_win_enter_ip_null_msg),
							4000).show();
					
				}		
			}
		});
		
		//m_popUpWindow.showAtLocation(ChannelListActivity.this.findViewById(R.id.ChnBtnListMain), Gravity.CENTER, 0, 0);
		popupHandler.sendEmptyMessageDelayed(0, 200);
		//Log.d("SetFirstIP", "PostSendEmptyMessage");
		
		//Log.d("IPPIPIPIP", m_IP);
		//m_IP = "pr0zel.vicp.net";
		//m_nPort = 39728;
	}
	
	
	private Handler popupHandler = new Handler(){ 
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (KeyEvent.KEYCODE_BACK == keyCode) {
			if (0 == m_nExitTag) {
				//
				m_nExitTag = 1;
				m_lLastPressExitBtnTime = System.currentTimeMillis();
				Toast.makeText(this, getResources().getString(R.string.exit_toast_tips), 4000).show();
				//Toast.makeText(this, "Press one more time to Exit", 4000).show();
			}
			else {
				//m_nExitTag = 0;
				//m_lLastPressExitBtnTime = 0;
				if (4000 >= (System.currentTimeMillis() - m_lLastPressExitBtnTime)) {
					SharedPreferences.Editor sharedata = getSharedPreferences("data", 0).edit();
					sharedata.putString("IP", m_IP);
					sharedata.putInt("Port", m_nPort);
					sharedata.commit();
					
					//System.exit(0);
					this.finish();
				}
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		return this.detector.onTouchEvent(event);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		detector.onTouchEvent(ev);
		m_ChnListGridView.onTouchEvent(ev);
		return super.dispatchTouchEvent(ev);
	}
	
	private boolean isConnectedWifi() {
		Boolean bWifiIsOn = false;
		
		ConnectivityManager manager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		State wifiState = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
		
		if (State.CONNECTED == wifiState) {
			bWifiIsOn = true;
		}
		
		
		return bWifiIsOn;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onPostCreate(savedInstanceState);
		
		//SetFirstIP();
		InitInteface();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		SharedPreferences.Editor sharedata = getSharedPreferences("data", 0).edit();
		sharedata.putString("IP", m_IP);
		sharedata.putInt("Port", m_nPort);
		sharedata.commit();
		
		super.onDestroy();
	}

}
