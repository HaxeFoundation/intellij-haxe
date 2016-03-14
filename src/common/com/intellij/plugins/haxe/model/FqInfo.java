/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.model;

import com.google.common.base.Joiner;
import com.intellij.plugins.haxe.model.util.HaxeNameUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class FqInfo {
  public final String packagePath;
  public final String fileName;
  public final String className;

  public FqInfo(String packagePath, String fileName, String className) {
    this.packagePath = packagePath;
    this.fileName = fileName;
    this.className = className;
  }

  @Nullable
  static public FqInfo parse(String fqName) {
    ArrayList<String> packageParts = new ArrayList<String>();
    ArrayList<String> classParts = new ArrayList<String>();
    String[] parts = StringUtils.split(fqName, '.');
    boolean inClassPart = false;

    for (String part : parts) {
      if (HaxeNameUtils.isValidClassName(part)) {
        classParts.add(part);
        inClassPart = true;
      }
      else if (HaxeNameUtils.isValidPackageName(part)) {
        if (inClassPart) {
          return null;
        }
        packageParts.add(part);
      }
      else {
        return null;
      }
    }

    if (classParts.isEmpty() || classParts.size() > 2) {
      return null;
    }

    String className = "";
    String fileName = "";
    if (classParts.size() >= 1) {
      className = classParts.get(classParts.size() - 1);
      fileName = classParts.get(classParts.size() - ((classParts.size() >= 2) ? 2 : 1));
    }

    return new FqInfo(Joiner.on('.').join(packageParts), fileName, className);
  }

  @Override
  public String toString() {
    return "FqInfo(" + packagePath + ":" + fileName + ":" + className + ")";
  }
}
