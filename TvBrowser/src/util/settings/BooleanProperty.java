/*
 * TV-Browser
 * Copyright (C) 04-2003 Martin Oberhauser (darras@users.sourceforge.net)
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
 *     $Date: 2004-10-22 16:02:11 +0200 (Fri, 22 Oct 2004) $
 *   $Author: darras $
 * $Revision: 836 $
 */
package util.settings;

/**
 * 
 * 
 * @author Til Schneider, www.murfman.de
 */
public class BooleanProperty extends Property {
  
  private boolean mDefaultValue;
  private boolean mIsCacheFilled;
  private boolean mCachedValue;
  
  
  
  public BooleanProperty(PropertyManager manager, String key,
    boolean defaultValue)
  {
    super(manager, key);
    
    mDefaultValue = defaultValue;
    mIsCacheFilled = false;
  }


  public boolean getDefault() {
    return mDefaultValue;
  }


  public boolean getBoolean() {
    if (! mIsCacheFilled) {
      String asString = getProperty();
      if (asString == null) {
        mCachedValue = mDefaultValue;
      } else {
        if (asString.equalsIgnoreCase("true") || asString.equalsIgnoreCase("yes")) {
          mCachedValue = true;
        }
        else if (asString.equalsIgnoreCase("false") || asString.equalsIgnoreCase("no")) {
          mCachedValue = false;
        }
        else {
          mCachedValue = mDefaultValue;
        }
      }

      mIsCacheFilled = true;
    }

    return mCachedValue;
  }
  
  
  public void setBoolean(boolean value) {
    if (value == mDefaultValue) {
      setProperty(null);
    } else {
      if (value) {
        setProperty("true");
      } else {
        setProperty("false");
      }
    }
    
    mCachedValue = value;
  }
  
  
  protected void clearCache() {
    mIsCacheFilled = false;
  }

}
