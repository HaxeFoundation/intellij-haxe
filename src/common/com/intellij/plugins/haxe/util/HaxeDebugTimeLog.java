/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.util;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

/**
 * Create, track, and log time stamps for debugging purposes.
 * Has a static interface for simple time stamps as well.
 */
public final class HaxeDebugTimeLog {

  // For logging issues with this class.
  final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.util.HaxeDebugTimeLog");

  // For logging time stamps that go on another logger.
  Logger myLog;
  LinkedList<TimeStamp> myTimeStamps;
  Since mySince;

  private SimpleDateFormat myDateFormatter;   // Re-used by internalFormat to avoid churn.
  private Date myDate;                        // Re-used by internalFormat to avoid churn.
  private static final String LESS_THAN_ONE_MS = " <1";

  /**
   * How to display time stamps.
   */
  public static enum Since {
    None,             // Don't print elapsed times.
    Start,            // Print elapsed time since the time log started. (=nnn)
    Previous,         // Print elapsed time since the last time stamp.  (+nnn)
    StartAndPrevious, // Print both of the above.                       (+nnn=nnn)
  };

  public HaxeDebugTimeLog(@Nullable String messagePrefix) {
    this(messagePrefix, Since.StartAndPrevious);
  }

  public HaxeDebugTimeLog(@Nullable String messagePrefix, Since since) {
    this(Logger.getInstance(messagePrefix), since);
    myLog.setLevel(Level.DEBUG);
  }

  public HaxeDebugTimeLog(@NotNull Logger log, Since since) {
    myLog = log;
    myTimeStamps = new LinkedList<TimeStamp>();
    mySince = since;
    myDateFormatter = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss.SSS");
    myDate = new Date(System.currentTimeMillis());
  }

  /**
   * Create a new log and add a "Start" entry to it.
   *
   * @param prefix - prefix, or log identifier.
   * @return a new time log against which events can be captured (see stamp()).
   */
  @NotNull
  static public HaxeDebugTimeLog startNew(@NotNull String prefix) {
    return startNew(prefix, Since.None);
  }

  /**
   * Create a new log and add a "Start" entry to it.
   *
   * @param prefix - prefix, or log identifier.
   * @param since - how to display elapsed and accumulated time.  (see Since)
   * @return a new time log against which events can be captured (see stamp()).
   */
  @NotNull
  static public HaxeDebugTimeLog startNew(@NotNull String prefix, Since since) {
    HaxeDebugTimeLog log = new HaxeDebugTimeLog(prefix, since);
    log.stamp("Start");
    return log;
  }

  /**
   * Log an event and time to the given Logger.
   *
   * @param logger where to write the time stamp.
   * @param message text to identify the message.
   */
  public static void logStamp(@NotNull Logger logger, @Nullable String message) {
    if (null == message) message = "";
    logger.debug(message + ":" + formatTime(System.currentTimeMillis()));
  }

  /**
   * Format a time as a human readable date and time.
   * An instance-based time formatter.
   *
   * @param timeMillis milliseconds since the Java epoch.
   * @return a readable date
   */
  public synchronized String internalFormat(long timeMillis) {
    myDate.setTime(timeMillis);
    return myDateFormatter.format(myDate);
  }

  /**
   * Format a time as a human readable date and time.
   * The static version creates churn; prefer internalFormat, if you can.
   *
   * @param timeMillis
   * @return
   */
  public static String formatTime(long timeMillis) {
    SimpleDateFormat f = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss.SSS");
    Date d = new Date(timeMillis);
    return f.format(d);
  }

  /**
   * Format a time difference for readability.
   *
   * @param timeMillis elapsed time in milliseconds.
   * @return a (hopefully) more readable time.
   */
  public static String formatElapsedTime(long timeMillis) {
    if (timeMillis <= 0) return LESS_THAN_ONE_MS;

    // java.text.DateFormat is far too heavyweight to use here.
    int millis = (int)(timeMillis % 1000);
    int secs = (int)((timeMillis / 1000) % 60);
    int mins = (int)((timeMillis / 1000 / 60) % 60);
    int hours = (int)((timeMillis / 1000 / 60 / 60) % 24);
    long days = (timeMillis / 1000 / 60 / 60 / 24);

    int size = 22;  // Allows 999999 days.
    int pos = size;
    char[] buf = new char[size];
    char[] numbers = {'0','1','2','3','4','5','6','7','8','9'};

    buf[--pos] = numbers[millis % 10];
    buf[--pos] = numbers[(millis / 10) % 10];
    buf[--pos] = numbers[(millis / 100)];
    if (days > 0 || hours > 0 || mins > 0 || secs > 0) {
      buf[--pos] = 's';
      buf[--pos] = numbers[secs % 10];
      buf[--pos] = numbers[secs / 10];
    }
    if (days > 0 || hours > 0 || mins > 0) {
      buf[--pos] = 'm';
      buf[--pos] = numbers[mins % 10];
      buf[--pos] = numbers[mins / 10];
    }
    if (days > 0 || hours > 0) {
      buf[--pos] = 'h';
      buf[--pos] = numbers[hours % 10];
      buf[--pos] = numbers[hours / 10];
    }
    if (days > 0) {
      buf[--pos] = 'd';
      while (days > 0 && pos > 0) {
        buf[--pos] = numbers[(int)(days % 10)];
        days /= 10;
      }
    }

    String s = new String(buf, pos, size-pos);
    return s;
  }

  /**
   * Create a time stamp and add it to this Log.
   *
   * @param message text to identify the message.
   */
  public void stamp(@Nullable String message) {
    TimeStamp ts = new TimeStamp(message);
    myTimeStamps.add(ts);
  }

  /**
   * Create a time stamp, add it to this Log, and echo it to the configured Logger
   * immediately.  The stamp will be repeated in the <code>print</code> output.
   *
   * @param message text to identify the message.
   */
  public void stampAndEcho(@Nullable String message) {
    stampAndEcho(message, mySince);
  }

  /**
   * Create a time stamp, add it to this Log, and echo it to the Logger
   * immediately.  The stamp will be repeated in the <code>print</code> output.
   *
   * @param message text to identify the message.
   * @param since output control of elapsed time (See HaxeDebugTimeLog.Since.)
   */
  public synchronized void stampAndEcho(@Nullable String message, Since since) {
    TimeStamp ts = new TimeStamp(message);
    TimeStamp previous = myTimeStamps.isEmpty() ? null : myTimeStamps.getLast();
    myTimeStamps.add(ts);
    switch (since) {
      default:
        LOG.assertTrue(false, "Unknown 'Since' type encountered.");
      case None:
        ts.log();
        break;
      case Start:
        ts.log(myLog, false, myTimeStamps.getFirst(), previous);
        break;
      case Previous:
        ts.log(myLog, false, null, previous);
        break;
      case StartAndPrevious:
        ts.log(myLog, false, myTimeStamps.getFirst(), previous);
        break;
    }
  }


  /**
   * Print the entire time log, nicely formatted, as one entry to the Logger,
   * but only if the log time span exceeds the given number of milliseconds.
   *
   * @param milliseconds the total time that the log must represent before
   *                     printing occurs.
   */
  public synchronized void printIfTimeExceeds(long milliseconds) {
    if (milliseconds < 0 || myTimeStamps.isEmpty()) {
      return;
    }
    TimeStamp first = myTimeStamps.getFirst();
    TimeStamp last = myTimeStamps.getLast();
    if (null == last) {
      return;
    }
    if (last.myTimeStamp - first.myTimeStamp > milliseconds) {
      print();
    }
  }

  /**
   * Print the entire time log, nicely formatted, as one entry to the Logger.
   */
  public synchronized void print() {
    StringBuilder sb = new StringBuilder(2048);
    TimeStamp first = myTimeStamps.getFirst();
    TimeStamp previous = null;
    sb.append("Log entries: \n");
    for (TimeStamp ts : myTimeStamps) {
      sb.append("        ");
      sb.append(ts.buildLogMsg(true, first, previous));
      sb.append("\n");
      previous = ts;
    }
    myLog.debug(sb.toString());
  }

  /**
   * Encapsulate a time stamp.
   */
  public class TimeStamp {
    final String myMessage;
    final long myTimeStamp;

    public TimeStamp(@Nullable String message) {
      myMessage = message;
      myTimeStamp = System.currentTimeMillis();
    }

    public void log() {
      log(myLog);
    }

    public void log(@NotNull Logger logger) {
      logger.debug(buildLogMsg(false, null, null));
    }

    public void log(@NotNull Logger logger, boolean showTimeStamp, @Nullable TimeStamp first, @Nullable TimeStamp previous) {
      logger.debug(buildLogMsg(showTimeStamp, first, previous));
    }

    public String buildLogMsg(boolean showTimeStamp, @Nullable TimeStamp first, @Nullable TimeStamp previous) {
      StringBuilder msg = new StringBuilder(256);
      if (showTimeStamp) {
        msg.append(internalFormat(myTimeStamp));
        msg.append(":");
      }
      if (null != previous && previous != this) {
        msg.append("+");
        msg.append(formatElapsedTime(myTimeStamp - previous.myTimeStamp));
      }
      if (null != first && first != this) {
        msg.append("=");
        msg.append(formatElapsedTime(myTimeStamp - first.myTimeStamp));
      }
      if (null != myMessage) {
        msg.append(": ");
        msg.append(myMessage);
      }
      return msg.toString();
    }

    public String toString() {
      return buildLogMsg(true, null, null);
    }

  }

}
