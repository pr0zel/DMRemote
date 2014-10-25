package com.lzh.dmcontroler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.flurry.android.FlurryAgent;
import com.lzh.dmcontroler.E2Service.E2CurrentServiceHandler;
import com.lzh.dmcontroler.E2Service.E2EpgNowNextListHandler;
import com.lzh.dmcontroler.E2Service.E2ServiceListHandler;
import com.lzh.dmcontroler.helpers.DreamSaxParser;
import com.lzh.dmcontroler.helpers.Event;
import com.lzh.dmcontroler.helpers.ExtendedHashMap;
import com.lzh.dmcontroler.helpers.Profile;
import com.lzh.dmcontroler.helpers.SimpleHttpClient;
import com.lzh.dmcontroler.helpers.URIStore;
import com.lzh.dmremote.R;

import android.content.Context;
import android.graphics.Color;
import android.location.GpsStatus.NmeaListener;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class ChannelList2 extends BaseAdapter {
	
	// channel list ref time is 2 minunts
	private static int m_nChnListRefTime = 120;
	
	public static class Item{
		public String strChannelName;  //频道名
		public String strProviderName; //提供者名
		public String strCurProgName; //当前节目名
		public String strNextProgName; //下一节目名
		public String strChnRef; //频道ref代码
		public int nProgTime; // 当前节目总时间
		//public int resId;
	}
	
	public static class CurPlaying {
		public String strChnRef;
		public String strChnName;
		public String strCurProgName;
	}
	
	public static class ServiceListItem {
		public String strServiceRef;
		public String strServiceName;
	}
	
	private CurPlaying m_CurPlaying;
	
	private List<Item> mItems = new ArrayList<ChannelList2.Item>();
	private SimpleHttpClient m_httpClient = null;
	private Context mContext;
	private List<Map<String, Object>> mServiceItems;
	
	private List<ServiceListItem> m_ServiceListItem = new ArrayList<ChannelList2.ServiceListItem>();
	
	private int m_ScreenWidth = 0;
	private int m_ScreenHight = 0;
	
	private String m_strIP = "";
	private int m_nPort = 0;
	
	Handler m_hRefChnList = new Handler();
	Runnable m_runableRefChnList = new Runnable(){
		@Override
		public void run() {
			refChnList();
			m_hRefChnList.postDelayed(this, 1000 * m_nChnListRefTime);
		}
	};
	
	public ChannelList2(Context context) {
		mContext = context;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mItems.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		//return null;
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}
	
	public void SetScreen(int nWidth, int nHight) {
		m_ScreenHight = nHight;
		m_ScreenWidth = nWidth;
	}
	
	public void SetContext(Context context) {
		mContext = context;
	}

	public void setIPAndConnect(String strIP, int nPort) {
		//
		if (0 == strIP.length() || 0 == nPort) {
			return;
		}
		
		m_strIP = strIP;
		m_nPort = nPort;
		
		Profile t_profile = new Profile ("Default", m_strIP, "", m_nPort, 8001, 80,
				false, "", "", false, false, false, false, false);
		
		mItems.clear();
		((BaseAdapter)this).notifyDataSetChanged();
		
		m_httpClient = new SimpleHttpClient(t_profile);
		
		//ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		//params.add(new BasicNameValuePair("bRef", "1:7:1:0:0:0:0:0:0:0:"));
		//(new getChnEpgNow()).execute(params);
		
		
		(new GetServiceGroupTask()).execute("");
	}
	
	public void refChnList() {
		if (0 == m_strIP.length()) {
			return;
		}
		
		mItems.clear();
		//String sss = "1:7:1:0:0:0:0:0:0:0: ORDER BY provider";
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("bRef", "1:7:1:0:0:0:0:0:0:0:"));		
		(new getChnEpgNow()).execute(params);
	}
	
	// 切换频道 nCHNID是显示频道列表的id号， bIsChange是是否切换频道的标志
	public void ChangeChannel(int nCHNID, Boolean bIsChange) {
		
		if (false == bIsChange)
			return;
		
		String ss = mItems.get(nCHNID).strChnRef;
		String strChannelName = mItems.get(nCHNID).strChannelName;
		
		String strChnNameDisplay = mContext.getResources().getString(R.string.chn_lst_active_chn_toase) + strChannelName;
		
		Toast.makeText(mContext, strChnNameDisplay, 1000).show();
		
		
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("sRef", mItems.get(nCHNID).strChnRef));
		(new ChnChannelTask()).execute(params);
		
		Map<String, String> map = new HashMap<String, String>();
		map.put("ChnRef", ss);
		map.put("ChnName", strChannelName);
		FlurryAgent.logEvent("ChangeChannel", map);
		
	}
	
	private String GetCurChannel() {
		String curChn = "";
		if (null == m_httpClient) {
			return curChn;
		}
		
		m_httpClient.fetchPageContent(URIStore.SUBSERVICES);
		String ss = m_httpClient.getPageContentString();
		
		return ss;
		
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		if (null == convertView) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item, null);
		}
		
		String strChnNameDisplay = mContext.getResources().getString(R.string.chn_list_crt_prog);
		String strCurrentProgTimeDisp = "";
		String strCurrentProgNameDisp = "";
		
		TextView chnNameTextView = (TextView)convertView.findViewById(R.id.listitemchnname);
		TextView chnCurProgName = (TextView)convertView.findViewById(R.id.listitemcurprogname);
		TextView chnCurProgTime = (TextView)convertView.findViewById(R.id.listitemcurprogtime);
		
		chnNameTextView.setTextSize(20);
		chnCurProgName.setTextSize(17);
		Item curItem = (Item)getItem(position);
		strChnNameDisplay += curItem.strCurProgName;
		if (12 < curItem.strCurProgName.length()) {
			strCurrentProgNameDisp = curItem.strCurProgName.substring(0, 12);
			strCurrentProgNameDisp += "..";
		}
		else {
			strCurrentProgNameDisp = curItem.strCurProgName;
		}
		strCurrentProgTimeDisp = curItem.nProgTime + mContext.getResources().getString(R.string.chn_list_crt_prog_time);
		chnNameTextView.setText(curItem.strChannelName);
		//chnCurProgName.setText(strChnNameDisplay);
		chnCurProgName.setText(strCurrentProgNameDisp);
		chnCurProgTime.setText(strCurrentProgTimeDisp);
		
		chnNameTextView.setBackgroundColor(Color.parseColor("#5a7285"));
		
		return convertView;
	}

	protected void finishListProgress(String title, ArrayList<ExtendedHashMap> list, List<Item> processItem) {
		//
		processItem.clear();
		
		for (ExtendedHashMap map:list) {
			//
			Item object = new Item();
			object.strChannelName = map.getString(Event.KEY_SERVICE_NAME);
			object.strChnRef = map.getString(Event.KEY_SERVICE_REFERENCE);
			object.strProviderName = map.getString(Event.KEY_SERVICE_PROVIDER_NAME);
			if ("None".equals(map.getString(Event.KEY_EVENT_DURATION))) {
				object.nProgTime = 0;
			}
			else
				object.nProgTime = Integer.valueOf(map.getString(Event.KEY_EVENT_DURATION), 10) / 60;
			
			processItem.add(object);
		}
		
		((BaseAdapter)this).notifyDataSetChanged();
	}
	
	public static class classSplitRefItem {
		public String strSrvRefID;
		public String strSrvID;
	}
	
	protected classSplitRefItem getServiceRef(String strServiceList) {
		String strRet = "";
		classSplitRefItem object = new classSplitRefItem();
		//1:7:1:0:0:0:0:0:0:0:FROM BOUQUET "userbouquet.dbe00.tv" ORDER BY bouquet
		//1:7:1:0:0:0:0:0:0:0:
		
		object.strSrvRefID = strServiceList.substring(0, 21);
		object.strSrvID = "";
		
		
		
		return object;
	}
	
	protected void finishListProgress(ArrayList<ExtendedHashMap> list, List<Item> processItem) {
		//
		processItem.clear();
		for (ExtendedHashMap map:list) {
			Item object = new Item();
			object.strChannelName = map.getString(Event.KEY_SERVICE_NAME);
			object.strChnRef = map.getString(Event.KEY_SERVICE_REFERENCE);
			object.strCurProgName = map.getString(Event.KEY_EVENT_TITLE);
			if ("None".equals(map.getString(Event.KEY_EVENT_DURATION))) {
				object.nProgTime = 0;
			}
			else
				if ("None".equals(map.getString(Event.KEY_EVENT_DURATION))) {
					object.nProgTime = 0;
				}else {
					//
					object.nProgTime = Integer.valueOf(map.getString(Event.KEY_EVENT_DURATION), 10) / 60;
				}
				
			//object.strNextProgName = map.getString(Event.KEY_NEXT_EVENT_TITLE);
			
			processItem.add(object);
		}
		((BaseAdapter)this).notifyDataSetChanged();
	}
	
	protected void finishProServiceList(ArrayList<ExtendedHashMap> list) {
		m_ServiceListItem.clear();
		
		for (ExtendedHashMap map:list) {
			ServiceListItem sListItem = new ServiceListItem();
			sListItem.strServiceName = map.getString(Event.KEY_SERVICE_NAME);
			sListItem.strServiceRef = map.getString(Event.KEY_SERVICE_REFERENCE);
			m_ServiceListItem.add(sListItem);
		}
	}
	
	private class GetServiceGroupTask extends
		AsyncTask<String, String, Boolean> {
		
		protected DreamSaxParser m_ServiceListParser;
		protected ArrayList<ExtendedHashMap> m_TaskList;
		
		//m_ServiceListItem
		
		public GetServiceGroupTask () {
			super();
			E2ServiceListHandler handler = new E2ServiceListHandler();
			m_TaskList = new ArrayList<ExtendedHashMap>();
			handler.setList(m_TaskList);
			m_ServiceListParser = new DreamSaxParser(handler);
			
		}
		
		
		@Override
		protected Boolean doInBackground(String... params) {
			// TODO Auto-generated method stub
			
			String xml = null;
			if (m_httpClient.fetchPageContent(URIStore.SERVICES)) {
				xml = m_httpClient.getPageContentString();
			}
			
			if ((null != xml) && (!isCancelled())) {
				return m_ServiceListParser.parse(xml);
			}
			
			return false;
		}
		//

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			finishProServiceList(m_TaskList);
			super.onPostExecute(result);
		}
	}
	
	//取当前频道列表，并显示的工作线程
	private class GetServiceListTask extends
		AsyncTask<ArrayList<NameValuePair>, String, Boolean> {
	
		protected ArrayList<ExtendedHashMap> mTaskList;
		private DreamSaxParser m_ChnListSaxParser;
		
		public GetServiceListTask() {
			super();
			E2ServiceListHandler handle = new E2ServiceListHandler();
			mTaskList = new ArrayList<ExtendedHashMap>();
			handle.setList(mTaskList);
			m_ChnListSaxParser = new DreamSaxParser(handle);
		}
	
		@Override
		protected Boolean doInBackground(ArrayList<NameValuePair>... params) {
			
			String xml = null;
			if (m_httpClient.fetchPageContent(URIStore.EPG_NOW, params[0])) {
				xml = m_httpClient.getPageContentString();
			}
			
			if (null != xml && !isCancelled()) {
				return m_ChnListSaxParser.parse(xml);
			}
			
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			
			finishListProgress(mTaskList, mItems);
			super.onPostExecute(result);
		}

		@Override
		protected void onProgressUpdate(String... values) {
			
			super.onProgressUpdate(values);
		}
	
	}

	//切换频道线程
	private class ChnChannelTask extends
	AsyncTask<ArrayList<NameValuePair>, String, Boolean> {
		
		String m_ChnResult = "";
		
		@Override
		protected Boolean doInBackground(ArrayList<NameValuePair>... params) {
			//return null;
			if (null == m_httpClient)
				return true;
			m_httpClient.fetchPageContent(URIStore.ZAP, params[0]);
			m_ChnResult = m_httpClient.getPageContentString();
			return true;
		}

		//@Override
		//protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			//finishChangeChannel(m_ChnResult);
			//super.onPostExecute(result);
		//}
		
	}
	
	//取所有频道的当前播放节目与下个节目信息
	private class getChnEpgNow extends
		AsyncTask<ArrayList<NameValuePair>, String, Boolean> {
		
		protected ArrayList<ExtendedHashMap> mTaskList;
		private DreamSaxParser m_ChnListSaxParser;
		
		// bRef=1:7:1:0:0:0:0:0:0:0
		public getChnEpgNow() {
			super();
			E2EpgNowNextListHandler handle = new E2EpgNowNextListHandler();
			mTaskList = new ArrayList<ExtendedHashMap>();
			handle.setList(mTaskList);
			m_ChnListSaxParser = new DreamSaxParser(handle);
		}
		
		//
		@Override
		protected Boolean doInBackground(ArrayList<NameValuePair>... params) {
			
			//http://dream.reichholf.net/wiki/Enigma2:WebInterface
			String xml = null;
			if (null == m_httpClient)
				return true;
			if (m_httpClient.fetchPageContent(URIStore.EPG_NOW, params[0])) {
				xml = m_httpClient.getPageContentString();
			}
			
			if (null != xml && !isCancelled()) {
				return m_ChnListSaxParser.parse(xml);
			}
			
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			finishListProgress(mTaskList, mItems);
			super.onPostExecute(result);
		}
	}
	
	
	// 取当前播放频道信息
	// 未完成，parse那里会卡死，原因未明
	private class getCurPlaying extends
		AsyncTask<ArrayList<NameValuePair>, String, Boolean> {
		
		protected DreamSaxParser m_SaxParser;
		protected ExtendedHashMap mExMap;
		
		public getCurPlaying() {
			//
			super();
			E2CurrentServiceHandler handle = new E2CurrentServiceHandler();
			handle.setMap(mExMap);
			m_SaxParser = new DreamSaxParser(handle);
		}
		
		@Override
		protected Boolean doInBackground(ArrayList<NameValuePair>... params) {
			// TODO Auto-generated method stub
			
			String xml = null;
			if (m_httpClient.fetchPageContent(URIStore.CURRENT)) {
				xml = m_httpClient.getPageContentString();
			}
			if (null != xml && !isCancelled()) {
				return m_SaxParser.parse(xml);
				
				//m_CurPlaying.strChnName = mExMap.getString(URIStore.)
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			m_CurPlaying = new CurPlaying();
			
			
			super.onPostExecute(result);
		}
	}
	
}
