package com.lzh.dmcontroler;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.util.InetAddressUtils;

import com.lzh.dmremote.R;
import com.lzh.dmremote.R.string;

import android.R.bool;
import android.R.integer;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class IpScanTask extends AsyncTask<Integer, Integer, Integer> {
	
	// 默认扫80端口
	private int m_nPort = 80;
	private Context m_Context;
	private String m_strLocalAddr;
	
	private int m_nProgress = 0;
	
	private ProgressDialog mProgressDialog;
	
	static private List<String> m_listIPAddr = null;
	
	private Handler mHandler;
	
	public IpScanTask (Context context) {
		m_Context = context;
		m_listIPAddr = new ArrayList<String>();
		m_listIPAddr.clear();
	}
	
	private synchronized void setFoundIP(String strIP) {
		m_listIPAddr.add(strIP);
	}
	
	public List<String> getDMIpList() {
		return m_listIPAddr;
	}
	
	private Handler m_MsgProcHdl = new Handler() {
		
		public void dispatchMessage(Message msg) {
			//
		}
	};
	
	private Boolean CheckDMByIP(String ip) {
		String strIPStrRet = "";
		
		strIPStrRet = sendHttpReq(ip);
		return CheckDMHead(strIPStrRet);
	}
	
	// 网络部分一定要放在线程里面实现
	public String sendHttpReq(String ip) {
		String res = "";
		String strUrlStr = "http://";
		HttpURLConnection urlconn = null;
		
		strUrlStr += ip;
		if (80 != m_nPort) {
			strUrlStr += ":" + m_nPort;
		}
		strUrlStr += "/web/about?";
		//strUrlStr = "http://pr0zel.vicp.net:39728/web/about?";
		try {
			URL url = new URL(strUrlStr);
			urlconn = (HttpURLConnection)url.openConnection();
			urlconn.setConnectTimeout(1000);
			urlconn.setReadTimeout(1000);
			
			InputStream is = urlconn.getInputStream();
			InputStreamReader in = new InputStreamReader(is);
			BufferedReader bf = new BufferedReader(in);
			
			String inputLine = null;
			while (((inputLine = bf.readLine()) != null))
			{
				res += inputLine +"\n";
			}
			
			//int k = res.length();
			//CheckDMHead(res);
			
			//res = StreamDeal(urlconn.getOutputStream());
		
		} catch (ConnectTimeoutException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		finally {
			if (null != urlconn)
				urlconn.disconnect();
		}
			
		return res;
	}
	
	private Boolean CheckDMHead(String strHttpString) {
		if (null == strHttpString)
			return false;
		
		if (100 > strHttpString.length())
			return false;
		
		if (false == strHttpString.substring(0, 6).equals("<?xml ")) {
			return false;
		}
		
		String ss = strHttpString.substring(39, 49);
		String ss2 = strHttpString.substring(51, 60);
		
		if (ss.equals("<e2abouts>") && ss2.equals("<e2about>"))
			return true;
		else
			return false;
	}
	
	public String sendMsg(String ip, String msg) {
		
		//向socketserver发信息
		String res = null;
		Socket socket = null;
		
		try {
			socket = new Socket(ip, m_nPort);
			PrintWriter outputStream = new PrintWriter(socket.getOutputStream());
			outputStream.println(msg);
			outputStream.flush();
			
			DataInputStream input = new DataInputStream(socket.getInputStream());
			res = input.readUTF();
			
			Message.obtain(m_MsgProcHdl, 222, res).sendToTarget();
		} catch (Exception unknownHost) {
			//
			unknownHost.printStackTrace();
			
		} finally {
			
			try {
				if (null != socket) {
					socket.close();
				}
			} catch (IOException ioException) {
				//
				ioException.printStackTrace();
			}
		}
		
		return res;
	}
	
	private class ScanThread implements Runnable
	{
		private int m_nIndex = 0;
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Boolean b = CheckDMByIP(IpScanTask.this.m_strLocalAddr + m_nIndex);
			if (true == b) {
				setFoundIP(IpScanTask.this.m_strLocalAddr + m_nIndex);
			}
			m_nProgress++;
			mProgressDialog.setProgress(m_nProgress);
		}
		
		public void setIndex(int nIndex) 
		{ 
			this.m_nIndex = nIndex; 
		}
	}
	
	public void Scan() {
		m_strLocalAddr = getLocAddrIndex();
		m_listIPAddr.clear();
		
		if (m_strLocalAddr.equals(""))
			return;
		
		m_nProgress = 0;
		
		ExecutorService service = Executors.newFixedThreadPool(255);
		
		for (int i = 2; i < 255; i++) {
            
            ScanThread scan = new ScanThread();
            scan.setIndex(i);
            
            service.execute(scan);
		}
		
		service.shutdown();
		
        try {
			service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        if (m_listIPAddr.isEmpty())
        {
        	Message msg = mHandler.obtainMessage(0, null);
        	
        	mHandler.sendMessage(msg);
        }
        else
        {
        	Message msg = mHandler.obtainMessage(1, m_listIPAddr);
        	
        	mHandler.sendMessage(msg);
        }
	}
	
	private String getLocAddress() {
		
		String strIPAddr = "";
		
		try
		{
			Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
			
			while (en.hasMoreElements()) {
				NetworkInterface networks = en.nextElement();
				Enumeration<InetAddress> address = networks.getInetAddresses();
				
				while (address.hasMoreElements()) {
					InetAddress ip = address.nextElement();
					if (!ip.isLoopbackAddress() 
							&& InetAddressUtils.isIPv4Address(ip.getHostAddress())) {
								//
						strIPAddr = ip.getHostAddress();
					}
				}
			}
			
		} catch(SocketException e) {
			//
		}
		
		return strIPAddr;
	}

	private String getLocAddrIndex() {
		String strIP = getLocAddress();
		
		if(!strIP.equals("")) {
			return strIP.substring(0, strIP.lastIndexOf(".") + 1);
		}
		
		return null;
	}

	@Override
	protected Integer doInBackground(Integer... arg0) {
		// TODO Auto-generated method stub
		Scan();
		return null;
	}
	
	@Override
	protected void onPreExecute()
	{
		mProgressDialog = new ProgressDialog(m_Context);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		//scanip_popupwin_title
		mProgressDialog.setTitle(m_Context.getResources().getString(R.string.scanip_popupwin_title));
		mProgressDialog.setIcon(R.drawable.ic_launcher);
		mProgressDialog.setProgress(254);
		mProgressDialog.setMax(254);
		mProgressDialog.setIndeterminate(false);
		mProgressDialog.setCancelable(true);
		mProgressDialog.show();
	}
	
	@Override
	protected void onPostExecute(Integer integer)
	{
		mProgressDialog.dismiss();
	}
	
	public void start(Handler handler)
	{
		mHandler = handler;
		
		Integer[] arrayOfInteger = new Integer[0];
		execute(arrayOfInteger);
	}
}
