package com.lzh.dmcontroler;

import java.sql.SQLClientInfoException;

import com.lzh.dmcontroler.helpers.DateTime;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


// table 1 for chncgnlog  tbl2 for chn sum

public class DmRemoteSql {
	SQLiteDatabase m_db = null;
	//String strDBName = "/data/data/com.lzh.dmremote/databases/DMRemoteDB.db";
	String m_strDBName;
	
	public DmRemoteSql(Context contex) {
		//m_db = SQLiteDatabase.openOrCreateDatabase("test.db", null);
		
		m_strDBName = "/data/data/" + contex.getPackageName() + "/DMRemoteDB.db";
		
		//Log.d("Tag", strDBName);
		initDB();
	}
	
	private Boolean openDB() {
		try {
			m_db = SQLiteDatabase.openOrCreateDatabase(m_strDBName, null);
		}
		catch(Exception e) {
			return false;
		}
		
		if (null == m_db)
			return false;
		else
			return true;
		
		
	}
	
	public Boolean initDB() {
		String strExecSQL = "";
		
		if (false == openDB())
			return false;
		
		String strExecSQL1 = "create table chnlog(_id INTEGER PRIMARY KEY AUTOINCREMENT, ChnName TEXT NOT NULL, ChnRef TEXT NOT NULL, ActTime DateTime)";
		String strExecSQL2 = "create table chnsum(_id INTEGER PRIMARY KEY AUTOINCREMENT, ChnName TEXT NOT NULL, ChnRef TEXT NOT NULL, sumNum INTEGER)";
		
		if (false == tabIsExist("chnlog")) {
			m_db.execSQL(strExecSQL1);
		}

		if (false == tabIsExist("chnsum")) {
			m_db.execSQL(strExecSQL2);
		}
		
		m_db.close();
		m_db = null;
		
		return true;
	}
	
	private Boolean tabIsExist(String tabName) {
		Boolean bRet = false;
		if (null == tabName)
			return false;
		
		Cursor cursor = null;
		
		try {
			String sql = "select count(*) as c from sqlite_master where type ='table' and name ='" + tabName.trim() + "' ";
			cursor = m_db.rawQuery(sql, null);
			if(cursor.moveToNext()){
				int count = cursor.getInt(0);
				 if(count > 0){
					 bRet = true;
				 }
			}
			
		} catch (Exception e) {
			//
		}
		
		return bRet;
	}
	
	public void AddReftoDB(String strChnName, String strChnRef) {
		int nChnTotal = 0;
		m_db = SQLiteDatabase.openDatabase(m_strDBName, null, SQLiteDatabase.OPEN_READWRITE);
		if (null == m_db)
			return;
		
		//DateTime dt = new DateTime();
		
		String strExecSQL1 = "";
		ContentValues values = new ContentValues();
		ContentValues values2 = new ContentValues();
		values.put("ChnName", strChnName);
		values.put("ChnRef", strChnRef);
		
		m_db.insert("chnlog", "_id", values);
		
		strExecSQL1 = "select * from chnsum where ChnRef = \"" + strChnRef +"\"";
		Cursor c = m_db.rawQuery(strExecSQL1, null);
		c.moveToFirst();
		
		if (-1 == nChnTotal || c.isAfterLast()) {
			// not exist
			values2.clear();
			values2.put("ChnName", strChnName);
			values2.put("ChnRef", strChnRef);
			values2.put("sumNum", 1);
			m_db.insert("chnsum", "_id", values2);
			m_db.close();
			return;
		}
		
		nChnTotal = c.getInt(c.getColumnIndex("sumNum"));
		nChnTotal ++;
		//values2.clear();
		values2.put("sumNum", nChnTotal);
		String[] args = {String.valueOf(strChnRef)};
		m_db.update("chnsum", values2, "ChnRef=?", args);	
		m_db.close();
		
	}
	
	public int getChnCountByName(String strName) {
		int nChnCount = 0;
		String strSQL = "";
		m_db = SQLiteDatabase.openDatabase(m_strDBName, null, SQLiteDatabase.OPEN_READONLY);
		
		strSQL = "select * from chnsum where ChnName = \"" + strName + "\"";
		Cursor c = m_db.rawQuery(strSQL, null);
		c.moveToFirst();
		nChnCount = c.getColumnIndex("sumNum");
		if (-1 == nChnCount) {
			nChnCount = 0;
		}
		
		m_db.close();
		return nChnCount;
	}
	
	public int getChnCountByRef(String strChnRef) {
		int nChnCount = 0;
		String strSQL = "";
		m_db = SQLiteDatabase.openDatabase(m_strDBName, null, SQLiteDatabase.OPEN_READONLY);
		
		strSQL = "select * from chnsum where ChnRef = \"" + strChnRef + "\"";
		Cursor c = m_db.rawQuery(strSQL, null);
		c.moveToFirst();
		nChnCount = c.getColumnIndex("sumNum");
		if (-1 == nChnCount) {
			nChnCount = 0;
		}
		
		m_db.close();
		return nChnCount;
	}
	
	public String[] getChnRefCountInDb(int nSort) {
		String[] strRet;
		String strSQL = "";
		int nColumnNum;
		int nColumnCount;
		m_db = SQLiteDatabase.openDatabase(m_strDBName, null, SQLiteDatabase.OPEN_READONLY);
		if (1 == nSort)
			strSQL = "select ChnRef from chnsum order by sumNum DESC";
		else if (2 == nSort)
			strSQL = "select ChnRef from chnsum order by sumNum";
		else
			strSQL = "select ChnRef from chnsum";
		
		
		Cursor c = m_db.rawQuery(strSQL, null);
		c.moveToFirst();
		nColumnNum = c.getCount();
		
		strRet = new String[nColumnNum];
		
		nColumnCount = 0;
		while(!c.isAfterLast()) {
			strRet[nColumnCount] = c.getString(0);
			
			c.moveToNext();
			nColumnCount ++;
		}
		
		
		return strRet;
	}
	
	
	
}
