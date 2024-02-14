/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
import org.jetbrains.annotations.NotNull;

public class HaxeDebugLogUtil {

  public static @NotNull Logger getLogger(String name) {
    return com.intellij.openapi.diagnostic.Logger.getInstance(name);
  }
  public static @NotNull Logger getLogger(Class clazz) {
    return com.intellij.openapi.diagnostic.Logger.getInstance(clazz);
  }
  public static void traceAs(Logger logger, StackTraceElement frame, Object message) {
    traceAs(logger, frame, message, null);
  }

  public static void traceAs(Logger logger, StackTraceElement frame, Object message, Throwable t) {
    String msg = "Line " + frame.getLineNumber() + ":" + (null != message ? message.toString() : "<no message>");

    if (logger.isTraceEnabled()) {
      logger.trace(msg);
      if (t != null) logger.trace(t);
    }
  }
}
