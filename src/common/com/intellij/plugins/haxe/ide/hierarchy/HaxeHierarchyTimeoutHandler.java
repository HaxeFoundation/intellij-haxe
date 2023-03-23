/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2015 Elias Ku
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
package com.intellij.plugins.haxe.ide.hierarchy;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.plugins.haxe.util.HaxeDebugTimeLog;
import org.apache.log4j.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ebishton on 1/20/15.
 *
 * Stops runaway hierarchy search tasks.  Uses a java.util.Timer to stop
 * the background task (via canceling the ProgressIndicator), if the task
 * takes too long to execute.
 *
 * (Too long is currently hard coded to 15 seconds.)
 *
 */
final public class HaxeHierarchyTimeoutHandler {

  private static final boolean DEBUG = false;
  private static final HaxeDebugLogger LOG = HaxeDebugLogger.getInstance("#com.intellij.ide.hierarchy.HaxeHierarchyTimeoutHandler");

  static
  {
    if (DEBUG) {
      LOG.setLevel(Level.DEBUG);
    }
  }

  // XXX: Probably should make this configurable somewhere in settings -- but where?
  private static final long max_duration_seconds = 20;
  private static final long max_duration = max_duration_seconds * 1000;

  // Track the time this takes.  If too long, we'll cancel it.
  final private long startTime;
  private long stopTime;
  private boolean canceled;
  private boolean stopped;
  private Timer myTimer;

  /**
   * Create and start a new timeout handler.
   */
  public HaxeHierarchyTimeoutHandler() {
    startTime = System.currentTimeMillis();
    stopTime = 0;
    canceled = false;
    stopped = false;
    final HaxeHierarchyTimeoutHandler myself = this;
    // We have to capture the progress indicator here because the timer fires on a different
    // thread, and won't be able to find it.  So we capture it and hold it in the closure.
    final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
    LOG.debug("Progress Indicator is" + progressIndicator.toString());

    myTimer = new Timer(getClass().getSimpleName());
    myTimer.schedule( new TimerTask() {
      @Override
      public void run() {
        LOG.debug("Timer fired.");
        myself.cancel(progressIndicator);
      }
    }, max_duration);
  }

  /**
   * Cancel the current search.  This is the workhorse when the timer fires, but is
   * fine to be called by another task that wants to cancel the search (such as
   * another search).
   *
   * Once canceled, use isCanceled() to determine if this function was called.
   * Use isStopped to determine if the timer was stopped via stop(), and isRunning()
   * to see if the tasks is still running.
   */
  public synchronized void cancel(@Nullable ProgressIndicator progressIndicator) {
    if (!canceled) {
      myTimer.cancel();
      stopTime = System.currentTimeMillis();
      if (null == progressIndicator) {
        progressIndicator = ProgressManager.getInstance().getProgressIndicator();
      }
      if (null != progressIndicator) {
        LOG.debug("Canceling the Progress Indicator.");
        progressIndicator.cancel();
      } else {
        LOG.debug("No Progress Indicator to cancel.");
      }

      HaxeDebugTimeLog.logStamp(LOG, "Canceling hierarchy request. Took too long.");
      LOG.debug("Timer canceled after " + (stopTime - startTime) + " milliseconds.");
    }
    canceled = true;
  }

  /**
   * Stop this handler from firing.  Destroys the underlying Timer.
   */
  public synchronized void stop() {
    myTimer.cancel();
    stopped = true;

    stopTime = System.currentTimeMillis();
    LOG.debug("Timer stopped after " + (stopTime - startTime) + " milliseconds.");
  }

  /**
   * Checks if the maximum time has been reached, and, if so, cancels the
   * search process.
   *
   * NOTE: This may not be necessary now that a Timer is being employed.
   *
   * @return  true if the process has been cancelled; false, otherwise.
   */
  public synchronized boolean checkAndCancelIfNecessary() {
    if (!canceled && (System.currentTimeMillis() - startTime) > max_duration) {
      cancel(null);
    }
    return canceled;
  }

  /**
   * Determine whether this timer was canceled.  (Either automatically, or by
   * calling cancel().)
   *
   * @return true if this timer was canceled; false, if not, though it may
   *          not still be running.  Use isRunning() to determine that.
   */
  public synchronized boolean isCanceled() {
    return canceled;
  }

  /**
   * Determine whether this timer was stopped manually.
   *
   * @return true if this timer was stopped via the stop() function; false, if not.
   */
  public synchronized boolean isStopped() {
    return stopped;
  }

  /**
   * Determine whether this timer is still running.
   *
   * @return true if the timer is still running, waiting for the allotted time to expire.
   */
  public synchronized boolean isRunning() {
    return !stopped && !canceled;
  }

  /**
   * Post a modal dialog on screen to alert the user that the search was cancelled and
   * the results may not be complete.
   *
   * Warns if the process was not cancelled.
   */
  public void postCanceledDialog(Project project) {
    if (!canceled) {
      LOG.warn("Displaying cancel message dialog when the process was not canceled.");
    }

    final Project messageProject = project;
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        // TODO: Put this message and title in a resource bundle.
        String title = "Call Hierarchy Search Timed Out";
        String msg = "Search took too long (>" + max_duration_seconds + "seconds).  Results may be incomplete.";
        if (DEBUG) {
          msg += " Canceled after " + (stopTime - startTime) + " milliseconds.";
        }
        Messages.showMessageDialog(messageProject, msg, title, Messages.getWarningIcon());
      }
    });
  }
}
