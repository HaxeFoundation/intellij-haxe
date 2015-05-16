/*
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
package com.intellij.plugins.haxe.build;

/**
 * Created by ebishton on 4/19/15.
 */
public class IdeaTarget {

  // Loads the string at compile-time from a templatized class.
  public static final String SDK_VERSION_STRING = IdeaSDKIdentifier.BUILD_IDENTIFIER;
  public static final String SDK_TYPE = IdeaSDKIdentifier.BUILD_TYPE;
  public static final int SDK_CODELINE = IdeaSDKIdentifier.BUILD_CODELINE;
  public static final int SDK_BUILD_NUMBER = IdeaSDKIdentifier.BUILD_NUMBER;
  public static final int SDK_BUILD_ITERATION = IdeaSDKIdentifier.BUILD_ITERATION;

  // Add new constants for specific requirements here.
  // All of these should be boolean constants.  They will
  // be used by the compiler to eliminate dead code, and to
  // allow non-compatible code to exist in the same file.
  //
  // Always use the most generic of the compatibility strings possible.

  public static final boolean IS_VERSION_13_COMPATIBLE = (SDK_CODELINE == 133 || SDK_CODELINE == 135);
  public static final boolean IS_VERSION_13_0_COMPATIBLE = (SDK_CODELINE == 133);
  public static final boolean IS_VERSION_13_1_COMPATIBLE = (SDK_CODELINE == 135);

  public static final boolean IS_VERSION_14_COMPATIBLE = (SDK_CODELINE == 139 || SDK_CODELINE == 141);
  public static final boolean IS_VERSION_14_0_COMPATIBLE = (SDK_CODELINE == 139);
  public static final boolean IS_VERSION_14_1_COMPATIBLE = (SDK_CODELINE == 141);

}
