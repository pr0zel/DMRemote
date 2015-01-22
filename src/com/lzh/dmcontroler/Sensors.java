package com.lzh.dmcontroler;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.util.Log;

public class Sensors implements SensorEventListener {

	private static final String TAG = "LightSensorActivity";
	private SensorManager mSenMgr;
	private Sensor mSensor;
	private Context mContex;
	 
	public Sensors(Context contex) {
		//
		this.mContex = contex;
		
	}
	 
	
	public Boolean InitSensor () {
		//
		Boolean bRet = false;
		this.mSenMgr = (SensorManager) this.mContex.getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> lSenSor = this.mSenMgr.getSensorList(Sensor.TYPE_LIGHT);
		
		if (0 == lSenSor.size())
			return bRet;
		if (lSenSor.isEmpty())
			return bRet;
			
		this.mSensor = lSenSor.get(0);
		
		return bRet;
	}
	
	private SensorListener mySensorListener = new SensorListener() {

		@Override
		public void onAccuracyChanged(int sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSensorChanged(int sensor, float[] values) {
			// TODO Auto-generated method stub
			if (sensor == SensorManager.SENSOR_LIGHT) {
				Log.d(TAG, "aaaaaaa");
				
			}
		}
		
	};
	
	public Boolean BeginLog() {
		this.mSenMgr.registerListener(mySensorListener, SensorManager.SENSOR_LIGHT);
		return true;
	}
	
	public Boolean EndLog() {
		this.mSenMgr.unregisterListener(mySensorListener);
		return true;
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub

	}

}
