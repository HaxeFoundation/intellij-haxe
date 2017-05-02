/*
 * Copyright 2017 Eric Bishton
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.management.ThreadInfo;

/**
 * Utilities to help debug this plugin.
 *
 * Created by ebishton on 4/25/17.
 */
public class HaxeDebugUtil {

  /**
   * Count the number of times a class appears on the current thread's stack.
   *
   * @param klass class to search for
   * @return the number of times the class appears in the stack.
   */
  public static int countStackAppearances(@NotNull Class klass) {
    return countStackAppearances(klass, null);
  }

  /**
   * Count the number of times a class (and, optionally, a method) appears on the stack.
   *
   * @param klass  class name to search for
   * @param method method name to search for
   * @return a count of the number of times that the class and method combination appears
   * on the stack.
   */
  public static int countStackAppearances(@NotNull Class klass, @Nullable String method) {
    int count = 0;

    String klassName = klass.getName();
    StackTraceElement[] stack = new Exception().getStackTrace();
    for (StackTraceElement frame : stack) {
      if (frame.getClassName().equals(klassName)) {
        if (null == method || method.equals(frame.getMethodName())) {
          count++;
        }
      }
    }
    return count;
  }

  /**
   * Gets the current stack depth (not counting this call).
   *
   * @return the number of stack frames on the stack at the point of our caller.
   */
  public static int getStackDepth() {
    return new Exception().getStackTrace().length - 1; // Don't count this call.
  }

  /**
   * Gets the calling stack frame at the point getMyCaller is called.  (So, two
   * frames up.)
   *
   * @return The stack frame for the caller of the method making the request.
   */
  public static StackTraceElement getCallerStackFrame() {
    StackTraceElement[] stack = new Exception().getStackTrace();
    if (stack.length <= 2) {
      throw new IndexOutOfBoundsException("Not enough callers on the stack.");
    }
    return stack[stack.length - 2];
  }

  /**
   * Create a trace message.  Limits overall length to 500 characters.
   *
   * @param message text to append to the standard trace output.
   * @return
   */
  public static String traceMessage(String message) {
    return traceMessage(message, 500);
  }

  /**
   * Create a trace message.  Limits overall length to a given number of characters.
   *
   * @param message text to append to the standard trace output.
   * @return
   */
  public static String traceMessage(String message, int maxMessageLength) {
    StringBuilder msg = new StringBuilder();
    //msg.append("thread=");
    //msg.append(Thread.currentThread().getId());
    //msg.append(':');
    msg.append("depth=");
    msg.append(HaxeDebugUtil.getStackDepth() - 1);  // Keep ourselves out of the count.
    msg.append(':');
    if (null != message) {
      msg.append(message);
    }
    int ellipsesPos = maxMessageLength - 3;
    if (maxMessageLength < msg.length() && ellipsesPos > 0) {
      msg.replace(ellipsesPos, maxMessageLength, "...");
    }
    int len = Math.min(maxMessageLength, msg.length());
    return msg.substring(0, len);
  }
}