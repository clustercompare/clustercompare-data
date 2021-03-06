/*
 * TV-Browser
 * Copyright (C) 04-2003 Martin Oberhauser (martin_oat@yahoo.de)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * CVS information:
 *  $RCSfile$
 *   $Source$
 *     $Date: 2010-06-28 19:33:48 +0200 (Mon, 28 Jun 2010) $
 *   $Author: bananeweizen $
 * $Revision: 6662 $
 */

package tvdataservice;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import tvbrowser.core.TvDataBase;
import tvbrowser.core.plugin.PluginProxy;
import tvbrowser.core.plugin.PluginProxyManager;
import util.io.IOUtilities;
import util.misc.HashCodeUtilities;
import util.misc.StringPool;
import util.program.ProgramUtilities;
import devplugin.Channel;
import devplugin.Date;
import devplugin.Marker;
import devplugin.Plugin;
import devplugin.PluginAccess;
import devplugin.Program;
import devplugin.ProgramFieldType;

/**
 * One program. Consists of the Channel, the time, the title and some extra
 * information.
 *
 * @author Til Schneider, www.murfman.de
 */
public class MutableProgram implements Program {

  private static final Logger mLog
    = Logger.getLogger(MutableProgram.class.getName());

  private static TimeZone mLocalTimeZone = TimeZone.getDefault();

  /**
   * The maximum length of a short info. Used for generating a short info out of a
   * (long) description.
   */
  public static final int MAX_SHORT_INFO_LENGTH = 200;

  /** A plugin array that can be shared by all the programs that are not marked
   * by any plugin. */
  protected static final Marker[] EMPTY_MARKER_ARR = new Marker[0];

  /** Contains all listeners that listen for events from this program. */
  private Vector<ChangeListener> mListenerList;

  /** Contains all Plugins that mark this program. We use a simple array,
   * because it takes less memory. */
  private Marker[] mMarkerArr;

  /** Tracks if the program is current loading/ being created. */
  private boolean mIsLoading;

  /** The cached ID of this program. */
  private String mId;

  /** The cached unique ID of this program. */
  private String mUniqueId;

  /** The date format, which is used in the unique ID */
  public static final String ID_DATE_FORMAT = "yyyy-MM-dd";

  /** The channel object of this program. */
  private Channel mChannel;

  /** The date of the program in the channel's time zone. */
  private Date mLocalDate;

  /** The normalized date of this program. (in the client's time zone) */
  private Date mNormalizedDate;

  /** The normalized start time of the program. (in the client's time zone) */
  private short mNormalizedStartTime;

  /** The state of this program */
  private byte mState;

  /** Contains the title */
  protected String mTitle;

  /** Contains the current mark priority of this program */
  private byte mMarkPriority = Program.NO_MARK_PRIORITY;

  /**
   * storage for INT and TIME fields
   */
  private int[] mIntValues;

  /**
   * storage for TEXT and BINARY fields
   */
  private Object[] mObjectValues;

  /**
   * Creates a new instance of MutableProgram.
   * <p>
   * The parameters channel, date, hours and minutes build the ID. That's why
   * they are not mutable.
   *
   * @param channel
   *          The channel object of this program.
   * @param localDate
   *          The date of this program.
   * @param localHours
   *          The hour-component of the start time of the program.
   * @param localMinutes
   *          The minute-component of the start time of the program.
   * @param isLoading
   *          If the program is currently being created.
   *
   * @see #setProgramLoadingIsComplete()
   */
  public MutableProgram(Channel channel, devplugin.Date localDate,
    int localHours, int localMinutes, boolean isLoading)
  {
    this (channel, localDate, isLoading);

    int localStartTime = localHours * 60 + localMinutes;
    setTimeField(ProgramFieldType.START_TIME_TYPE, localStartTime);
  }

  /**
   * Creates a new instance of MutableProgram.
   * <p>
   * The parameters channel, date, hours and minutes build the ID. That's why they
   * are not mutable.
   *
   * @param channel The channel object of this program.
   * @param localDate The date of this program.
   * @param isLoading If the program is currently loading.
   * @see #setProgramLoadingIsComplete()
   */
  public MutableProgram(final Channel channel, final devplugin.Date localDate, final boolean isLoading) {
    if (channel == null) {
      throw new NullPointerException("channel is null");
    }
    if (localDate == null) {
      throw new NullPointerException("localDate is null");
    }

    mIntValues = new int[ProgramFieldType.getIntFieldCount()];
    Arrays.fill(mIntValues, -1);
    mObjectValues = new Object[ProgramFieldType.getObjectFieldCount()];
    mListenerList = null; // defer initialization until needed, to save memory
    mMarkerArr = EMPTY_MARKER_ARR;
    mIsLoading = isLoading;

    mTitle = null;

    // These attributes are not mutable, because they build the ID.
    mChannel = channel;
    mLocalDate = localDate;

    // The title is not-null.
    setTextField(ProgramFieldType.TITLE_TYPE, "");

    mMarkerArr = EMPTY_MARKER_ARR;
    mState = IS_VALID_STATE;
  }


  private void normalizeTimeZone(final Date localDate, final int localStartTime) {
    TimeZone channelTimeZone=mChannel.getTimeZone();

    int timeZoneOffset=(mLocalTimeZone.getRawOffset()-channelTimeZone.getRawOffset())/ 60000 + mChannel.getTimeZoneCorrectionMinutes();

    mNormalizedStartTime = (short) (localStartTime + timeZoneOffset);
    mNormalizedDate=localDate;

    if (mNormalizedStartTime >= (24 * 60)) {
      mNormalizedStartTime -= (24 * 60);
      mNormalizedDate = mNormalizedDate.addDays(1);
    }
    else if (mNormalizedStartTime < 0) {
      mNormalizedStartTime += (24 * 60);
      mNormalizedDate = mNormalizedDate.addDays(-1);
    }
  }


  /**
   * Adds a ChangeListener to the program.
   *
   * @param listener the ChangeListener to add
   * @see #fireStateChanged
   * @see #removeChangeListener
   */
  public void addChangeListener(ChangeListener listener) {
    if (mListenerList == null) {
      mListenerList = new Vector<ChangeListener>(1);
    }
    if (!mListenerList.contains(listener)) {
      mListenerList.add(listener);
    }
  }



  /**
   * Removes a ChangeListener from the program.
   *
   * @param listener the ChangeListener to remove
   * @see #fireStateChanged
   * @see #addChangeListener
   */
  public void removeChangeListener(ChangeListener listener) {
    if (mListenerList == null) {
      return;
    }
    mListenerList.remove(listener);
  }



  /**
   * Send a ChangeEvent, whose source is this program, to each listener.
   *
   * @see #addChangeListener
   * @see EventListenerList
   */
  protected void fireStateChanged() {
    if (mListenerList == null) {
      return;
    }
    ChangeEvent changeEvent = new ChangeEvent(this);

    for (int i = 0; i < mListenerList.size(); i++) {
      mListenerList.get(i).stateChanged(changeEvent);
    }
  }


  public final String getTimeString() {
    return IOUtilities.timeToString(getStartTime());
  }

  public final String getEndTimeString() {
	return IOUtilities.timeToString(getStartTime() + getLength());
  }


  public final String getDateString() {
    Date d = getDate();
    if (d == null) {
      mLog.info(mChannel.getName() + " at " + getHours() + ":" + getMinutes()
        + ", NO DATE : '" + getTitle() + "'");
      return "";
    }
    return d.toString();
  }


  /**
   * Gets whether this program is marked as "on air".
   */
  public boolean isOnAir() {
    return ProgramUtilities.isOnAir(this);
  }

  /**
   * Marks the program for a Java plugin.
   *
   * @param javaPlugin The plugin to mark the program for.
   */
  public final void mark(Plugin javaPlugin) {
    PluginAccess plugin = PluginProxyManager.getInstance().getPluginForId(javaPlugin.getId());
    mark(plugin);
  }


  /**
   * Removes the marks from the program for a Java plugin.
   * <p>
   * If the program wasn't marked for the plugin, nothing happens.
   *
   * @param javaPlugin The plugin to remove the mark for.
   */
  public final void unmark(Plugin javaPlugin) {
    PluginAccess plugin = PluginProxyManager.getInstance().getPluginForId(javaPlugin.getId());
    unmark(plugin);
  }


  /**
   * Marks the program for a plugin.
   *
   * @param marker The plugin to mark the program for.
   */
  public final synchronized void mark(Marker marker) {
    if(mState == Program.IS_VALID_STATE) {
      boolean alreadyMarked = getMarkedByPluginIndex(marker) != -1;
      int oldCount = mMarkerArr.length;

      if (! alreadyMarked) {
        // Append the new plugin
        Marker[] newArr = new Marker[oldCount + 1];
        System.arraycopy(mMarkerArr, 0, newArr, 0, oldCount);
        newArr[oldCount] = marker;
        mMarkerArr = newArr;

        Arrays.sort(mMarkerArr,new Comparator<Marker>() {
          public int compare(Marker o1, Marker o2) {
            return o1.getId().compareTo(o2.getId());
          }
        });

        mMarkPriority = (byte) Math.max(mMarkPriority,marker.getMarkPriorityForProgram(this));

        // add program to artificial plugin tree
        if (marker instanceof PluginProxy) {
          PluginProxy proxy = (PluginProxy) marker;
          if (! proxy.canUseProgramTree() || proxy.hasArtificialPluginTree() ) {
            if (proxy.getArtificialRootNode() == null || proxy.getArtificialRootNode().size() < 100) {
              proxy.addToArtificialPluginTree(this);
            }
          }
        }

        fireStateChanged();
      }

      if(oldCount < 1) {
        MarkedProgramsList.getInstance().addProgram(this);
      }
    }
    else if(mState == Program.WAS_UPDATED_STATE) {
      Program p = Plugin.getPluginManager().getProgram(getDate(), getID());

      if(p != null) {
        p.mark(marker);
      }
    }
  }

  /**
   * Removes the marks from the program for a plugin.
   * <p>
   * If the program wasn't marked for the plugin, nothing happens.
   *
   * @param marker The plugin to remove the mark for.
   */
  public final synchronized void unmark(Marker marker) {
    if(mState == Program.IS_VALID_STATE) {
      int idx = getMarkedByPluginIndex(marker);
      if (idx != -1) {
        if (mMarkerArr.length == 1) {
          // This was the only plugin
          mMarkerArr = EMPTY_MARKER_ARR;
          mMarkPriority = Program.NO_MARK_PRIORITY;
        }
        else {
          int oldCount = mMarkerArr.length;
          Marker[] newArr = new Marker[oldCount - 1];
          System.arraycopy(mMarkerArr, 0, newArr, 0, idx);
          System.arraycopy(mMarkerArr, idx + 1, newArr, idx, oldCount - idx - 1);

          mMarkPriority = Program.NO_MARK_PRIORITY;

          for(Marker mark : newArr) {
            mMarkPriority = (byte) Math.max(mMarkPriority,mark.getMarkPriorityForProgram(this));
          }

          mMarkerArr = newArr;
        }

        // remove from artificial plugin tree
        if (marker instanceof PluginProxy) {
          PluginProxy proxy = (PluginProxy) marker;
          if (proxy.hasArtificialPluginTree() && proxy.getArtificialRootNode().size() < 100) {
            proxy.getArtificialRootNode().removeProgram(this);
          }
        }

        fireStateChanged();
      }

      if(mMarkerArr.length < 1) {
        MarkedProgramsList.getInstance().removeProgram(this);
      }
    }
    else if(mState == Program.WAS_UPDATED_STATE) {
      Program p = Plugin.getPluginManager().getProgram(getDate(), getID());

      if(p != null) {
        p.unmark(marker);
      }
    }
  }



  private int getMarkedByPluginIndex(Marker plugin) {
    for (int i = 0; i < mMarkerArr.length; i++) {
      if (mMarkerArr[i].getId().compareTo(plugin.getId()) == 0) {
        return i;
      }
    }

    return -1;
  }


  public Marker[] getMarkerArr() {
    return mMarkerArr;
  }



  /**
   * Gets whether this program is expired.
   */
  public boolean isExpired() {
    devplugin.Date today = Date.getCurrentDate();

    int comp = today.compareTo(getDate());
    if (comp < 0) {
      return false;
    }
    if (comp > 0) {
      return ! isOnAir();
    }

    // This program is (or was) today -> We've got to check the time
    int currentMinutesAfterMidnight = IOUtilities.getMinutesAfterMidnight();
    int programMinutesAfterMidnight = getStartTime() + getLength() - 1;
    return (programMinutesAfterMidnight < currentMinutesAfterMidnight);

  }


  /**
   * Gets the ID of this program. This ID is unique for a certain date.
   *
   * @return The ID of this program.
   */
  public synchronized String getID() {
    if (mId == null) {
      if  (mChannel.getDataServiceProxy() != null) {
        String dataServiceId = mChannel.getDataServiceProxy().getId();
        String groupId = mChannel.getGroup().getId();
        String channelId = mChannel.getId();
        String country = mChannel.getCountry();

        mId = (new StringBuilder(dataServiceId).append('_').append(groupId)
            .append('_').append(country).append('_').append(channelId).append(
                '_').append(getHours()).append(':').append(getMinutes())
            .append(':').append(TimeZone.getDefault().getRawOffset() / 60000))
            .toString();
      }
    }
    return mId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized String getUniqueID() {
    if (mUniqueId == null) {
      if  (mChannel.getDataServiceProxy() != null) {
        String dataServiceId = mChannel.getDataServiceProxy().getId();
        String groupId = mChannel.getGroup().getId();
        String channelId = mChannel.getId();
        String country = mChannel.getCountry();
        String date = new SimpleDateFormat(ID_DATE_FORMAT).format(getDate().getCalendar().getTime());

        mUniqueId = (new StringBuilder(dataServiceId).append('_').append(
            groupId).append('_').append(country).append('_').append(channelId)
            .append('_').append(date).append('_').append(getHours())
            .append(':').append(getMinutes()).append(':').append(TimeZone
            .getDefault().getRawOffset() / 60000)).toString();
      }
    }
    return mUniqueId;
  }


  // FieldHash
  public byte[] getBinaryField(ProgramFieldType type) {
    checkFormat(type, ProgramFieldType.BINARY_FORMAT);
    return (byte[]) getObjectValueField(type);
  }


  public String getTextField(ProgramFieldType type) {
    checkFormat(type, ProgramFieldType.TEXT_FORMAT);
    if(type == ProgramFieldType.TITLE_TYPE && mTitle != null && mTitle.trim().length() > 0) {
      return mTitle;
    }

    String value = (String) getObjectValueField(type);

    if (type == ProgramFieldType.SHORT_DESCRIPTION_TYPE) {
      value = validateShortInfo(value);
    }

    return value;
  }


  /**
   * access method to object field values. this allows ondemand programs to reimplement the access
   * @param type
   * @return
   */
  protected Object getObjectValueField(final ProgramFieldType type) {
    return mObjectValues[type.getStorageIndex()];
  }

  public int getIntField(final ProgramFieldType type) {
    checkFormat(type, ProgramFieldType.INT_FORMAT);
    return mIntValues[type.getStorageIndex()];
  }



  /**
   * Gets the value of a int field as String.
   *
   * @param type The type of the wanted field. Must have a int format.
   * @return The value of the field as String or <code>null</code>, if there is
   *         no value for this field.
   */
  public String getIntFieldAsString(final ProgramFieldType type) {
    int value = getIntField(type);
    if (value == -1) {
      return null;
    } else {
      return Integer.toString(value);
    }
  }


  public int getTimeField(final ProgramFieldType type) {
    checkFormat(type, ProgramFieldType.TIME_FORMAT);
    return mIntValues[type.getStorageIndex()];
  }


  /**
   * Gets the value of a time field as String of the pattern "h:mm".
   *
   * @param type The type of the wanted field. Must have a time format.
   * @return The value of the field as String or <code>null</code>, if there is
   *         no value for this field.
   */
  public String getTimeFieldAsString(final ProgramFieldType type) {
    int value = getTimeField(type);
    if (value == -1) {
      return null;
    } else {

      // Correct the TimeZone
      TimeZone channelTimeZone=mChannel.getTimeZone();
      TimeZone localTimeZone=TimeZone.getDefault();

      int timeZoneOffsetMinutes=(localTimeZone.getRawOffset()-channelTimeZone.getRawOffset())/(60 * 1000) + mChannel.getTimeZoneCorrectionMinutes();
      value += timeZoneOffsetMinutes;

      int hours = value / 60;
      int minutes = value % 60;

      if (hours >= 24) {
        hours -= 24;
      }
      else if (hours < 0) {
        hours += 24;
      }

      return new StringBuilder().append(hours).append(":").append(
          (minutes < 10) ? "0" : "").append(minutes).toString();
    }
  }

  /**
   * Gets the number of fields this program has.
   *
   * @return the number of fields this program has.
   */
  public int getFieldCount() {
    int count = 0;
    for (int mIntValue : mIntValues) {
      if (mIntValue != -1) {
        count++;
      }
    }
    for (Object mObjectValue : mObjectValues) {
      if (mObjectValue != null) {
        count++;
      }
    }
    return count;
  }

  private static class ProgramFieldIterator implements Iterator<ProgramFieldType> {
    private int mIndex;
    private ArrayList<ProgramFieldType> mFieldTypes;

    public ProgramFieldIterator(final MutableProgram program) {
      mFieldTypes = new ArrayList<ProgramFieldType>(20);
      for (Iterator<ProgramFieldType> iterator = ProgramFieldType.getTypeIterator(); iterator.hasNext();) {
        ProgramFieldType fieldType = iterator.next();
        int format = fieldType.getFormat();
        if (format == ProgramFieldType.INT_FORMAT) {
          if (program.getIntField(fieldType) != -1) {
            mFieldTypes.add(fieldType);
          }
        }
        else if (format == ProgramFieldType.TIME_FORMAT) {
          if (program.getTimeField(fieldType) != -1) {
            mFieldTypes.add(fieldType);
          }
        }
        else if (format == ProgramFieldType.TEXT_FORMAT) {
          if (program.getTextField(fieldType) != null) {
            mFieldTypes.add(fieldType);
          }
        }
        else if (format == ProgramFieldType.BINARY_FORMAT) {
          if (program.getBinaryField(fieldType) != null) {
            mFieldTypes.add(fieldType);
          }
        }
      }
      mIndex = 0;
    }

    @Override
    public boolean hasNext() {
      return mIndex < mFieldTypes.size();
    }

    @Override
    public ProgramFieldType next() {
      return mFieldTypes.get(mIndex++);
    }

    @Override
    public void remove() {
      // not implemented
    }

  }


  /**
   * Gets an iterator over the types of all fields this program has.
   *
   * @return an iterator over {@link ProgramFieldType}s.
   */
  public Iterator<ProgramFieldType> getFieldIterator() {
    return new ProgramFieldIterator(this);
  }

  /**
   * Set a binary field.
   *
   * @param type The type of the field.
   * @param value The binary value to set.
   */
  public void setBinaryField(ProgramFieldType type, byte[] value) {
    checkFormat(type, ProgramFieldType.BINARY_FORMAT);
    setObjectValueField(type, value);
    notifyChangedStatus();
  }

  protected void setObjectValueField(ProgramFieldType type, Object value) {
    synchronized (mObjectValues) {
      mObjectValues[type.getStorageIndex()] = value;
    }
  }

  private void notifyChangedStatus() {
    try {
      if(!mIsLoading) {
        TvDataBase.getInstance().setDayProgramWasChangedByPlugin(getDate(),getChannel());
      }
    }catch(Exception e) {}

    fireStateChanged();
  }

  /**
   * Set a text field.
   *
   * @param type The type of the field.
   * @param value The text value to set.
   */
  public void setTextField(final ProgramFieldType type, String inValue) {
    checkFormat(type, ProgramFieldType.TEXT_FORMAT);

    // Special field treating
    if (type == ProgramFieldType.SHORT_DESCRIPTION_TYPE) {
      inValue = validateShortInfo(inValue);
    }
    String value;
    // filter all the duplicate origin or other fields
    if (inValue != null && inValue.equals("")) {
      value = StringPool.getString("");
    }
    else if (type == ProgramFieldType.ORIGIN_TYPE || type == ProgramFieldType.GENRE_TYPE) {
      value = StringPool.getString(inValue);
    }
    else {
      value = inValue;
    }

    if (type == ProgramFieldType.TITLE_TYPE && value.length() > 0) {
      mTitle = value;
    }

    setObjectValueField(type, value);

    notifyChangedStatus();
  }

  private void checkFormat(final ProgramFieldType type, final int format) {
    if (type.getFormat() != format) {
      throw new IllegalArgumentException("The field " + type.getName()
        + " can't be accessed as " + ProgramFieldType.getFormatName(format)
        + ", because it is " + ProgramFieldType.getFormatName(type.getFormat()));
    }
  }


  /**
   * Set an int field.
   *
   * @param type The type of the field.
   * @param value The int value to set.
   */
  public void setIntField(ProgramFieldType type, int value) {
    checkFormat(type, ProgramFieldType.INT_FORMAT);

    if (type == ProgramFieldType.RATING_TYPE && (value < 0 || (value > 100))) {
      mLog.warning("The value for field " + type.getName()
        + " must be between in [0..100], but it was set to " + value+"; program: "+toString());
      value = -1;
    }

    synchronized (mIntValues) {
      mIntValues[type.getStorageIndex()] = value;
    }

    notifyChangedStatus();
  }


  /**
   * Set a time field.
   *
   * @param type The type of the field.
   * @param value The time value to set.
   */
  public void setTimeField(ProgramFieldType type, int value) {
    checkFormat(type, ProgramFieldType.TIME_FORMAT);

    if ((value < 0) || (value >= (24 * 60))) {
      mLog.warning("The time value for field " + type.getName()
        + " must be between in [0..1439], but it was set to " + value+"; program: "+toString());
    }

    synchronized (mIntValues) {
      mIntValues[type.getStorageIndex()] = value;
    }

    notifyChangedStatus();

    if (type == ProgramFieldType.START_TIME_TYPE) {
      normalizeTimeZone(mLocalDate, value);
    }
  }

  /**
   * Trim text for shortinfo-field
   * @param shortInfo generate Text from this field
   * @return Text that fits into shortInfo
   * @since 2.7
   */
  public static String generateShortInfoFromDescription(String shortInfo) {
    // Get the end of the last fitting sentence
    int lastDot = shortInfo.lastIndexOf('.', MAX_SHORT_INFO_LENGTH);

    int n = shortInfo.lastIndexOf('!', MAX_SHORT_INFO_LENGTH);
    if (n > lastDot) {
      lastDot = n;
    }
    n = shortInfo.lastIndexOf('?', MAX_SHORT_INFO_LENGTH);
    if (n > lastDot) {
      lastDot = n;
    }
    n = shortInfo.lastIndexOf(" - ", MAX_SHORT_INFO_LENGTH);
    if (n > lastDot) {
      lastDot = n;
    }

    int lastMidDot = shortInfo.lastIndexOf('\u00b7', MAX_SHORT_INFO_LENGTH);

    int cutIdx = Math.max(lastDot, lastMidDot);

    // But show at least half the maximum length
    if (cutIdx < (MAX_SHORT_INFO_LENGTH / 2)) {
      cutIdx = shortInfo.lastIndexOf(' ', MAX_SHORT_INFO_LENGTH);
    }

    return shortInfo.substring(0, cutIdx + 1) + "...";
  }


  private String validateShortInfo(String shortInfo) {
    if ((shortInfo != null) && (shortInfo.length() > MAX_SHORT_INFO_LENGTH + 4)) {
      shortInfo = generateShortInfoFromDescription(shortInfo);
      // nowadays this should be checked with the checker plugin
      // these messages will not help the users in any way
      // mLog.warning("Short description longer than " + MAX_SHORT_INFO_LENGTH + " characters: ("+shortInfo.length()+") " + this.toString());
    }

    return shortInfo;
  }


  /**
   * Sets the title of this program.
   *
   * @param title the new title of this program.
   */
  public void setTitle(String title) {
    mTitle = title;

    setTextField(ProgramFieldType.TITLE_TYPE, title);
  }

  /**
   * Returns the title of this program.
   *
   * @return the title of this program.
   */
  public String getTitle() {
    if(mTitle != null && mTitle.trim().length() > 0) {
      return mTitle;
    } else {
      mTitle = getTextField(ProgramFieldType.TITLE_TYPE);
      return mTitle;
    }
  }



  /**
   * Sets a short information about the program (about three lines). May be null.
   * <p>
   * If the length of the short info exceeds 100 characters it will be cut using
   * a smart algorithm.
   *
   * @param shortInfo The new short info.
   */
  public void setShortInfo(String shortInfo) {
    setTextField(ProgramFieldType.SHORT_DESCRIPTION_TYPE, shortInfo);
  }

  /**
   * Returns a short information about the program (about three lines). May be null.
   *
   * @return The short info.
   */
  public String getShortInfo() {
    return getTextField(ProgramFieldType.SHORT_DESCRIPTION_TYPE);
  }



  /**
   * Sets a description about the program. May be null.
   *
   * @param description The description.
   */
  public void setDescription(String description) {
    setTextField(ProgramFieldType.DESCRIPTION_TYPE, description);
  }

  /**
   * Returns a description about the program. May be null.
   *
   * @return The description.
   */
  public String getDescription() {
    return getTextField(ProgramFieldType.DESCRIPTION_TYPE);
  }


  /**
   * Gets the the start time of the program in minutes after midnight.
   *
   * @return the start time.
   */
  public int getStartTime() {
    return mNormalizedStartTime;
  }


  /**
   * Gets the hour-component of the start time of the program.
   *
   * @return the hour-component of the start time.
   */
  public int getHours() {
    return mNormalizedStartTime / 60;
  }


  /**
   * Gets the minute-component of the start time of the program.
   *
   * @return the minute-component of the start time.
   */
  public int getMinutes() {
    return mNormalizedStartTime % 60;
  }


  /**
   * @return The local start time.
   */
  public int getLocalStartTime() {
    return getTimeField(ProgramFieldType.START_TIME_TYPE);
  }


  /**
   * Sets the length of this program in minutes.
   *
   * @param length the new length.
   */
  public void setLength(int length) {
    int startTime = getTimeField(ProgramFieldType.START_TIME_TYPE);
    int endTime = startTime + length;
    if (endTime >= (24 * 60)) {
      endTime -= (24 * 60);
    }

    setTimeField(ProgramFieldType.END_TIME_TYPE, endTime);
  }

  public int getLength() {
    int endTime = getTimeField(ProgramFieldType.END_TIME_TYPE);
    if (endTime == -1) {
      return -1;
    }

    int startTime = getTimeField(ProgramFieldType.START_TIME_TYPE);
    if (endTime < startTime) {
      endTime += (24 * 60);
    }

    return endTime - startTime;
  }



  /**
   * Sets additional information of the program (or zero).
   *
   * @param info The new additional information.
   */
  public void setInfo(int info) {
    setIntField(ProgramFieldType.INFO_TYPE, info);
  }

  /**
   * Returns additional information of the program (or zero).
   *
   * @return the new additional information.
   */
  public int getInfo() {
    return getIntField(ProgramFieldType.INFO_TYPE);
  }



  /**
   * Returns the channel object of this program.
   *
   * @return The channel.
   */
  public Channel getChannel() {
    return mChannel;
  }



  /**
   * Returns the date of this program.
   *
   * @return the date.
   */
  public devplugin.Date getDate() {
    return mNormalizedDate;
  }

  /**
   * @return The local date
   */
  public devplugin.Date getLocalDate() {
    return mLocalDate;
  }


  /**
   * Gets a String representation of this program for debugging.
   *
   * @return A String representation for debugging.
   */
  public String toString() {
    return "On " + mChannel.getName() + " at " + getHours() + ":" + getMinutes()
      + ", " + getDateString() + ": '" + getTitle() + "'";
  }


  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (o instanceof Program) {
      Program program = (Program)o;

      String title = getTitle();
      String otherTitle = program.getTitle();

      return getStartTime() == program.getStartTime()
        && equals(mChannel, program.getChannel())
        && equals(getDate(), program.getDate())
        && title != null && otherTitle != null
        && title.compareTo(otherTitle) == 0;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = HashCodeUtilities.hash(getStartTime());
    result = HashCodeUtilities.hash(result, mChannel);
    result = HashCodeUtilities.hash(result, getDate());
    result = HashCodeUtilities.hash(result, getTitle());
    return result;
  }

  /**
   * check if two programs are identical by their field contents
   *
   * @param program
   * @return <code>true</code>, if all fields are equal
   * @since 2.6
   */
  public boolean equalsAllFields(MutableProgram program) {
    if (!equals(program)) {
      return false;
    }
    if (!Arrays.equals(mIntValues, program.mIntValues)) {
      return false;
    }
    for (int i = 0; i < mObjectValues.length; i++) {
      if ((mObjectValues[i] == null) != (program.mObjectValues[i] == null)) {
        return false;
      }
    }
    for (Iterator<ProgramFieldType> iterator = ProgramFieldType.getTypeIterator(); iterator.hasNext();) {
      ProgramFieldType fieldType = iterator.next();
      int format = fieldType.getFormat();
      if (format == ProgramFieldType.TEXT_FORMAT || format == ProgramFieldType.BINARY_FORMAT) {
        Object thisValue = getObjectValueField(fieldType);
        Object otherValue = program.getObjectValueField(fieldType);
        if (thisValue!= null && !thisValue.equals(otherValue)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Gets whether two objects are equal. Can handle null values.
   *
   * @param o1 The first object.
   * @param o2 The second object.
   * @return Whether the two objects are equal.
   */
  private boolean equals(Object o1, Object o2) {
    if ((o1 == null) || (o2 == null)) {
      return (o1 == o2);
    } else {
      return o1.equals(o2);
    }
  }

  /**
   * Sets the state of this program to a
   * program state.
   *
   * @param state The state of this program.
   * @since 2.2
   */
  protected void setProgramState(int state) {
    mState = (byte) state;
  }

  /**
   * Returns the state of this program.
   *
   * @return The program state.
   * @since 2.2
   */
  public int getProgramState() {
    return mState;
  }

  /**
   * Informs the ChangeListeners for repainting if a Plugin
   * uses more than one Icon for the Program.
   *
   * @since 2.2.2
   */
  public final void validateMarking() {
    mMarkPriority = Program.NO_MARK_PRIORITY;

    for(Marker mark : mMarkerArr) {
      mMarkPriority = (byte) Math.max(mMarkPriority,mark.getMarkPriorityForProgram(this));
    }

    fireStateChanged();
  }

  /**
   * Sets the marker array of this program.
   *
   * @param marker The marker array.
   * @since 2.2.1
   */
  protected void setMarkerArr(Marker[] marker) {
    if (marker.length == 0) {
      mMarkerArr = EMPTY_MARKER_ARR;
    }
    else {
      mMarkerArr = marker;
    }
    //fireStateChanged();
  }

  /**
   * Sets the loading state to false.
   * Call this after creation of the program from the data service.
   *
   * @since 2.2.2
   */
  public void setProgramLoadingIsComplete() {
    mIsLoading = false;
  }

  /**
   * Gets the priority of the marking of this program.
   *
   * @return The mark priority.
   * @since 2.5.1
   */
  public int getMarkPriority() {
    return mMarkPriority;
  }

  /**
   * Sets the mark priority for this program
   *
   * @since 2.5.1
   */
  protected void setMarkPriority(int markPriority) {
    mMarkPriority = (byte) markPriority;
  }

  public boolean hasFieldValue(final ProgramFieldType type) {
    int format = type.getFormat();
    if (format == ProgramFieldType.INT_FORMAT || format == ProgramFieldType.TIME_FORMAT) {
      synchronized (mIntValues) {
        return mIntValues[type.getStorageIndex()] != -1;
      }
    }
    else if (format == ProgramFieldType.TEXT_FORMAT || format == ProgramFieldType.BINARY_FORMAT) {
      synchronized (mObjectValues) {
        return mObjectValues[type.getStorageIndex()] != null;
      }
    }
    return false;
  }
}
