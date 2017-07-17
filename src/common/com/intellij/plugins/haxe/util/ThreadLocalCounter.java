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

/**
 * A counter that is thread static.  Use it like this:
 * Created by ebishton on 7/16/17.
 */
public class ThreadLocalCounter {
  static final HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
  final String debugName;

  private final ThreadLocal<Integer> counter = new ThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return 0;
    }
  };

  public ThreadLocalCounter() {
    this(HaxeDebugUtil.getCallerCanonicalName());
  }

  public ThreadLocalCounter(String debugName) {
    this.debugName = null == debugName ? "Anonymous" : debugName;
  }

  public int increment() {
    int newval = counter.get() + 1;
    counter.set(newval);
    return newval;
  }

  public int decrement() {
    int newval = counter.get() - 1;
    counter.set(newval);
    if (newval < 0) {
      LOG.debug(HaxeDebugUtil.traceThreadMessage(
        debugName + " decremented past zero by " + HaxeDebugUtil.getCallerCanonicalName()));
    }
    return newval;
  }

  public int value() {
    return counter.get();
  }

  public String toString() {
    // XXX: Output debugName here?
    return Integer.toString(counter.get());
  }
}
