/*
 * Copyright 2018 Eric Bishton
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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;

/**
 * Sends user notifications to the event log.
 */
public class HaxeEventLogUtil {

  private HaxeEventLogUtil() {}

  /**
   * Posts/displays a user message to the event log.  May force the event log pane to
   * open depending upon the {@link NotificationType} of the {@link Notification}.
   *
   * @param n Notification to post/display.
   */
  public static void SendToEventLog(Notification n) {
    Notifications sink = ApplicationManager.getApplication().getMessageBus().syncPublisher(Notifications.TOPIC);
    sink.notify(n);
  }

  /**
   * Post/display an error message to the event log.  This *WILL* force the event log to open.
   *
   * @param id - Group display ID.  Used for grouping messages.  See {@link Notification}.
   * @param title
   * @param details
   */
  public static void error(String id, String title, String... details) {
    SendToEventLog(new Notification(id, title, concat(details), NotificationType.ERROR));
  }

  /**
   * Post/display a warning message to the event log.  Does NOT force the event log to open.
   *
   * @param id - Group display ID.  Used for grouping messages.  See {@link Notification}.
   * @param title
   * @param details
   */
  public static void warn(String id, String title, String... details) {
    SendToEventLog(new Notification(id, title, concat(details), NotificationType.WARNING));
  }

  /**
   * Post/display an informational message to the event log.  Does NOT force the event log to open.
   *
   * @param id - Group display ID.  Used for grouping messages.  See {@link Notification}.
   * @param title
   * @param details
   */
  public static void info(String id, String title, String... details) {
    SendToEventLog(new Notification(id, title, concat(details), NotificationType.INFORMATION));
  }

  /**
   * Formats the details for display.
   *
   * @param details
   * @return
   */
  private static String concat(String... details) {
    return String.join(" ", details);
  }
}
