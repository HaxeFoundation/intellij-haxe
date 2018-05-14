/*
 * Copyright 2017-2018 Ilya Malanin
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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

public class FullyQualifiedInfo {
  public static final char PATH_SEPARATOR = '.';

  final public String packagePath;
  final public String fileName;
  final public String className;
  final public String memberName;

  public FullyQualifiedInfo(String packagePath, @Nullable String fileName, @Nullable String className, @Nullable String memberName) {
    this.packagePath = packagePath;
    this.fileName = fileName;
    this.className = className;
    this.memberName = memberName;
  }

  public FullyQualifiedInfo(@Nullable String fullyQualifiedIdentifier) {
    this(new ArrayList<>(fullyQualifiedIdentifier != null
                         ? Arrays.asList(StringUtils.split(fullyQualifiedIdentifier, PATH_SEPARATOR))
                         : Collections.emptyList()));
  }

  public FullyQualifiedInfo(@Nullable HaxeReferenceExpression referenceExpression) {
    this(referenceExpression != null ? referenceExpression.getText() : null);
  }

  private FullyQualifiedInfo(ArrayList<String> parts) {
    StringBuilder packagePathBuilder = new StringBuilder();

    int i = 0;
    int size = parts.size();
    while (i < size) {
      String identifier = parts.get(i);
      if (identifier == null) {
        packagePath = null;
        fileName = null;
        memberName = null;
        className = null;
        return;
      }
      if (!Character.isLowerCase(identifier.charAt(0))) {
        break;
      }
      if (i > 0) packagePathBuilder.append(PATH_SEPARATOR);
      packagePathBuilder.append(identifier);

      i++;
    }

    packagePath = packagePathBuilder.toString();
    fileName = i < size ? parts.get(i++) : null;

    if (fileName == null) {
      className = null;
      memberName = null;
    } else {
      final String classOrMemberName = i < size ? parts.get(i++) : null;
      if (classOrMemberName != null && Character.isLowerCase(classOrMemberName.charAt(0))) {
        memberName = classOrMemberName;
        className = fileName;
      } else {
        className = classOrMemberName;
        memberName = i < size ? parts.get(i) : null;
      }
    }
  }

  public static String getRelativeQualifiedName(HaxeModel model, HaxeModel relativeModel) {
    FullyQualifiedInfo relativeInfo = relativeModel.getQualifiedInfo();
    FullyQualifiedInfo methodInfo = model.getQualifiedInfo();
    if (relativeInfo != null &&
        methodInfo != null &&
        Objects.equals(relativeInfo.packagePath, methodInfo.packagePath) &&
        Objects.equals(relativeInfo.fileName, methodInfo.fileName)) {
      final String relativeMethodPath = relativeInfo.getPresentableText();
      final String methodPath = methodInfo.getPresentableText();
      int commonPrefixLength = StringUtil.commonPrefixLength(relativeMethodPath, methodPath, true);
      if (commonPrefixLength > 0) {
        if (relativeMethodPath.charAt(commonPrefixLength-1) != '.') {
          commonPrefixLength = relativeMethodPath.substring(0, commonPrefixLength).lastIndexOf('.')+1;
        }
        return methodPath.substring(commonPrefixLength);
      }
    }
    if (methodInfo != null) return methodInfo.getPresentableText();
    return model.getName();
  }

  public static String getQualifiedName(HaxeModel model) {
    FullyQualifiedInfo info = model.getQualifiedInfo();
    if (info != null) {
      return info.getPresentableText();
    }

    return model.getName();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (packagePath != null && !packagePath.isEmpty()) {
      builder.append(packagePath);
    }

    if (fileName == null || fileName.isEmpty()) return builder.toString();
    if (builder.length() > 0) builder.append(PATH_SEPARATOR);
    builder.append(fileName);

    if (className != null && !className.isEmpty()) {
      builder.append(PATH_SEPARATOR);
      builder.append(className);
    }

    if (memberName != null && !memberName.isEmpty()) {
      builder.append(PATH_SEPARATOR);
      builder.append(memberName);
    }

    return builder.toString();
  }

  public String getPresentableText() {
    StringBuilder builder = new StringBuilder();
    if (packagePath != null && !packagePath.isEmpty()) {
      builder.append(packagePath);
    }

    if (fileName == null || fileName.isEmpty()) return builder.toString();
    if (builder.length() > 0) builder.append(PATH_SEPARATOR);
    builder.append(fileName);

    if (className != null && !className.isEmpty() && !className.equals(fileName)) {
      builder.append(PATH_SEPARATOR);
      builder.append(className);
    }

    if (memberName != null && !memberName.isEmpty()) {
      builder.append(PATH_SEPARATOR);
      builder.append(memberName);
    }

    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;

    if (obj instanceof FullyQualifiedInfo) {
      FullyQualifiedInfo equalsObject = (FullyQualifiedInfo)obj;

      return getPresentableText().equals(equalsObject.getPresentableText());
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(packagePath, fileName, className, memberName);
  }

  public String getClassPath() {
    return getFilePath() + PATH_SEPARATOR + className;
  }

  public String getFilePath() {
    String result = packagePath;

    if (result.isEmpty()) {
      result = fileName;
    } else {
      result += PATH_SEPARATOR + fileName;
    }

    return result;
  }

  public FullyQualifiedInfo toPackageQualifiedName() {
    return new FullyQualifiedInfo(this.packagePath, null, null, null);
  }

  public boolean equalsToNamedPart(String name) {
    return equalsToMemberName(name) || equalsToClassName(name) || equalsToFileName(name);
  }

  private boolean equalsToMemberName(String name) {
    return memberName != null && memberName.equals(name);
  }

  private boolean equalsToClassName(String name) {
    return memberName == null && className != null && className.equals(name);
  }

  private boolean equalsToFileName(String name) {
    return className == null && fileName != null && fileName.equals(name);
  }
}
