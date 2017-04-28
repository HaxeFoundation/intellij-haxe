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
  private static final String SDK_VERSION_STRING = IdeaSDKIdentifier.BUILD_IDENTIFIER;
  private static final String SDK_TYPE = IdeaSDKIdentifier.BUILD_TYPE;
  private static final int SDK_CODELINE = IdeaSDKIdentifier.BUILD_CODELINE;
  private static final int SDK_BUILD_NUMBER = IdeaSDKIdentifier.BUILD_NUMBER;
  private static final int SDK_BUILD_PATCH = IdeaSDKIdentifier.BUILD_PATCH;

  // Add new constants for specific requirements here.
  // All of these should be boolean constants resolvable at compile time.
  // They will be used by the compiler to eliminate dead code, and to
  // allow non-compatible code to exist in the same file.
  //
  // Specific code inflection points (breaking changes in a codeline)
  // that require special handling should be placed here as well, perhaps
  // even documenting the change(s) that necessitated the check.
  //
  // When writing code using these constants, always use the most generic
  // of the compatibility strings possible.
  //


  public static final boolean IS_VERSION_17_2_COMPATIBLE = (SDK_CODELINE >= 172); // EAP
  public static final boolean IS_VERSION_17_1_COMPATIBLE = (SDK_CODELINE >= 171);
  // No 2017.0 release.
  public static final boolean IS_VERSION_17_COMPATIBILE= (IS_VERSION_17_1_COMPATIBLE || IS_VERSION_17_2_COMPATIBLE);

  public static final boolean IS_VERSION_16_3_COMPATIBLE = (SDK_CODELINE >= 163);
  public static final boolean IS_VERSION_16_2_COMPATIBLE = (SDK_CODELINE >= 162);
  public static final boolean IS_VERSION_16_1_COMPATIBLE = (SDK_CODELINE >= 145);
  // There was no 2016.0 release.
  public static final boolean IS_VERSTION_16_COMPATIBLE = (IS_VERSION_16_1_COMPATIBLE || IS_VERSION_16_2_COMPATIBLE || IS_VERSION_16_3_COMPATIBLE);

  public static final boolean IS_VERSION_15_0_COMPATIBLE = (SDK_CODELINE >= 143);
  public static final boolean IS_VERSION_15_COMPATIBLE = (IS_VERSION_15_0_COMPATIBLE);

  public static final boolean IS_VERSION_14_1_5_COMPATIBLE = (SDK_CODELINE > 141 ||
                                                              ( SDK_CODELINE == 141 && SDK_BUILD_NUMBER >= 2735));
  public static final boolean IS_VERSION_14_1_6_COMPATIBLE = (SDK_CODELINE > 141 ||
                                                              ( SDK_CODELINE == 141 && SDK_BUILD_NUMBER >= 3056));

  public static final boolean IS_VERSION_14_1_COMPATIBLE = (SDK_CODELINE >= 141);
  public static final boolean IS_VERSION_14_0_COMPATIBLE = (SDK_CODELINE >= 139);
  public static final boolean IS_VERSION_14_COMPATIBLE = (IS_VERSION_14_0_COMPATIBLE || IS_VERSION_14_1_COMPATIBLE);

  public static final boolean IS_VERSION_13_1_COMPATIBLE = (SDK_CODELINE >= 137);
  public static final boolean IS_VERSION_13_0_COMPATIBLE = (SDK_CODELINE >= 135);
  public static final boolean IS_VERSION_13_COMPATIBLE = (IS_VERSION_13_0_COMPATIBLE || IS_VERSION_13_1_COMPATIBLE);
}
