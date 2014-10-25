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
	String strDBName;
	
	public DmRemoteSql(Context cOntex) {
		//m_db = SQLiteDatabase.openOrCreateDatabase("test.db", null);
		
		strDBName = "/data/data/" + cOntex.getPackageName() + "/DMRemoteDB.db";
		
		//Log.d("Tag", strDBName);
		initDB();
	}
	
	private Boolean openDB() {
		try {
			m_db = SQLiteDatabase.openOrCreateDatabase(strDBName, null);
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
		m_db = SQLiteDatabase.openDatabase(strDBName, null, SQLiteDatabase.OPEN_READWRITE);
		if (null == m_db)
			return;
		
		DateTime dt = new DateTime();
		
		String strExecSQL1 = "";
		ContentValues values = new ContentValues();
		values.put("ChnName", strChnName);
		values.put("ChnRef", strChnRef);
		
		m_db.insert("chnlog", "_id", values);
		
		strExecSQL1 = "select * from chnsum where ChnRef = \"" + strChnRef +"\"";
		Cursor c = m_db.rawQuery(strExecSQL1, null);
		c.moveToFirst();
		nChnTotal = c.getColumnIndex("sumNum");
		
		if (-1 == nChnTotal) {
			// not exist
			values.clear();
			values.put("ChnName", strChnName);
			values.put("ChnRef", strChnRef);
			values.put("sumNum", 1);
			m_db.insert("chnsum", "_id", values);
		}
		else {
			// sumNum ++
			
			nChnTotal ++;
			values.clear();
			values.put("sumNum", nChnTotal);
			m_db.update("chnsum", values, "ChnRef=?", new String[]{strChnRef});
			
		}
		
		
		m_db.close();
		
	}
	
	public int getChnCountByName(String strName) {
		int nChnCount = 0;
		String strSQL = "";
		m_db = SQLiteDatabase.openDatabase(strDBName, null, SQLiteDatabase.OPEN_READONLY);
		
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
		m_db = SQLiteDatabase.openDatabase(strDBName, null, SQLiteDatabase.OPEN_READONLY);
		
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
	
	public String[] getChnRefCountInDb() {
		String[] strRet = null;
		String strSQL = "";
		int nColumnNum;
		int nColumnCount;
		m_db = SQLiteDatabase.openDatabase(strDBName, null, SQLiteDatabase.OPEN_READONLY);
		strSQL = "select ChnRef from chnsum order by sumNum DESC";
		Cursor c = m_db.rawQuery(strSQL, null);
		c.moveToFirst();
		nColumnNum = c.getColumnCount();
		
		strRet = new String[nColumnNum];
		
		nColumnCount = 0;
		while(!c.isAfterLast()) {
			strRet[nColumnCount] = c.getColumnName(nColumnCount);
			
			c.moveToNext();
			nColumnCount ++;
		}
		
		
		return strRet;
	}
	
	
	
}
