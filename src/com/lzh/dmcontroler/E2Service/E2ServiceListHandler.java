/* © 2010 Stephan Reichholf <stephan at reichholf dot net>
 * 
 * Licensed under the Create-Commons Attribution-Noncommercial-Share Alike 3.0 Unported
 * http://creativecommons.org/licenses/by-nc-sa/3.0/
 */

package com.lzh.dmcontroler.E2Service;

import org.xml.sax.Attributes;

import com.lzh.dmcontroler.helpers.ExtendedHashMap;
import com.lzh.dmcontroler.helpers.Service;

/**
 * @author sreichholf
 * 
 */
public class E2ServiceListHandler extends E2ListHandler {

	protected static final String TAG_E2SERVICENAME = "e2servicename";
	protected static final String TAG_E2SERVICEREFERENCE = "e2servicereference";
	protected static final String TAG_E2SERVICE = "e2service";

	private boolean inService;
	private boolean inReference;
	private boolean inName;

	private ExtendedHashMap mService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
	 * java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String namespaceUri, String localName, String qName, Attributes attrs) {
		if (localName.equals(TAG_E2SERVICE)) {
			inService = true;
			mService = new ExtendedHashMap();
		} else if (localName.equals(TAG_E2SERVICEREFERENCE)) {
			inReference = true;
		} else if (localName.equals(TAG_E2SERVICENAME)) {
			inName = true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(String namespaceURI, String localName, String qName) {
		if (localName.equals(TAG_E2SERVICE)) {
			inService = false;
			mList.add(mService);
		} else if (localName.equals(TAG_E2SERVICEREFERENCE)) {
			inReference = false;
		} else if (localName.equals(TAG_E2SERVICENAME)) {
			inName = false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char ch[], int start, int length) {
		String value = new String(ch, start, length);

		if (inService) {
			if (inReference) {
				mService.putOrConcat(Service.KEY_REFERENCE, value);
			} else if (inName) {
				mService.putOrConcat(Service.KEY_NAME, value.replaceAll("\\p{Cntrl}", ""));
			}
		}
	}
}
