/*
 * Copyright 2017-2017 Ilya Malanin
 * Copyright 2019-2020 Eric Bishton
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

import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import lombok.Getter;
import lombok.With;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@With
public class FullyQualifiedInfo {
  public static final char PATH_SEPARATOR = '.';
  public static final String PARAMETER_SEPARATOR = "#";

  @Getter @NotNull final public String packageName;
  @Getter @Nullable final public String moduleName;
  @Getter @Nullable final public String className;
  @Getter @Nullable final public String memberName;
  @Getter @Nullable final public String parameterName;


  public boolean hasModuleName() {
    return moduleName!= null;
  }

  public boolean hasClassName() {
    return className!= null;
  }

  public boolean hasMemberName() {
    return memberName!= null;
  }

  public boolean hasParameterName() {
    return parameterName != null;
  }

  public FullyQualifiedInfo(String packageName, @Nullable String moduleName, @Nullable String className, @Nullable String memberName) {
    this.packageName = packageName;
    this.moduleName = moduleName;
    this.className = className;
    this.memberName = memberName;
    this.parameterName = null;
  }
  public FullyQualifiedInfo(String packageName, @Nullable String moduleName, @Nullable String className, @Nullable String memberName, @Nullable String parameterName) {
    this.packageName = packageName;
    this.moduleName = moduleName;
    this.className = className;
    this.memberName = memberName;
    this.parameterName = parameterName;
  }

  public FullyQualifiedInfo(@Nullable String fullyQualifiedIdentifier) {
    this(new ArrayList<>(fullyQualifiedIdentifier != null
                         ? Arrays.asList(StringUtils.split(fullyQualifiedIdentifier, PATH_SEPARATOR))
                         : Collections.emptyList()));
  }

  public FullyQualifiedInfo(@Nullable HaxeReferenceExpression referenceExpression) {
    this(referenceExpression != null ? referenceExpression.getText() : null);
  }

  public FullyQualifiedInfo(List<String> parts) {
    StringBuilder packagePathBuilder = new StringBuilder();

    int i = 0;
    int size = parts.size();
     while (i < size) {
      String identifier = parts.get(i);
      if (identifier == null) {
        packageName = null;
        moduleName = null;
        className = null;
        memberName = null;
        parameterName = null;
        return;
      }
      if (Character.isUpperCase(identifier.charAt(0))) {
        break;
      }
      if (i > 0) packagePathBuilder.append(PATH_SEPARATOR);
      packagePathBuilder.append(identifier);

      i++;
    }

    packageName = packagePathBuilder.toString();
    moduleName = i < size ? parts.get(i++) : null;

    if (moduleName == null) {
      className = null;
      memberName = null;
      parameterName = null;
    } else {
      final String classOrMemberName = i < size ? parts.get(i++) : null;
      if (classOrMemberName != null && Character.isLowerCase(classOrMemberName.charAt(0))) {
        if (classOrMemberName.contains(PARAMETER_SEPARATOR)) {
          String[] split = classOrMemberName.split(PARAMETER_SEPARATOR);
          memberName = split[0];
          parameterName = split[1];
        } else {
          memberName = classOrMemberName;
          parameterName = null;
        }
        className = moduleName;
      } else {
        className = classOrMemberName;
        String possibleMember = i < size ? parts.get(i) : null;
        if (possibleMember == null) {
          memberName = null;
          parameterName = null;
        } else if (possibleMember.contains(PARAMETER_SEPARATOR)) {
          String[] split = possibleMember.split(PARAMETER_SEPARATOR);
          memberName = split[0];
          parameterName = split[1];
        } else {
          memberName = possibleMember;
          parameterName = null;
        }
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (packageName != null && !packageName.isEmpty()) {
      builder.append(packageName);
    }

    if (moduleName == null || moduleName.isEmpty()) return builder.toString();
    if (!builder.isEmpty()) builder.append(PATH_SEPARATOR);
    builder.append(moduleName);

    if (className != null && !className.isEmpty()) {
      builder.append(PATH_SEPARATOR);
      builder.append(className);
    }

    if (memberName != null && !memberName.isEmpty()) {
      builder.append(PATH_SEPARATOR);
      builder.append(memberName);
    }
    if (parameterName != null && !parameterName.isEmpty()) {
      builder.append(PARAMETER_SEPARATOR);
      builder.append(parameterName);
    }

    return builder.toString();
  }

  public String getPresentableText() {
    return getQualifiedName(true);
  }
  public String getQualifiedName(boolean includeModule) {
    StringBuilder builder = new StringBuilder();
    if (packageName != null && !packageName.isEmpty()) {
      builder.append(packageName);
    }

    if (moduleName == null || moduleName.isEmpty()) {
      if (!includeModule)return builder.toString();
    }else {
      if (!builder.isEmpty()) builder.append(PATH_SEPARATOR);
      builder.append(moduleName);
    }

    if (className != null && !className.isEmpty() && (includeModule || !className.equals(moduleName))) {
      builder.append(PATH_SEPARATOR);
      builder.append(className);
    }

    if (memberName != null && !memberName.isEmpty()) {
      builder.append(PATH_SEPARATOR);
      builder.append(memberName);
    }

    if (parameterName != null && !parameterName.isEmpty()) {
      builder.append(PARAMETER_SEPARATOR);
      builder.append(parameterName);
    }

    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;

    if (obj instanceof FullyQualifiedInfo equalsObject) {
      return getQualifiedName(false).equals(equalsObject.getQualifiedName(false));
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(packageName, moduleName, className, memberName);
  }

  public String getClassPath() {
    return getFilePath() + PATH_SEPARATOR + className;
  }

  public String getFilePath() {
    String result = packageName;

    if (result.isEmpty()) {
      result = moduleName;
    } else {
      result += PATH_SEPARATOR + moduleName;
    }

    return result;
  }

  public FullyQualifiedInfo toPackageQualifiedName() {
    return new FullyQualifiedInfo(this.packageName, null, null, null);
  }
  // avoiding full path to class (MyPackage.MyClass.Myclass, where Myclass is both module name and classname)
  public String toShortendImportReferenceString() {
    if (moduleName.equals(className)) {
      return new FullyQualifiedInfo(packageName, moduleName, null, memberName).toString();
    }
    return toString();
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
    return className == null && moduleName != null && moduleName.equals(name);
  }

    public FullyQualifiedInfo toClassQualifiedName() {
        return this.withMemberName(null).withParameterName(null);
    }
}
