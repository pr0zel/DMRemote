/* © 2010 Stephan Reichholf <stephan at reichholf dot net>
 * 
 * Licensed under the Create-Commons Attribution-Noncommercial-Share Alike 3.0 Unported
 * http://creativecommons.org/licenses/by-nc-sa/3.0/
 */

package com.lzh.dmcontroler.E2Service;

import com.lzh.dmcontroler.helpers.ExtendedHashMap;
import com.lzh.dmcontroler.helpers.Event;

import org.xml.sax.Attributes;

/**
 * @author sreichholf
 * 
 */
public class E2EventListHandler extends E2ListHandler {

	protected static final String TAG_E2EVENT = "e2event";
	protected static final String TAG_E2EVENTID = "e2eventid";
	protected static final String TAG_E2EVENTSTART = "e2eventstart";
	protected static final String TAG_E2EVENTDURATION = "e2eventduration";
	protected static final String TAG_E2EVENTCURRENTTIME = "e2eventcurrenttime";
	protected static final String TAG_E2EVENTTITLE = "e2eventtitle";
	protected static final String TAG_E2EVENTDESCRIPTION = "e2eventdescription";
	protected static final String TAG_E2EVENTDESCRIPTIONEXTENDED = "e2eventdescriptionextended";
	protected static final String TAG_E2EVENTSERVICEREFERENCE = "e2eventservicereference";
	protected static final String TAG_E2EVENTSERVICENAME = "e2eventservicename";
	protected static final String TAG_E2EVENTPROVIDER = "e2eventprovidername";

	private boolean inEvent = false;
	private boolean inId = false;
	private boolean inStart = false;
	private boolean inDuration = false;
	private boolean inCurrentTime = false;
	private boolean inTitle = false;
	private boolean inDescription = false;
	private boolean inDescriptionEx = false;
	private boolean inServiceRef = false;
	private boolean inServiceName = false;
	private boolean inProviderName = false;

	private ExtendedHashMap mEvent;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
	 * java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String namespaceUri, String localName, String qName, Attributes attrs) {
		if (localName.equals(TAG_E2EVENT)) {
			inEvent = true;
			mEvent = new ExtendedHashMap();

		} else if (localName.equals(TAG_E2EVENTID)) {
			inId = true;
		} else if (localName.equals(TAG_E2EVENTSTART)) {
			inStart = true;
		} else if (localName.equals(TAG_E2EVENTDURATION)) {
			inDuration = true;
		} else if (localName.equals(TAG_E2EVENTCURRENTTIME)) {
			inCurrentTime = true;
		} else if (localName.equals(TAG_E2EVENTTITLE)) {
			inTitle = true;
		} else if (localName.equals(TAG_E2EVENTDESCRIPTION)) {
			inDescription = true;
		} else if (localName.equals(TAG_E2EVENTDESCRIPTIONEXTENDED)) {
			inDescriptionEx = true;
		} else if (localName.equals(TAG_E2EVENTSERVICEREFERENCE)) {
			inServiceRef = true;
		} else if (localName.equals(TAG_E2EVENTSERVICENAME)) {
			inServiceName = true;
		}else if (localName.equals(TAG_E2EVENTPROVIDER)) {
			inProviderName = true;
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
		if (localName.equals(TAG_E2EVENT)) {
			inEvent = false;
			Event.supplementReadables(mEvent);
			mList.add(mEvent);
		} else if (localName.equals(TAG_E2EVENTID)) {
			inId = false;
		} else if (localName.equals(TAG_E2EVENTSTART)) {
			inStart = false;
		} else if (localName.equals(TAG_E2EVENTDURATION)) {
			inDuration = false;
		} else if (localName.equals(TAG_E2EVENTCURRENTTIME)) {
			inCurrentTime = false;
		} else if (localName.equals(TAG_E2EVENTTITLE)) {
			inTitle = false;
		} else if (localName.equals(TAG_E2EVENTDESCRIPTION)) {
			inDescription = false;
		} else if (localName.equals(TAG_E2EVENTDESCRIPTIONEXTENDED)) {
			inDescriptionEx = false;
		} else if (localName.equals(TAG_E2EVENTSERVICEREFERENCE)) {
			inServiceRef = false;
		} else if (localName.equals(TAG_E2EVENTSERVICENAME)) {
			inServiceName = false;
		} else if (localName.equals(TAG_E2EVENTPROVIDER)) {
			inProviderName = false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	public void characters(char ch[], int start, int length) {
		String value = new String(ch, start, length);

		if (inEvent) {
			if (inId) {
				mEvent.putOrConcat(Event.KEY_EVENT_ID, value.trim());
			} else if (inStart) {
				mEvent.putOrConcat(Event.KEY_EVENT_START, value.trim());
			} else if (inDuration) {
				mEvent.putOrConcat(Event.KEY_EVENT_DURATION, value.trim());
			} else if (inCurrentTime) {
				mEvent.putOrConcat(Event.KEY_CURRENT_TIME, value.trim());
			} else if (inTitle) {
				mEvent.putOrConcat(Event.KEY_EVENT_TITLE, value.trim());
			} else if (inDescription) {
				mEvent.putOrConcat(Event.KEY_EVENT_DESCRIPTION, value.trim());
			} else if (inDescriptionEx) {
				mEvent.putOrConcat(Event.KEY_EVENT_DESCRIPTION_EXTENDED, value.trim());
			} else if (inServiceRef) {
				mEvent.putOrConcat(Event.KEY_SERVICE_REFERENCE, value.trim());
			} else if (inServiceName) {
				mEvent.putOrConcat(Event.KEY_SERVICE_NAME, value.replaceAll("\\p{Cntrl}", "").trim());
			} else if (inProviderName) {
				mEvent.putOrConcat(Event.KEY_SERVICE_PROVIDER_NAME, value.trim());
			}
		}
	}

}
