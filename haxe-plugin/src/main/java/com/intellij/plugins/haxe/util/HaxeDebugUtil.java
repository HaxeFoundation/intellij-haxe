/*
 * Copyright 2017-2018 Eric Bishton
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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
   * Determine whether a class appears on the (current thread's) stack.
   * @param klass Class name to look for.
   * @return true, if the class is on the stack (some member function is being executed); false, otherwise.
   */
  public static boolean appearsOnStack(@NotNull Class klass) {
    String klassName = klass.getName();
    StackTraceElement[] stack = new Exception().getStackTrace();
    for (StackTraceElement frame : stack) {
      if (frame.getClassName().equals(klassName)) {
        return true;
      }
    }
    return false;
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
   * Gets the calling stack frame at the point getCallerStackFrame is called.  (So, two
   * frames up.)
   *
   * @return The stack frame for the caller of the method making the request.
   */
  public static StackTraceElement getCallerStackFrame() {
    StackTraceElement[] stack = new Exception().getStackTrace();
    if (stack.length <= 2) {
      throw new IndexOutOfBoundsException("Not enough callers on the stack.");
    }
    return stack[2]; // [0] is this method
  }

  public static String getCallerCanonicalName() {
    StackTraceElement[] stack = new Exception().getStackTrace();
    if (stack.length <= 2) {
      throw new IndexOutOfBoundsException("Not enough callers on the stack.");
    }
    // stack[0] is this method, [1] is the method that wants to know its caller.
    StackTraceElement caller = stack[2];
    return caller.getClassName() + "." + caller.getMethodName();
  }

  public static String printCallers(int count) {
    StringBuilder out = new StringBuilder();
    StackTraceElement[] stack = new Exception().getStackTrace();
    int offset = 2;
    for (int i = offset; i < count + offset && i < stack.length; ++i) {
      out.append(stack[i].toString());
      out.append('\n');
    }
    return out.toString();
  }

  /**
   * Create a trace message.  Limits overall length to 500 characters.
   *
   * @param message text to append to the standard trace output.
   * @return
   */
  public static String traceMessage(String message) {
    return createTraceMessage(message, 500, 1, false);
  }

  /**
   * Create a trace message.  Limits overall length to a given number of characters.
   *
   * @param message text to append to the standard trace output.
   * @return
   */
  public static String traceMessage(String message, int maxMessageLength) {
    return createTraceMessage(message, maxMessageLength, 1, false);
  }

  /**
   * Create a trace message with thread info.  Limits overall length to 500 characters.
   *
   * @param message text to append to the standard trace output.
   * @return
   */
  public static String traceThreadMessage(String message) {
    return createTraceMessage(message, 500, 1, true);
  }

  /**
   * Create a trace message, including thread info  Limits overall length to a given number of characters.
   *
   * @param message text to append to the standard trace output.
   * @return
   */
  public static String traceThreadMessage(String message, int maxMessageLength) {
    return createTraceMessage(message, maxMessageLength, 1, true);
  }

  /**
   * Create a trace message
   * @param message text to append to the standard trace output.
   * @param maxMessageLength length limit (0 for no limit).
   * @param depthAdjustment number of elements to remove from the stack count. (used by internal callers)
   * @param includeThreadId whether to include the thread ID (text name) in the message.
   * @return
   */
  private static String createTraceMessage(String message, int maxMessageLength, int depthAdjustment, boolean includeThreadId) {

    StringBuilder msg = new StringBuilder();
    if (includeThreadId) {
      msg.append("thread=");
      msg.append(Thread.currentThread().getId());
      msg.append(':');
    }
    msg.append("depth=");
    depthAdjustment++;  // Keep ourselves out of the count.
    msg.append(HaxeDebugUtil.getStackDepth() - depthAdjustment);
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

  public static String elementLocation(PsiElement element) {
    if (null == element) {
      return "(unknown file) : 0";
    }

    final PsiFile file = element.getContainingFile();
    final String contents = null != file ? file.getText() : null;
    final int line = null != contents ? StringUtil.offsetToLineNumber(contents, element.getTextOffset()) : 0;

    StringBuilder builder = new StringBuilder(null != file && file.isPhysical() ? file.getName() : "(virtual file)");
    builder.append(" : ");
    builder.append(line);

    return builder.toString();
  }

  /**
   * Use this exception when an invalid (e.g. default) value occurs in a switch statement.
   * Ultimately, we would rather catch this issue at compile time (that not all possible cases
   * were covered.)
   *
   * THIS IS A PROGRAMMING ERROR.
   */
  public static class InvalidCaseException extends RuntimeException {
    public InvalidCaseException() {
      super(HaxeBundle.message("invalid.case.value.unadorned"));
    }
    public InvalidCaseException(Object o) {
      super(HaxeBundle.message("invalid.case.value", o.toString()));
    }
  }

  /**
   * Use this exception when an invalid (unexpected) value occurs at runtime.
   *
   * THIS IS A PROGRAMMING ERROR.
   */
  public static class InvalidValueException extends RuntimeException {
    private InvalidValueException() {}
    public InvalidValueException(Object o) {
      super(valueMessage(o));
    }
    private static String valueMessage(Object o) {
      return HaxeBundle.message("invalid.value.detected.at.runtime", o.toString());
    }
  }

}