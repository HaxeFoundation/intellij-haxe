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

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.resolver.HaxeResolver2File;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class HaxeFileModel {
  public final HaxeFile file;

  public HaxeFileModel(HaxeFile file) {
    this.file = file;
  }

  public String getName() {
    return file.getName();
  }

  static public HaxeFileModel fromElement(PsiElement element) {
    if (element instanceof PsiFile) return new HaxeFileModel((HaxeFile)element);
    return new HaxeFileModel((HaxeFile)element.getContainingFile());
  }

  @Nullable
  public HaxePackageStatement getPackagePsi() {
    return UsefulPsiTreeUtil.getChild(file, HaxePackageStatement.class);
  }

  public HaxePackageModel getPackage() {
    return getProject().getPackageFromFile(file);
  }

  public HaxeDocumentModel getDocument() {
    return HaxeDocumentModel.fromFile(file);
  }

  public HaxeImportsModel getImports() {
    return new HaxeImportsModel(this);
  }

  @Nullable
  public HaxeClassModel getHaxeClass(String name) {
    List<HaxeClassModel> classes = getHaxeClasses();
    for (HaxeClassModel clazz : classes) {
      if (clazz.getName().equals(name)) return clazz;
    }

    return null;
  }

  public List<HaxeClassModel> getHaxeClasses() {
    ArrayList<HaxeClassModel> classes = new ArrayList<HaxeClassModel>();
    for (PsiElement element : file.getChildren()) {
      if (element instanceof HaxeClass) {
        classes.add(((HaxeClass)element).getModel());
      }
    }
    return classes;
  }

  public HaxeProjectModel getProject() {
    return HaxeProjectModel.fromElement(file);
  }

  public HaxeModuleModel getModule() {
    return HaxeModuleModel.fromElement(file);
  }

  public HaxeResolver2File getResolver() {
    return new HaxeResolver2File(this);
  }
}
