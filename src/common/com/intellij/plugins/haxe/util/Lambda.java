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
 * A small wrapper class used to wrap a bit of functionality that should be
 * applied to some collection that requires this interface.
 *
 * This is usually to allow an iteration over a collection that has specific
 * iteration constraints (such as being synchronized) -- or simply to create
 * a functional interface.
 */
public abstract class Lambda<V> {
  /**
   * Process a single entry in the collection.
   *
   * @param item An element of type &lt;V&gt; to be processed.
   * @return Generally, true if the loop should keep running, false if not. This
   *         is up to the iterator calling this interface.
   */
  public abstract boolean process(V item);
}
