package com.lzh.dmcontroler;

import java.net.ContentHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.LocalActivityManager;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout.LayoutParams;
import android.text.Html;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewDebug.FlagToString;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

import com.flurry.android.FlurryAgent;
import com.lzh.dmremote.R;

public class MainTabActivity extends Activity {

	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private int m_nCurrentView = 0;
	private static int m_nMaxTabIndex = 2;
	private Animation m_AnimaslideLeftIn; 
	private Animation m_AnimaslideLeftOut; 
	private Animation m_AnimaslideRightIn; 
	private Animation m_AnimaslideRightOut;
	private View m_CurView;
	
	private static boolean isExit = false;
	
	private TabHost mTabHost;
	private Intent mChnListIntent;
	private Intent mCurChnIntent;
	
	private String m_strIP = "";
	private int m_nPort = 80;
	private int m_nScreenHight;
	private int m_nScreenWidth;
	
	private GestureDetector m_gestureDetector; 
	private ViewFlipper m_viewFlipper;
	private View.OnTouchListener m_gestureListener;
	private Context m_thisContext;
	private ViewPager m_pager = null;
	
	private ProgressDialog mProgressDialog;
	private int m_count = 0;
	
	PopupWindow m_popUpWindow = null;
	PopupWindow m_popUpWindowAbout = null;
	
	LocalActivityManager m_actManager = null;
	List<View> m_listViews;
	
	private class IPAndPort {
		String strIPAddr;
		int nPort;
	}
	
	private enum ContentType {
		HINT, DATA, MSG
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		//DmRemoteSql ss = new DmRemoteSql(getBaseContext());
		//ss.initDB();
		//ss.AddReftoDB("高清翡翠台", "1:EEEEEE");
		//this.openOrCreateDatabase("test_db.db", Context.MODE_PRIVATE, null);
		setContentView(R.layout.viewpager);
		
		m_thisContext = MainTabActivity.this;
		m_actManager = new LocalActivityManager(this, true);
		m_actManager.dispatchCreate(savedInstanceState);
		
		m_pager = (ViewPager) findViewById(R.id.viewpager);
		
		//InitInteface();
	}
	
	private View CreateInputPopWindow()
	{	
		final View view = View.inflate(this, R.layout.input_ip, null);
		
		if (m_popUpWindow != null)
			return view;
		
		view.setBackgroundResource(R.drawable.rounded_corners_view);
		
		m_popUpWindow = new PopupWindow(view, LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, true);
		m_popUpWindow.setWidth(LayoutParams.WRAP_CONTENT);
		m_popUpWindow.setHeight(LayoutParams.WRAP_CONTENT);
		m_popUpWindow.setAnimationStyle(R.style.AnimationPreview);
		m_popUpWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_corners_pop));
		m_popUpWindow.setFocusable(true);
		
		Button btnOK = (Button)view.findViewById(R.id.inputIP_BtnOK);
		btnOK.setOnClickListener(new OnClickListener() { 
			//
			@Override
			public void onClick(View v) {
				//
				EditText etIPEditText = (EditText)view.findViewById(R.id.inputIPTextView);
				//Toast.makeText(getApplicationContext(), etIPEditText.getText().toString(), 5000).show();
				if (0 != etIPEditText.getText().toString().length()) {
					IPAndPort addr = getIPAndPort(etIPEditText.getText().toString());
					m_strIP = addr.strIPAddr;
					m_nPort = addr.nPort;
					//Connect();
					InitInteface();
					m_popUpWindow.dismiss();
				}
				else {
					Toast.makeText(getApplicationContext(), 
							getResources().getString(R.string.alert_win_enter_ip_null_msg),
							3000).show();
				}
			}
		});
		
		Button btnScanIP = (Button)view.findViewById(R.id.inputIP_BtnScanIP);
		
		btnScanIP.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				FlurryAgent.logEvent("ScanIP_Click");
				if (true == isConnectedWifi()) {
					
					m_popUpWindow.dismiss();
					
					FlurryAgent.logEvent("ScanIP_Start");
					ScanIPFunc(MainTabActivity.this);
				}
				else {
					// no wifi
					Toast.makeText(getApplicationContext(), 
							getResources().getString(R.string.connect_wifi_toast), 3000).show();
				}
			}
		});
		
		m_popUpWindow.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss() {
				// TODO Auto-generated method stub
				WindowManager.LayoutParams lp = getWindow().getAttributes();
				lp.alpha = 1f;
				getWindow().setAttributes(lp);
			}
		});
		
		return view;
	}
	
	private void InitInteface() {
		final Resources res = getResources();
		m_nScreenHight = res.getDisplayMetrics().heightPixels;
		m_nScreenWidth = res.getDisplayMetrics().widthPixels;
		
		mChnListIntent = new Intent(m_thisContext, ChannelListAct.class);
		mCurChnIntent = new Intent(m_thisContext, CurrentChnActivity.class);
		
		//mChnListIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		//mCurChnIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		mCurChnIntent.putExtra(IntentExtDataDef.IP, m_strIP);
		mCurChnIntent.putExtra(IntentExtDataDef.Port, m_nPort);
		mCurChnIntent.putExtra(IntentExtDataDef.ScreenHight, m_nScreenHight);
		mCurChnIntent.putExtra(IntentExtDataDef.ScreenWidth, m_nScreenWidth);
		
		mChnListIntent.putExtra(IntentExtDataDef.IP, m_strIP);
		mChnListIntent.putExtra(IntentExtDataDef.Port, m_nPort);
		mChnListIntent.putExtra(IntentExtDataDef.ScreenHight, m_nScreenHight);
		mChnListIntent.putExtra(IntentExtDataDef.ScreenWidth, m_nScreenWidth);
		
		m_listViews = new ArrayList<View>();
		final View chnlistView = getView("A", mChnListIntent);
		final View curChnView = getView("C", mCurChnIntent);
		m_listViews.add(chnlistView);
		m_listViews.add(curChnView);
		
		mTabHost = (TabHost) findViewById(R.id.tabhost1);
		mTabHost.setup();
		mTabHost.setup(m_actManager);
		mTabHost.clearAllTabs(); // 清除以前的tab
		
		RelativeLayout tabIndicator1 = (RelativeLayout) LayoutInflater.from(m_thisContext).inflate(R.layout.tabwidget, null);
		TextView tvTab1 = (TextView) tabIndicator1.findViewById(R.id.tv_title);
		tvTab1.setText(getResources().getString(R.string.chn_disp_title));
		RelativeLayout tabIndicator2 = (RelativeLayout) LayoutInflater.from(m_thisContext).inflate(R.layout.tabwidget, null);
		TextView tvTab2 = (TextView) tabIndicator2.findViewById(R.id.tv_title);
		tvTab2.setText(getResources().getString(R.string.crt_chn_title));
		
		Intent intent = new Intent(m_thisContext, EmptyActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		mTabHost.addTab(mTabHost.newTabSpec("A").setIndicator(tabIndicator1).setContent(intent));
		mTabHost.addTab(mTabHost.newTabSpec("B").setIndicator(tabIndicator2).setContent(intent));
		
		m_pager.setAdapter(new MyPageAdapter(m_listViews));
		m_pager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
				//mTabHost.setCurrentTab(arg0);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onPageSelected(int arg0) {
				// TODO Auto-generated method stub
				FlurryAgent.logEvent("Act_Slide");
				mTabHost.setCurrentTab(arg0);
			}
		});
		
		mTabHost.setOnTabChangedListener(new OnTabChangeListener() {

			@Override
			public void onTabChanged(String tabId) {
				// TODO Auto-generated method stub
				if ("A".equals(tabId)) {
					m_pager.setCurrentItem(0);
				}
				if ("B".equals(tabId)) {
					m_pager.setCurrentItem(1);
				}
			}
			
		});
	}
	
	private View getView(String id, Intent intent) {
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return m_actManager.startActivity(id, intent).getDecorView();
	}
	private void SaveToShareData() {
		//保存数据
		if (0 == m_strIP.length())
			return;
		
		SharedPreferences.Editor shareWriteData = getSharedPreferences("data", 0).edit();
		shareWriteData.putInt(IntentExtDataDef.ScreenWidth, m_nScreenWidth);
		shareWriteData.putInt(IntentExtDataDef.ScreenHight, m_nScreenHight);
		shareWriteData.putString(IntentExtDataDef.IP, m_strIP);
		shareWriteData.putInt(IntentExtDataDef.Port, m_nPort);
		shareWriteData.commit();
	}
	
	private void LoadFromShareData() {
		SharedPreferences shareReadData = getSharedPreferences("data", 0);
		m_strIP = shareReadData.getString(IntentExtDataDef.IP, "");
		m_nPort = shareReadData.getInt(IntentExtDataDef.Port, 80);
		m_nScreenHight = shareReadData.getInt(IntentExtDataDef.ScreenHight, 1280);
		m_nScreenWidth = shareReadData.getInt(IntentExtDataDef.ScreenWidth, 720);
	}
	
	private class MyPageAdapter extends PagerAdapter {
		private List<View> list;
		
		private MyPageAdapter(List<View> list) {
			this.list = list;
		}

		@Override
		public void finishUpdate(View container) {
			// TODO Auto-generated method stub
			super.finishUpdate(container);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return list.size();
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			ViewPager pViewPager = ((ViewPager) container);
            pViewPager.addView(list.get(position));
            return list.get(position);
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			// TODO Auto-generated method stub
			return arg0 == arg1;
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			ViewPager pViewPager = ((ViewPager) container);
			pViewPager.removeView(list.get(position));
		}

		@Override
		public Parcelable saveState() {
			return null;
		}
	}
	
	private static Handler mExitHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			isExit = false;
		}
	};
	
	private void MyExit() {
		if (!isExit) {
			isExit = true;
			String strDisp = getResources().getString(R.string.exit_toast_tips);
			Toast.makeText(getApplicationContext(), strDisp, 2000).show();
			mExitHandler.sendEmptyMessageDelayed(0, 3000);
		}
		else {
			SaveToShareData();
			this.finish();
		}
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		// TODO Auto-generated method stub
		if (KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
			if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
				MyExit();
				return true;
			}
		}
		return super.dispatchKeyEvent(event);
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
		
		String strFiltIP = strFiltIPType(ipPort.strIPAddr);
		ipPort.strIPAddr = strFiltIP;
		
		return ipPort;
	}
	
	private String strFiltIPType(String strInIP) {
		String strRet = "";
		//转换一些SB们输入192.168.001.002的地址
		String[] strIPSpt = strInIP.split("\\.");
		if (4 != strIPSpt.length) {
			strRet = strInIP;
			return strRet;
		}
		for (int i=0; i < 4; i++) {
			if (2 > strIPSpt[i].length())
				continue;
			
			if ("0".equals(strIPSpt[i].substring(0, 1))) {
				String sTmp = strIPSpt[i].substring(1, strIPSpt[i].length());
				strIPSpt[i] = sTmp;
			}
			if ("0".equals(strIPSpt[i].substring(0, 1))) {
				String sTmp2 = strIPSpt[i].substring(1, strIPSpt[i].length());
				strIPSpt[i] = sTmp2;
			}
				
		}
		
		strRet =  strIPSpt[0] + "." + strIPSpt[1] + "." + strIPSpt[2] + "." + strIPSpt[3];
		
		return strRet;
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
	
	private Handler m_hScanHandler = new Handler() { 
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				View view = View.inflate(MainTabActivity.this, R.layout.input_ip, null);
				
				m_popUpWindow.showAtLocation(view, Gravity.CENTER, 10, 10);
				
				Toast.makeText(getApplicationContext(), 
						getResources().getString(R.string.scanip_no_found_ip), 
						3000).show();
				FlurryAgent.logEvent("ScanIP_notFound");
				
				// 准备开启 扫描IP扫不到的情况下发邮件 只发一次。
				//IPNotFoundSendFeedBack();
				break;
				
			case 1:
				Toast.makeText(getApplicationContext(), 
						getResources().getString(R.string.scanip_found_ip_connect),
						3000).show();
				
				List<String> listDm = (List<String>)msg.obj;
				
				IPAndPort addr = getIPAndPort(listDm.get(0));
				m_strIP = addr.strIPAddr;
				m_nPort = addr.nPort;
				
				SaveToShareData();
				
				Map<String, String> map = new HashMap<String, String>();
				map.put("ip", m_strIP);
				map.put("port", String.valueOf(m_nPort));
				FlurryAgent.logEvent("ScanIP_Found", map);
				
				Intent i = getBaseContext().getPackageManager()
						.getLaunchIntentForPackage(getBaseContext().getPackageName());
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
				break;
			}
		}
	};
	
	private void ScanIPFunc(Context context) {
		
		IpScanTask ipScanTask = new IpScanTask(context);
		
		ipScanTask.start(m_hScanHandler);
	}
	
	private Handler m_hPopupHandler = new Handler() { 
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				m_popUpWindow.showAtLocation(findViewById(R.id.viewpager),
						Gravity.CENTER, 0, 0);
				//m_popUpWindow.showAtLocation(getTabHost(), Gravity.CENTER, 0, 0);
				m_popUpWindow.update();
				break;
			}
		}
	};
	
	private Handler m_hAboutWinPopHandle = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case 0:
				m_popUpWindowAbout.showAtLocation(findViewById(R.id.viewpager),
						Gravity.CENTER, 0, 0);
				m_popUpWindowAbout.update();
				break;
			}
		}
	};
	
	private void setPopupCover(Context ctx, LayoutInflater inflater, View vParentPopupWindow) {
		//
		final View vPopView = inflater.inflate(R.layout.process, null, false);
		PopupWindow kPopupWin = new PopupWindow(vPopView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
		kPopupWin.showAtLocation(findViewById(R.id.viewpager), Gravity.CENTER, 0, 0);
	}
	
	private void EnterAnNewIP() {
		
		final View vPopupWindow = CreateInputPopWindow();
		
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.alpha = 0.3f;
		getWindow().setAttributes(lp);
		
		EditText et = (EditText)vPopupWindow.findViewById(R.id.inputIPTextView);
		et.setHint(getResources().getString(R.string.alert_win_enter_ip_hint));
		if (80 == m_nPort)
			et.setText(m_strIP);
		else {
			String ssSet = m_strIP;
			ssSet += ":";
			ssSet += m_nPort;
			et.setText(ssSet);
		}
		
		m_hPopupHandler.sendEmptyMessageDelayed(0, 200);
	}
	
	private void SetFirstIP() {
		LoadFromShareData();
		
		if (0 != m_strIP.length()) {
			InitInteface();
			return;
		}
		
		final View vPopupWindow = CreateInputPopWindow();
		
		vPopupWindow.setBackgroundResource(R.drawable.rounded_corners_view);
		
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.alpha = 0.3f;
		getWindow().setAttributes(lp);
		
		EditText et = (EditText)vPopupWindow.findViewById(R.id.inputIPTextView);
		et.setHint(getResources().getString(R.string.alert_win_enter_ip_hint));
		
		if (80 == m_nPort)
			et.setText(m_strIP);
		else {
			String ssSet = m_strIP;
			ssSet += ":";
			ssSet += m_nPort;
			et.setText(ssSet);
		}
		
		m_hPopupHandler.sendEmptyMessageDelayed(0, 200);
	}

	private void ShowAbout() {
		Context ctx = this;
		LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View vPopupWindow = inflater.inflate(R.layout.about, null, false);
		//m_popUpWindowAbout = new PopupWindow(vPopupWindow, 400, 700, true);
		m_popUpWindowAbout = new PopupWindow(vPopupWindow, LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, true);
		vPopupWindow.setBackgroundResource(R.drawable.rounded_corners_view);
		m_popUpWindowAbout.setWidth(LayoutParams.WRAP_CONTENT);
		m_popUpWindowAbout.setHeight(LayoutParams.WRAP_CONTENT);
		m_popUpWindowAbout.setAnimationStyle(R.style.AnimationPreview);
		m_popUpWindowAbout.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_corners_pop));
		m_popUpWindowAbout.setFocusable(true);
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.alpha = 0.3f;
		getWindow().setAttributes(lp);
		
		m_popUpWindowAbout.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss() {
				// TODO Auto-generated method stub
				WindowManager.LayoutParams lp = getWindow().getAttributes();
				lp.alpha = 1.0f;
				getWindow().setAttributes(lp);
			}
		});
		
		final int padding = getResources().getDimensionPixelSize(R.dimen.pnl_margin);
		TextView tv1 = (TextView)vPopupWindow.findViewById(R.id.textView_aboutwin);
		tv1.setPadding(padding, padding, padding, padding);
		tv1.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
		//tv1.setTextSize(getResources().getDimension(R.dimen.text_small));
		tv1.setTextColor(Color.parseColor("#ffffff"));
		tv1.setGravity(Gravity.NO_GRAVITY);
		tv1.setTag(ContentType.MSG);
		tv1.setText(Html.fromHtml(getResources().getString(R.string.about_win_show_text)));
		tv1.setMovementMethod(ScrollingMovementMethod.getInstance());
		tv1.setMovementMethod(LinkMovementMethod.getInstance());
		
		m_hAboutWinPopHandle.sendEmptyMessageDelayed(0, 200);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, 1, 1, getResources().getString(R.string.opt_menu_setip));
		menu.add(Menu.NONE, 2, 3, getResources().getString(R.string.opt_munu_exit));
		menu.add(Menu.NONE, 3, 2, getResources().getString(R.string.opt_menu_about));
		//menu.add(Menu.NONE, 4, 3, getResources().getString(R.string.opt_menu_feedback));
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId()) {
		case 1:
			FlurryAgent.logEvent("Menu_EnterIP");
			EnterAnNewIP();
			break;
		case 2:
			SaveToShareData();
			this.finish();
			break;
		case 3:
			FlurryAgent.logEvent("Menu_About");
			ShowAbout();
			break;
		case 4:
			PackageInfo pkg = null;
			String strSubject;
			String strText;
			try {
				pkg = getPackageManager().getPackageInfo(getApplication().getPackageName(), 0);
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Intent data = new Intent(Intent.ACTION_SENDTO); 
			data.setData(Uri.parse("mailto:dmremotea@gmail.com"));
			if (null == pkg)
				strSubject = "DmRemote feedback";
			else
				strSubject = "DmRemote v" + pkg.versionName + " feedback";
			
			strText = "Product Model: " + android.os.Build.MODEL + ", " + android.os.Build.VERSION.SDK
					+ ", " + android.os.Build.VERSION.RELEASE + ", " + Locale.getDefault().getLanguage() + "-"
					+ Locale.getDefault().getCountry() + "\n";
			DisplayMetrics dm = new DisplayMetrics();
			WindowManager windowMgr = (WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
			windowMgr.getDefaultDisplay().getMetrics(dm);
			
			strText += "Resolution: " + dm.widthPixels + " * " + dm.heightPixels + "\n\n";
			
			data.putExtra(Intent.EXTRA_SUBJECT, strSubject);
			data.putExtra(Intent.EXTRA_TEXT, strText);
			startActivity(data);
			
			
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void IPNotFoundSendFeedBack() {
		
		PackageInfo pkg = null;
		String strSubject;
		String strText;
		
		SharedPreferences shareReadData = getSharedPreferences("data", 0);
		
		if (1 == shareReadData.getInt(IntentExtDataDef.IPNotFoundSendMail, 0)) {
			return;
		}
		
		SharedPreferences.Editor shareWriteDataEditor = getSharedPreferences("data", 0).edit();
		shareWriteDataEditor.putInt(IntentExtDataDef.IPNotFoundSendMail, 1);
		shareWriteDataEditor.commit();
		
		Intent data = new Intent(Intent.ACTION_SENDTO); 
		data.setData(Uri.parse("mailto:dmremotea@gmail.com"));
		if (null == pkg)
			strSubject = "DmRemote Ip not found feedback";
		else
			strSubject = "DmRemote v" + pkg.versionName + "ip not found feedback";
		
		strText = "Product Model: " + android.os.Build.MODEL + ", " + android.os.Build.VERSION.SDK
				+ ", " + android.os.Build.VERSION.RELEASE + ", " + Locale.getDefault().getLanguage() + "-"
				+ Locale.getDefault().getCountry() + "\n";
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager windowMgr = (WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
		windowMgr.getDefaultDisplay().getMetrics(dm);
		
		strText += "Resolution: " + dm.widthPixels + " * " + dm.heightPixels + "\n\n";
		
		data.putExtra(Intent.EXTRA_SUBJECT, strSubject);
		data.putExtra(Intent.EXTRA_TEXT, strText);
		startActivity(data);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		SetFirstIP();
		super.onPostCreate(savedInstanceState);
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		FlurryAgent.onStartSession(this, "Z974DHPGXN7K3PTFMYT8");
		super.onStart();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		SaveToShareData();
		FlurryAgent.onEndSession(this);
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
}
