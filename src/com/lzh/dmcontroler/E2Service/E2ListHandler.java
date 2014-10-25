/* © 2010 Stephan Reichholf <stephan at reichholf dot net>
 * 
 * Licensed under the Create-Commons Attribution-Noncommercial-Share Alike 3.0 Unported
 * http://creativecommons.org/licenses/by-nc-sa/3.0/
 */

package com.lzh.dmcontroler.E2Service;

import java.util.ArrayList;

import org.xml.sax.helpers.DefaultHandler;

import com.lzh.dmcontroler.helpers.ExtendedHashMap;

/**
 * @author sre
 *
 */
public abstract class E2ListHandler extends DefaultHandler {
	protected ArrayList<ExtendedHashMap> mList;
	
	public E2ListHandler(){
		mList = null;
	}
	
	public void setList(ArrayList<ExtendedHashMap> list){
		mList = list;
	}
}