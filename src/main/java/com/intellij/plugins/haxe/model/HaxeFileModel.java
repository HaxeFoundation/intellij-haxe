/*
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
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

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HaxeFileModel implements HaxeExposableModel {

  protected static final Key<HaxeFileModel> HAXE_FILE_MODEL_KEY = new Key<>("HAXE_FILE_MODEL");
  private final HaxeFile file;

  protected HaxeFileModel(@NotNull HaxeFile file) {
    this.file = file;
  }

  @Nullable
  static public HaxeFileModel fromElement(PsiElement element) {
    if (element == null) return null;

    final PsiFile file = element instanceof PsiFile ? (PsiFile)element : element.getContainingFile();
    if (file instanceof HaxeFile) {
      HaxeFileModel model = file.getUserData(HAXE_FILE_MODEL_KEY);
      if (model == null) {
        model = new HaxeFileModel((HaxeFile)file);
        file.putUserData(HAXE_FILE_MODEL_KEY, model);
      }
      return model;
    }
    return null;
  }

  @Override
  public PsiElement getBasePsi() {
    return this.file;
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    return getPackageModel();
  }

  @Nullable
  @Override
  public FullyQualifiedInfo getQualifiedInfo() {
    HaxeExposableModel container = getExhibitor();
    if (container != null) {
      FullyQualifiedInfo qualifiedInfo = container.getQualifiedInfo();
      if (qualifiedInfo != null) {
        return new FullyQualifiedInfo(qualifiedInfo.packagePath, getName(), null, null);
      }
    }
    return null;
  }

  @Nullable
  @Override
  public List<HaxeModel> getExposedMembers() {
    List<HaxeClassModel> models = getClassModels();
    List<HaxeModel> publicModels = new ArrayList<>();
    for(HaxeClassModel model : models) {
      if(model.isPublic()) publicModels.add(model);
    }
    return  publicModels;
  }

  @Nullable
  public HaxeClassModel getMainClassModel() {
    return getClassModel(getName());
  }

  @Nullable
  public HaxeClassModel getClassModel(String name) {
    Optional<HaxeModule> module = getModuleBody();

    if (module.isPresent()) {
      HaxeClass haxeClass = (HaxeClass)Arrays.stream(module.get().getChildren())
        .filter(element -> element instanceof HaxeClass && Objects.equals(name, ((HaxeClass)element).getName()))
        .findFirst()
        .orElse(null);

      return haxeClass != null ? haxeClass.getModel() : null;

    }
    return null;
  }

  @NotNull
  public Optional<HaxeModule> getModuleBody() {
    return Arrays.stream(file.getChildren())
      .filter(element -> (element instanceof HaxeModule))
      .map(element -> (HaxeModule)element)
      .findFirst();
  }
  @NotNull
  public PsiElement[] getModuleBodyChildren() {
    return getModuleBody().map(PsiElement::getChildren).orElseGet(() ->PsiElement.EMPTY_ARRAY);
  }

  @NotNull
  public HaxeFile getFile() {
    return file;
  }

  @NotNull
  public String getName() {
    return FileUtil.getNameWithoutExtension(file.getName());
  }

  @NotNull
  public String getFileName() {
    return file.getName();
  }

  @Nullable
  public HaxePackageStatement getPackagePsi() {
    return UsefulPsiTreeUtil.getChild(file, HaxePackageStatement.class);
  }

  @Nullable
  public String getPackageName() {
    HaxePackageStatement value = getPackagePsi();
    if (value != null) {
      String name = value.getPackageName();
      return name == null ? "" : name;
    }
    return detectPackageName();
  }

  public HaxeProjectModel getProject() {
    return HaxeProjectModel.fromElement(file);
  }

  public FullyQualifiedInfo getFullyQualifiedInfo() {
    return new FullyQualifiedInfo(getPackageName(), getName(), null, null);
  }

  public List<HaxeClassModel> getClassModels() {
    boolean COLLECT_USING_STREAMS = false;
    if (COLLECT_USING_STREAMS) {
      return getClassModelsStream().collect(Collectors.toList());
    } else {
      ArrayList<HaxeClassModel> list = new ArrayList<>();
      for (HaxeClass element : file.getClassList()) {
        list.add(element.getModel());
      }
      return list;
    }
  }

  public Stream<HaxeClassModel> getClassModelsStream() {
    return Arrays.stream(getModuleBodyChildren())
      .filter(element -> element instanceof HaxeClass)
      .map(element -> ((HaxeClass)element).getModel());
  }

  public List<HaxeImportStatement> getImportStatements() {
    return Arrays.stream(file.getChildren())
      .filter(element -> element instanceof HaxeImportStatement)
      .map(element -> ((HaxeImportStatement)element))
      .collect(Collectors.toList());
  }

  public List<HaxeImportModel> getImportModels() {
    return Arrays.stream(file.getChildren())
      .filter(element -> element instanceof HaxeImportStatement)
      .map(element -> ((HaxeImportStatement)element).getModel())
      .collect(Collectors.toList());
  }

  public List<HaxeUsingStatement> getUsingStatements() {
    return Arrays.stream(file.getChildren())
      .filter(element -> element instanceof HaxeUsingStatement)
      .map(element -> (HaxeUsingStatement)element)
      .collect(Collectors.toList());
  }

  public List<HaxeUsingModel> getUsingModels() {
    return Arrays.stream(file.getChildren())
      .filter(element -> element instanceof HaxeUsingStatement)
      .map(element -> ((HaxeUsingStatement)element).getModel())
      .collect(Collectors.toList());
  }
  @NotNull
  public List<HaxeImportableModel> getOrderedImportAndUsingModels() {
    PsiElement[] children = file.getChildren();
    List<HaxeImportableModel>  result = new ArrayList<>();
    for(PsiElement child : children) {

       if(child instanceof HaxeImportStatement importStatement) {
        result.add(importStatement.getModel());
      }
      else if(child instanceof HaxeUsingStatement usingStatement) {
        result.add(usingStatement.getModel());
      }

    }
    return result ;
  }

  public HaxePackageModel getPackageModel() {
    HaxeProjectModel project = HaxeProjectModel.fromElement(file);
    HaxeSourceRootModel result = null;
    for (HaxeSourceRootModel rootModel : project.getRoots()) {
      if (rootModel.contains(file)) {
        result = rootModel;
        break;
      }
    }

    if (result == null && project.getSdkRoot().contains(file)) {
      result = project.getSdkRoot();
    }

    if (result != null) {
      HaxeModel model = result.resolve(getFullyQualifiedInfo().toPackageQualifiedName());
      if (model instanceof HaxePackageModel packageModel) {
        return packageModel;
      }
    }

    return null;
  }

  public HaxeModel resolve(FullyQualifiedInfo info) {
    if (isReferencingCurrentFile(info)) {
      if (info.className == null) return this;

      HaxeClassModel classModel = getClassModel(info.className);
      if (classModel != null) {
        if (info.memberName != null) {
          return classModel.getMember(info.memberName, null);
        }
        return classModel;
      }
    }

    return null;
  }

  protected boolean isReferencingCurrentFile(FullyQualifiedInfo info) {
    return info.fileName != null && info.fileName.equals(getName());
  }

  private String detectPackageName() {
    HaxeSourceRootModel sourceRootModel = getProject().getContainingRoot(file.getContainingFile().getParent());
    if (sourceRootModel != null) {
      return StringUtils.replace(sourceRootModel.resolvePath(file.getParent()), "/", ".");
    }

    return "";
  }

  public void replaceOrCreatePackageStatement(@NotNull HaxePackageStatement statement) {
    HaxePackageStatement currentPsi = getPackagePsi();
    if (currentPsi != null) {
      currentPsi.replace(statement);
    } else {
      file.addBefore(statement, file.getFirstChild());
    }
  }

  public HaxeImportStatement addImport(String path) {
    // FIXME Move code of this helper inside model + add mode addImport methods
    return HaxeAddImportHelper.addImport(path, this.file);
  }
}