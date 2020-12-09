package com.cidana.cidanadtvsample;

/**
 * @author Wyvern
 * @since 2015/5/21
 */

import com.cidana.cipl.CSplitBlob;

/**
 * class of CiplEvent
 */
public class CiplEvent {
    private String[] mEventArray;
    private CSplitBlob mBlob;
    private char mEscCh;

    /**
     * create a CiplEvent
     * @param eventArray array of event information
     * @param escCh escape character
     */
    public CiplEvent(String[] eventArray, char escCh) {
        mEventArray = eventArray;
        mEscCh = escCh;
        mBlob = new CSplitBlob(eventArray[1], escCh);
    }

    /**
     * get session name of CiplEvent
     * @return string of event session
     */
    public String getSessionName() {
        return mEventArray[0];
    }

    /**
     * get event name of CiplEvent
     * @return string of event name
     */
    public String getEventName() {
        if (mBlob.getNumberCols(0) > 1) {
            return mBlob.getItem(0, 0);
        }
        else {
            return "";
        }
    }

    /**
     * get event information in string array
     * @return string array of event
     */
    public String[] getEventArray() {
        return mEventArray;
    }

    /**
     * get string blob by index
     * @param idx index of event information
     * @return a string blob of event information
     */
    public CSplitBlob getBlob(int idx) {
        if (idx < 0 || idx >= mEventArray.length) {
            return null;
        }
        return new CSplitBlob(mEventArray[idx], mEscCh);
    }

    /**
     * compare two event information
     * @param otherEvt event object to compare
     * @return true--same event, false--different
     */
    public boolean compare(CiplEvent otherEvt)
    {
        if (mEventArray.length != otherEvt.mEventArray.length)
            return false;
        for (int i = 0; i < mEventArray.length; i ++)
        {
            if (!mEventArray[i].equals(otherEvt.mEventArray[i]))
                return false;
        }
        return true;
    }
}
