/*
 * Copyright 2018 Ilya Malanin
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
package com.intellij.plugins.haxe;

import com.vdurmont.semver4j.Semver;

public enum HaxeLanguageLevel {
  HAXE_3(new Semver("3.4.0"), HaxeBundle.message("haxe.language.level.3")),
  HAXE_4(new Semver("4.0.0"), HaxeBundle.message("haxe.language.level.4"));

  private final Semver version;
  private final String presentableText;

  HaxeLanguageLevel(Semver version, String presentableText) {
    this.version = version;
    this.presentableText = presentableText;
  }

  public static HaxeLanguageLevel fromVersionString(String string) {
    Semver version = new Semver(string).withClearedSuffixAndBuild();
    for (HaxeLanguageLevel level : values()) {
      if (level.version.getMajor().equals(version.getMajor()) && level.version.getMinor().equals(version.getMinor())) return level;
    }
    return HAXE_3;
  }

  public String getPresentableText() {
    return presentableText;
  }
}
