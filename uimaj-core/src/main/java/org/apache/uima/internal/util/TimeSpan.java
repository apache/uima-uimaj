/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.internal.util;

/**
 * Encode a span of time. The main purpose of this class is to provide a printing utility for time
 * spans. E.g., 1081 ms should be printed as 1.081 s, 108101 ms should be printed as 1 min 48.101 s,
 * etc.
 * <p>
 * 
 * Note that the largest value you can represent with this class is 9223372036854775807
 * (<code>Long.MAX_VALUE</code>), or equivalently, 292471208 yrs 247 days 7 hrs 12 min 55.807 sec.
 * Overflow is not handled gracefully by this class.
 * <p>
 * 
 * Also note that for the purposes of this class, a year has 365 days. I.e., a year corresponds to
 * <code>365 * 24 * 60 * 60 * 1000</code> ms.
 */
public class TimeSpan {

  private long all;

  private int years;

  private int days;

  private int hours;

  private int minutes;

  private int seconds;

  private int milliseconds;

  private boolean knowsFull = false;

  private boolean knowsMS = false;

  private static final long msSecond = 1000;

  private static final long msMinute = msSecond * 60;

  private static final long msHour = msMinute * 60;

  private static final long msDay = msHour * 24;

  private static final long msYear = msDay * 365;

  private static final String yearsString = "yrs";

  private static final String daysString = "days";

  private static final String hoursString = "hrs";

  private static final String minutesString = "min";

  private static final String secondsString = "sec";

  private static final String msString = "ms";

  private static final String unknownTime = "unknown";

  /**
   * Create an uninstantiated <code>TimeSpan</code>.
   */
  public TimeSpan() {
  }

  /**
   * Create a <code>TimeSpan</code> from a ms interval.
   * 
   * @param milliseconds
   *          The interval in ms. If <code>milliseconds
   &lt; 0</code>, an uninstantiated <code>TimeSpan</code> is created.
   */
  public TimeSpan(long milliseconds) {
    if (milliseconds >= 0) {
      all = milliseconds;
      knowsMS = true;
    }
  }

  /**
   * @return <code>true</code>, if the object has been instantiated with a legal interval;
   *         <code>false</code>, else.
   */
  public boolean isInstantiated() {
    return (knowsMS || knowsFull);
  }

  /**
   * Set the year fraction of this <code>TimeSpan</code>.
   * 
   * @param years
   *          The number of years.
   * @return <code>false</code>, if <code>years &lt; 0</code>; <code>true</code>, else.
   */
  public boolean setYears(int years) {
    if (years < 0) {
      return false;
    }
    this.years = years;
    knowsFull = true;
    knowsMS = false;
    return true;
  }

  /**
   * Set the day fraction of this <code>TimeSpan</code>.
   * 
   * @param days
   *          The number of days.
   * @return <code>false</code>, if <code>days &lt; 0</code>; <code>true</code>, else.
   */
  public boolean setDays(int days) {
    if (days < 0) {
      return false;
    }
    this.days = days;
    knowsFull = true;
    knowsMS = false;
    return true;
  }

  /**
   * Set the hour fraction of this <code>TimeSpan</code>.
   * 
   * @param hours
   *          The number of hours.
   * @return <code>false</code>, if <code>hours &lt; 0</code>; <code>true</code>, else.
   */
  public boolean setHours(int hours) {
    if (hours < 0) {
      return false;
    }
    this.hours = hours;
    knowsFull = true;
    knowsMS = false;
    return true;
  }

  /**
   * Set the minute fraction of this <code>TimeSpan</code>.
   * 
   * @param minutes
   *          The number of minutes.
   * @return <code>false</code>, if <code>minutes &lt; 0</code>; <code>true</code>, else.
   */
  public boolean setMinutes(int minutes) {
    if (minutes < 0) {
      return false;
    }
    this.minutes = minutes;
    knowsFull = true;
    knowsMS = false;
    return true;
  }

  /**
   * Set the second fraction of this <code>TimeSpan</code>.
   * 
   * @param seconds
   *          The number of seconds.
   * @return <code>false</code>, if <code>seconds &lt; 0</code>; <code>true</code>, else.
   */
  public boolean setSeconds(int seconds) {
    if (seconds < 0) {
      return false;
    }
    this.seconds = seconds;
    knowsFull = true;
    knowsMS = false;
    return true;
  }

  /**
   * Set the millisecond fraction of this <code>TimeSpan</code>.
   * 
   * @param milliseconds
   *          The number of milliseconds.
   * @return <code>false</code>, if <code>milliseconds &lt; 0</code>; <code>true</code>, else.
   */
  public boolean setMilliseconds(int milliseconds) {
    if (milliseconds < 0) {
      return false;
    }
    this.milliseconds = milliseconds;
    knowsFull = true;
    knowsMS = false;
    return true;
  }

  /**
   * Set the full <code>TimeSpan</code> in terms of milliseconds.
   * 
   * @param milliseconds
   *          The number of milliseconds.
   * @return <code>false</code>, if <code>milliseconds &lt; 0</code>; <code>true</code>, else.
   */
  public boolean setFullMilliseconds(long milliseconds) {
    if (milliseconds < 0) {
      return false;
    }
    all = milliseconds;
    knowsMS = true;
    knowsFull = false;
    return true;
  }

  /**
   * Get the length of the <code>TimeSpan</code> as milliseconds.
   * 
   * @return The number of milliseconds, if known. <code>-1</code>, else (e.g., when the
   *         <code>TimeSpan</code> is not instantiated).
   */
  public long getFullMilliseconds() {
    ensureAll();
    if (!knowsMS) {
      return -1;
    }
    return all;
  }

  /**
   * Get the year fraction of this object.
   * 
   * @return <code>-1</code>, if this object is not instantiated; the year fraction, else.
   */
  public int getYears() {
    ensureFull();
    return isInstantiated() ? years : -1;
  }

  /**
   * Get the day fraction of this object.
   * 
   * @return <code>-1</code>, if this object is not instantiated; the day fraction, else.
   */
  public int getDays() {
    ensureFull();
    return isInstantiated() ? days : -1;
  }

  /**
   * Get the hour fraction of this object.
   * 
   * @return <code>-1</code>, if this object is not instantiated; the hour fraction, else.
   */
  public int getHours() {
    ensureFull();
    return isInstantiated() ? hours : -1;
  }

  /**
   * Get the minute fraction of this object.
   * 
   * @return <code>-1</code>, if this object is not instantiated; the minute fraction, else.
   */
  public int getMinutes() {
    ensureFull();
    return isInstantiated() ? minutes : -1;
  }

  /**
   * Get the second fraction of this object.
   * 
   * @return <code>-1</code>, if this object is not instantiated; the second fraction, else.
   */
  public int getSeconds() {
    ensureFull();
    return isInstantiated() ? seconds : -1;
  }

  /**
   * Get the millisecond fraction of this object.
   * 
   * @return <code>-1</code>, if this object is not instantiated; the millisecond fraction, else.
   */
  public int getMilliseconds() {
    ensureFull();
    return isInstantiated() ? milliseconds : -1;
  }

  /**
   * @return String representation of object. See class comments.
   */
  @Override
  public String toString() {
    ensureFull();
    if (!knowsFull) {
      return unknownTime;
    }
    StringBuffer buf = new StringBuffer();
    boolean started = false;
    if (years > 0) {
      buf.append(years);
      buf.append(' ');
      buf.append(yearsString);
      started = true;
    }
    if (started || days > 0) {
      if (started) {
        buf.append(' ');
      }
      buf.append(days);
      buf.append(' ');
      buf.append(daysString);
      started = true;
    }
    if (started || hours > 0) {
      if (started) {
        buf.append(' ');
      }
      buf.append(hours);
      buf.append(' ');
      buf.append(hoursString);
      started = true;
    }
    if (started || minutes > 0) {
      if (started) {
        buf.append(' ');
      }
      buf.append(minutes);
      buf.append(' ');
      buf.append(minutesString);
      started = true;
    }
    if (started || seconds > 0) {
      if (started) {
        buf.append(' ');
      }
      buf.append(seconds);
      started = true;
    }
    if (started) {
      buf.append('.');
      if (milliseconds < 100) {
        buf.append('0');
        if (milliseconds < 10) {
          buf.append('0');
        }
      }
      buf.append(milliseconds);
      buf.append(' ');
      buf.append(secondsString);
    } else {
      buf.append(milliseconds);
      buf.append(' ');
      buf.append(msString);
    }
    return buf.toString();
  }

  private void ensureAll() {
    if (knowsMS || !knowsFull) {
      return;
    }
    all = 0;
    all += years * msYear;
    all += days * msDay;
    all += hours * msHour;
    all += minutes * msMinute;
    all += seconds * msSecond;
    all += milliseconds;
    knowsMS = true;
  }

  private void ensureFull() {
    if (knowsFull || !knowsMS) {
      return;
    }
    long t = all;
    years = (int) (t / msYear);
    t %= msYear;
    days = (int) (t / msDay);
    t %= msDay;
    hours = (int) (t / msHour);
    t %= msHour;
    minutes = (int) (t / msMinute);
    t %= msMinute;
    seconds = (int) (t / msSecond);
    t %= msSecond;
    milliseconds = (int) t;
    knowsFull = true;
  }

}
