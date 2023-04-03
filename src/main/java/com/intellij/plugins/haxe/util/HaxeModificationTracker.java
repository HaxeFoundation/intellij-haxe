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


import lombok.CustomLog;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Track modifications to an entity.
 *
 * This class is intended to be used in two ways:
 *
 *  - to keep track of modifications by creating "stamps" that can be captured and
 *    compared (for uniqueness) against another stamp.  This is used in caches and
 *    whatnot to quickly check whether the state of an object has changed and the
 *    cache needs to be invalidated.  A lightweight and lazy approach to tracking
 *    cohesion as opposed to a Listener interface.
 *
 *  - (Not yet implemented) to act as a Listener registrant and notifier.
 *    TODO: Add the listener interface to this.
 *
 * Created by ebishton on 9/7/2017.
 */
@CustomLog
public class HaxeModificationTracker {

  // XXX: Maybe add a "disposed" state and set it true with the underlying object is no longer valid/used?

  private AtomicInteger updateCounter = new AtomicInteger(0);
  private final String debugName;
  private static int debugCount = 0;

  public HaxeModificationTracker(String debugName) {
    debugCount++;
    this.debugName = debugName + "(" + Integer.toString(debugCount) + ")";
  }

  public Stamp getStamp() {
    return new Stamp(updateCounter.get());
  }

  public boolean isModifiedSince(HaxeTrackedModifiable.Stamp s) {
    if (s instanceof HaxeModificationTracker.Stamp) {
      return isModifiedSince((HaxeModificationTracker.Stamp)s);
    }
    return true;  // invalid stamp; always modified.
  }

  public boolean isModifiedSince(Stamp stamp) {
    if (null == stamp) {
      return true;  // invalid stamp; always modified.
    }

    if (stamp.tracker != this) {
      String msg = "Stamps can only be compared to their allocating Tracker.";
      log.error(msg);
    }

    return this != stamp.tracker || updateCounter.get() != stamp.value;
  }

  public void notifyUpdated() {
    updateCounter.incrementAndGet();
  }

  public class Stamp implements HaxeTrackedModifiable.Stamp {
    private final int value;
    private final HaxeModificationTracker tracker;

    private Stamp(int value) {
      this.value = value;
      this.tracker = HaxeModificationTracker.this;
    }

    public boolean isOutOfDate() {
      return tracker.isModifiedSince(this);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Stamp stamp = (Stamp)o;

      if (value != stamp.value) return false;
      return tracker.equals(stamp.tracker);
    }

    @Override
    public int hashCode() {
      int result = value;
      result = 31 * result + tracker.hashCode();
      return result;
    }
  }
}
