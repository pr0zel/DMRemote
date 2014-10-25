package com.lzh.dmcontroler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import com.lzh.dmcontroler.helpers.Event;
import com.lzh.dmcontroler.E2Service.E2ListHandler;
import com.lzh.dmcontroler.E2Service.E2ServiceListHandler;
import com.lzh.dmcontroler.helpers.DreamSaxParser;
import com.lzh.dmcontroler.helpers.ExtendedHashMap;
import com.lzh.dmcontroler.helpers.SimpleHttpClient;
import com.lzh.dmcontroler.helpers.Profile;
import com.lzh.dmcontroler.helpers.URIStore;
import com.lzh.dmremote.R;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;


public class chngrid extends BaseAdapter {

	public static class Item{
		public String text;
		public String chnRefString;
		public int resId;
	}
	
	private SimpleHttpClient m_httpClient = null;

	private List<Item> mItems = new ArrayList<chngrid.Item>();
	private Context mContext;
	
	private List<Map<String, Object>> mServiceItems;
	
	private int m_ScreenWidth = 0;
	private int m_ScreenHight = 0;
	
	private String m_strIP = "";
	private int m_nPort = 0;
	
	
	public chngrid(Context context) {
		mContext = context;
		
	}
	
	public void SetScreen(int nWidth, int nHight) {
		m_ScreenHight = nHight;
		m_ScreenWidth = nWidth;
	}
	
	public void SetIPAndPortAndConnect(String strIP, int nPort) {
		
		if (0 == strIP.length() || 0 == nPort) {
			return;
		}
		
		m_strIP = strIP;
		m_nPort = nPort;
		Profile t_profile = new Profile ("Default", m_strIP, "", m_nPort, 8001, 80,
				false, "", "", false, false, false, false, false);
		
		mItems.clear();
		((BaseAdapter)this).notifyDataSetChanged();
		
		//if (null == m_httpClient) {
			m_httpClient = new SimpleHttpClient(t_profile);
			//String sss = "1:7:1:0:0:0:0:0:0:0:(type == 1) || (type == 17) || (type == 195) || (type == 25) ORDER BY provider";
			
			
			String sss = "1:7:1:0:0:0:0:0:0:0:FROM BOUQUET \"userbouquet.favourites.tv\" ORDER BY provider";
			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("sRef", sss));
			
			(new GetServiceListTask()).execute(params);
			
		//}
		
		
		
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
	
	public void ChangeChannel(int nCHNID, Boolean bIsChange) {
		if (false == bIsChange)
			return;
		
		String ss = mItems.get(nCHNID).chnRefString;
		String strChannelName = mItems.get(nCHNID).text;
		
		String strChnNameDisplay = mContext.getResources().getString(R.string.chn_lst_active_chn_toase) + strChannelName;
		
		Toast.makeText(mContext, strChnNameDisplay, 1000).show();
		
		//Log.d("CHANNEL_CHANGE", ss);
		//m_httpClient.fetchPageContent(URIStore.ZAP, mItems.get(nCHNID));
		//m_httpClient.fetchPageContent(URIStore.ZAP + mItems.get(nCHNID).chnRefString);
		
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("sRef", mItems.get(nCHNID).chnRefString));
		(new ChnChannelTask()).execute(params);
		
		//if (m_httpClient.fetchPageContent(URIStore.ZAP, params))
		//{
			//xml = m_httpClient.getPageContentString();
		//}
		//Log.d("AAAA", xml);
		
	}
	
	protected void finishChangeChannel(String strRetString)
	{
		/*
		E2ServiceListHandler handle1 = new E2ServiceListHandler();
		ArrayList<ExtendedHashMap> kTaskList;
		kTaskList = new ArrayList<ExtendedHashMap>();
		handle1.setList(kTaskList);
		DreamSaxParser ChnChangeResult = new DreamSaxParser(handle1);
		
		ChnChangeResult.parse(strRetString);
		Toast.makeText(mContext, strRetString, 3000).show();
		*/
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		
		return mItems.size();
		//return 0;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (null == convertView) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.item, null);
		}
		//ImageView image = (ImageView) convertView.findViewById(R.id.item_icon);
		TextView text = (TextView) convertView.findViewById(R.id.item_text);
		Item item = (Item) getItem(position);
		//image.setImageResource(item.resId);
		//item_icon
		
		int nHight = (m_ScreenWidth - 50) / 2;
		int nBigTextSize = (int) (nHight / (float)3.5);
		int nSmallTextSize = nBigTextSize / 2;
		
		convertView.setMinimumHeight(nHight);
		convertView.setMinimumWidth(nHight);
		//convertView.setBackgroundColor(Color.BLUE);
		
		
		text.setWidth(nHight);
		text.setHeight(nHight);
		
		//text.setText(item.text);
		//text.setAlpha(0f);
		
		final SpannableStringBuilder spaString_disp = new SpannableStringBuilder(item.text);
		//spaString_disp.setSpan(new AbsoluteSizeSpan(100), 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		//spaString_disp.setSpan(new AbsoluteSizeSpan(50), 1, item.text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		spaString_disp.setSpan(new AbsoluteSizeSpan(nBigTextSize), 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		spaString_disp.setSpan(new AbsoluteSizeSpan(nSmallTextSize), 1, item.text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		text.setText(spaString_disp);
		text.setBackgroundColor(Color.LTGRAY);
		text.setTextColor(Color.BLUE);
		
		//Log.d("ChnGrid - getView", item.text);
		
		
		
		//image.setBackgroundColor(Color.GRAY);
		//image.setAlpha(0f);
		
		return convertView;
	}
	
	protected Boolean isNetChannel(String strCHNURL) {
		/*
		if (0 < strCHNURL.indexOf(":10000:0:0:0:"))
			return true;
		else
			return false;
		*/
		return false;
		
	}
	
	
	protected void finishListProgress(String title, ArrayList<ExtendedHashMap> list) {
		mItems.clear();
		
		for (ExtendedHashMap map :list) {
			
			StringBuilder strBuilder = new StringBuilder("c");
			strBuilder.append(map.getString(Event.KEY_SERVICE_REFERENCE));
			
			//检查是否是网络频道
			if (true == isNetChannel(strBuilder.toString()))
				continue;
			
			//String strRef = (strBuilder.toString().replaceAll(":", "_")).toLowerCase();
			
			Item object = new Item();
			object.text = map.getString(Event.KEY_SERVICE_NAME);
			object.chnRefString = map.getString(Event.KEY_SERVICE_REFERENCE);
			//object.resId = R.drawable.defenedicon;
			mItems.add(object);
			
		}
		
		((BaseAdapter)this).notifyDataSetChanged();
	}
	
	
	
	private class GetServiceListTask extends
		AsyncTask<ArrayList<NameValuePair>, String, Boolean> {
		
		//private DreamSaxParser saxParser;
		protected ArrayList<ExtendedHashMap> mTaskList;
		
		private DreamSaxParser m_ChnListSaxParser;
		//private DreamSaxParser m_CurtChnSamParser;
		
		public GetServiceListTask() {
			super();
			E2ServiceListHandler handle = new E2ServiceListHandler();
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
			if (m_httpClient.fetchPageContent(URIStore.SERVICES, params[0])) {
				xml = m_httpClient.getPageContentString();
			}
			
			
			
			if (null != xml && !isCancelled()) {
				return m_ChnListSaxParser.parse(xml);
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			finishListProgress("main", mTaskList);
			super.onPostExecute(result);
		}
		
		

		
	}
	
	//change channel Thread
	private class ChnChannelTask extends
	AsyncTask<ArrayList<NameValuePair>, String, Boolean> {
		
		String m_ChnResult = "";
		
		@Override
		protected Boolean doInBackground(ArrayList<NameValuePair>... params) {
			//return null;
			m_httpClient.fetchPageContent(URIStore.ZAP, params[0]);
			m_ChnResult = m_httpClient.getPageContentString();
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			finishChangeChannel(m_ChnResult);
			super.onPostExecute(result);
		}
		
	}
	
}
