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
 * Public-facing interface to objects managed by HaxeModificationTracker.
 *
 * Created by ebishton on 9/7/2017.
 */
public interface HaxeTrackedModifiable {

  Stamp INVALID_STAMP = new Stamp(){
    @Override
    public boolean isOutOfDate() {
      return true;
    }
  };

  /**
   * An object that captures the modification "state" of a tracked entity.
   */
  interface Stamp {

    /**
     * Determines whether the tracked object has been modified since the
     * stamp was taken.
     *
     * @return true if the object has been modified since the Stamp was
     *         taken; false if the object has not been modified.
     */
    boolean isOutOfDate();
  }


  /**
   * Gets a new stamp that can be used to detect whether the current state of
   * a tracked object has changed since the stamp was taken.
   *
   * Stamps acquired without intervening changes to the tracked object are
   * guaranteed equal(), but not "==".  However, using Stamp.isOutOfDate()
   * or HaxeModificationTracker.isModifiedSince(Stamp) is recommended.
   *
   * Stamps taken after intervening changes that restore an earlier state of
   * the underlying object will NOT be equivalent.  That is, a stamp does not
   * track the actual state of the object -- only whether it has been changed.
   *
   * @return a new stamp representing the current modification state.
   */
  Stamp getStamp();

  /**
   * Determines whether this HaxeTrackedModifiable has been changed since the
   * stamp was taken.
   *
   * It is a run-time error (a LOG message will be issued) to test a Stamp against
   * a HaxeModificationTracker other than the one that allocated it.
   *
   * @param s - stamp to test against.
   * @return true if the tracked object has been changed, false if not.
   */
  boolean isModifiedSince(Stamp s);
}
