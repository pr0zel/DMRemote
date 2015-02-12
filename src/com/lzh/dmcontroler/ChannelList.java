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

public class ChannelList extends BaseAdapter {
	
	// channel list ref time is 1 minunt
	private static int m_nChnListRefTime = 60;
	private static Boolean m_bUploadChnNumberTig = false;
		
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
	
	private CurPlaying m_CurPlaying;
	
	private List<Item> mItems = new ArrayList<ChannelList.Item>();
	private SimpleHttpClient m_httpClient = null;
	private Context mContext;
	private List<Map<String, Object>> mServiceItems;
	
	private int m_ScreenWidth = 0;
	private int m_ScreenHight = 0;
	
	private String m_strIP = "";
	private int m_nPort = 0;
	
	private DmRemoteSql m_dmRemoteSql;
	private String[] m_strChnRef;
	private Boolean m_bSortChnnel = true;
	
	public ChannelList(Context context) {
		mContext = context;
		m_dmRemoteSql = new DmRemoteSql(mContext);
		//m_strChnRef = m_dmRemoteSql.getChnRefCountInDb();
		
		//m_hRefChnList.postDelayed(m_runableRefChnList, 1000 * m_nChnListRefTime);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		if (null == mItems)
			return 0;
		
		return mItems.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		//return null;
		if (null == mItems)
			return null;
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}
	
	Handler m_hRefChnList = new Handler();
	Runnable m_runableRefChnList = new Runnable(){
		@Override
		public void run() {
			refChnList();
			m_hRefChnList.postDelayed(this, 1000 * m_nChnListRefTime);
		}
	};
	
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
		//String sss = "1:7:1:0:0:0:0:0:0:0:(type == 1) || (type == 17) || (type == 195) || (type == 25) ORDER BY provider";
		//String sss = "1:7:1:0:0:0:0:0:0:0:FROM BOUQUET \"userbouquet.favourites.tv\" ORDER BY provider";
		//String sss = "1:7:1:0:0:0:0:0:0:0: ORDER BY provider";
		
		//ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		//params.add(new BasicNameValuePair("sRef", sss));
		//(new GetServiceListTask()).execute(params);
		m_bUploadChnNumberTig = true;
		
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("bRef", "1:7:1:0:0:0:0:0:0:0:"));
		//(new GetServiceListTask()).execute(params);
		(new getChnEpgNow()).execute(params);
		
		m_hRefChnList.postDelayed(m_runableRefChnList, 1000 * m_nChnListRefTime);
		
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
		
		if (true == m_bUploadChnNumberTig) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("ChnRef", ss);
			map.put("ChnName", strChannelName);
			FlurryAgent.logEvent("ChangeChannel", map);
			m_bUploadChnNumberTig = false;
		}
		
		
		m_dmRemoteSql.AddReftoDB(strChannelName, ss);
		
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
		if (15 < curItem.strCurProgName.length()) {
			strCurrentProgNameDisp = curItem.strCurProgName.substring(0, 15);
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
	/*
	private String ChangeChnName(String strOri) {
		String strRet = "";
		if ("翡翠台".equals(strOri)) {
			strRet = "Jade";
		}
		else if ("明珠台".equals(strOri)) {
			strRet = "Pearl";
		}
		else if ("本港台".equals(strOri))
			strRet = "aTV Home";
		else if ("亞洲台".equals(strOri))
			strRet = "aTV Asia";
		else if ("歲月留聲".equals(strOri))
			strRet = "aTV Classic";
		else if ("CCTV-1 綜合頻道".equals(strOri))
			strRet = "CCTV-1";
		else if ("國際台".equals(strOri))
			strRet = "aTV World";
		else if ("深圳衛視".equals(strOri))
			strRet = "SZTV";
		else if ("香港電台高清頻道".equals(strOri))
			strRet = "RTHK HD";
		else if ("港台電視 32".equals(strOri))
			strRet = "RTHK 32";
		else if ("香港電台標清頻道二".equals(strOri))
			strRet = "RTHK 31";
		else if ("J2台".equals(strOri))
			strRet = "J2";
		else if ("互動新聞台".equals(strOri))
			strRet = "iNews";
		else if ("高清翡翠台".equals(strOri))
			strRet = "HD Jade";
		else
			strRet = strOri;
		
		return strRet;
	}
	*/
	
	// 排序
	protected List<Item> sortChnItems(List<Item> processItem) {
		List<Item> itemRet = null;
		List<Item> itemInDb = null;
		List<Item> itemNotInDb = null;
		Boolean bTag = false;
		
		itemRet = new ArrayList<Item>();
		itemRet.clear();
		itemInDb = new ArrayList<Item>();
		itemInDb.clear();
		itemNotInDb = new ArrayList<Item>();
		itemNotInDb.clear();
		
		// 1是倒序列  2是顺序排列  0是不排列
		String[] strRefCount = m_dmRemoteSql.getChnRefCountInDb(1);
		
		if (null != strRefCount)
			//if (0 != strRefCount.length) {
			if (2 <= strRefCount.length) {
				//
				for (Item map:processItem) {
					bTag = false;
					for (String strtMp:strRefCount) {
						if (map.strChnRef.equalsIgnoreCase(strtMp)) {
							itemInDb.add(map);
							bTag = true;
							break;
						}
					}
					
					if (false == bTag) {
						itemNotInDb.add(map);
					}
				}
				
				if (0 != itemInDb.size()) {
					for (String strRef:strRefCount) {
						for (Item map:itemInDb) {
							if (map.strChnRef.equalsIgnoreCase(strRef)) {
								itemRet.add(map);
								continue;
							}
						}
					}
				}
				
				itemRet.addAll(itemNotInDb);
				
			}
		
		if (null == itemRet)
			itemRet.addAll(processItem);
		
		if (0 == itemRet.size())
			itemRet.addAll(processItem);
		
		return itemRet;
	}
	
	protected void finishListProgress(String title, ArrayList<ExtendedHashMap> list, List<Item> processItem, Boolean bSort) {
		//
		List<Item> item = null;
		processItem.clear();
		item = new ArrayList<ChannelList.Item>();
		item.clear();
		
		for (ExtendedHashMap map:list) {
			//
			Item object = new Item();
			object.strChannelName = map.getString(Event.KEY_SERVICE_NAME);
			//object.strChannelName = ChangeChnName(map.getString(Event.KEY_SERVICE_NAME));
			object.strChnRef = map.getString(Event.KEY_SERVICE_REFERENCE);
			object.strProviderName = map.getString(Event.KEY_SERVICE_PROVIDER_NAME);
			if ("None".equals(map.getString(Event.KEY_EVENT_DURATION))) {
				object.nProgTime = 0;
			}
			else {
				try {
					object.nProgTime = Integer.valueOf(map.getString(Event.KEY_EVENT_DURATION), 10) / 60;
				} catch (NumberFormatException e) {
					object.nProgTime = 0;
				}
				
			}
			item.add(object);
		}
		
		if (true == bSort) {
			processItem.addAll(sortChnItems(item));
		}
		else {
			processItem.addAll(item);
		}
		
		
		((BaseAdapter)this).notifyDataSetChanged();
	}
	
	
	protected void finishListProgress(ArrayList<ExtendedHashMap> list, List<Item> processItem, Boolean bSort) {
		//
		processItem.clear();
		List<Item> item = null;
		item = new ArrayList<ChannelList.Item>();
		item.clear();
		
		for (ExtendedHashMap map:list) {
			Item object = new Item();
			object.strChannelName = map.getString(Event.KEY_SERVICE_NAME);
			//object.strChannelName = ChangeChnName(map.getString(Event.KEY_SERVICE_NAME));
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
					try {
						object.nProgTime = Integer.valueOf(map.getString(Event.KEY_EVENT_DURATION), 10) / 60;
					} catch(NumberFormatException e) {
						object.nProgTime = 0;
					}
					
				}
				
			//object.strNextProgName = map.getString(Event.KEY_NEXT_EVENT_TITLE);
			
			item.add(object);
		}
		
		if (true == bSort) {
			processItem.addAll(sortChnItems(item));
		}
		else {
			processItem.addAll(item);
		}
		
		Map<String, String> map = new HashMap<String, String>();
		map.put("ChnNumber", String.format("%d", processItem.size()));
		FlurryAgent.logEvent("ChnNumber", map);
		
		((BaseAdapter)this).notifyDataSetChanged();
	}
	
	//取当前频道列表，并显示的工作线程
	private class GetServiceListTask extends
		AsyncTask<ArrayList<NameValuePair>, String, Boolean> {
	
		//private DreamSaxParser saxParser;
		protected ArrayList<ExtendedHashMap> mTaskList;
	
		private DreamSaxParser m_ChnListSaxParser;
		//private DreamSaxParser m_CurtChnSamParser;
	
		public GetServiceListTask() {
			super();
			E2ServiceListHandler handle = new E2ServiceListHandler();
			//E2EpgNowNextListHandler handle = new E2EpgNowNextListHandler();
			mTaskList = new ArrayList<ExtendedHashMap>();
			handle.setList(mTaskList);
			m_ChnListSaxParser = new DreamSaxParser(handle);
			//m_CurtChnSamParser = new DreamSaxParser(handle);
		}
	
		@Override
		protected Boolean doInBackground(ArrayList<NameValuePair>... params) {
			// TODO Auto-generated method stub
		
			publishProgress("publishProgress");
		
			//String dd = GetCurChannel();
			//saxParser.parse(dd);
		
			// get current channel
			//m_ChnListSaxParser.parse(GetCurChannel());
		
			String xml = null;
			if (m_httpClient.fetchPageContent(URIStore.EPG_NOW, params[0])) {
				xml = m_httpClient.getPageContentString();
			}
			//if (m_httpClient.fetchPageContent(URIStore.SERVICES, params[0])) {
				//xml = m_httpClient.getPageContentString();
			//}
			
			if (null != xml && !isCancelled()) {
				return m_ChnListSaxParser.parse(xml);
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			//finishListProgress("main", mTaskList, mItems);
			
			finishListProgress(mTaskList, mItems, m_bSortChnnel);
			super.onPostExecute(result);
		}

		@Override
		protected void onProgressUpdate(String... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
		}
	
	}

	
	//切换频道线程
	private class ChnChannelTask extends
	AsyncTask<ArrayList<NameValuePair>, String, Boolean> {
		
		String m_ChnResult = "";
		
		@Override
		protected Boolean doInBackground(ArrayList<NameValuePair>... params) {
			if (null == m_httpClient)
				return true;
			m_httpClient.fetchPageContent(URIStore.ZAP, params[0]);
			m_ChnResult = m_httpClient.getPageContentString();
			return true;
		}
		
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
			finishListProgress(mTaskList, mItems, m_bSortChnnel);
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
