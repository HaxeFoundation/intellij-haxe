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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxePackageStub;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HaxeFileModel implements HaxeExposableModel {

  private final HaxeFile file;

  public HaxeFileModel(@NotNull HaxeFile file) {
    this.file = file;
  }

  @Nullable
  static public HaxeFileModel fromElement(PsiElement element) {
    if (element == null) return null;

    final PsiFile file = element instanceof PsiFile  psiFile ? psiFile: element.getContainingFile();
    if (file instanceof HaxeFile haxeFile) {
      return haxeFile.getModel();
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

  @Override
  public FullyQualifiedInfo getQualifiedInfo() {
    return new FullyQualifiedInfo(getPackageName(), getName(), null, null);
  }

  @Override
  public boolean isValid() {
    return file.isValid();
  }

  @NotNull
  @Override
  public List<HaxeModel> getExposedMembers() {
    List<HaxeModel> publicModels = new ArrayList<>();

    HaxeModule module = getModuleBody();
    if(module != null && module.getModel() instanceof HaxeModuleModel model){
      publicModels.add(model);
    }
  // NOTE: order matters there, we want module first and then main class
    HaxeClassModel mainClassModel = getMainClassModel();
    if(mainClassModel != null) {
      publicModels.add(mainClassModel);
    }


    return  publicModels;
  }
  @NotNull
  public List<HaxeModel> getModuleMembers() {
    List<HaxeModel> publicModels = new ArrayList<>();

    HaxeModule module = getModuleBody();
    if(module != null && module.getModel() instanceof HaxeModuleModel model){
      publicModels.addAll(model.getExposedMembers());
    }
    return  publicModels;
  }

  @Nullable
  public HaxeClassModel getMainClassModel() {
    return getClassModel(getName());
  }

  @Nullable
  public HaxeClassModel getClassModel(String name) {
    HaxeModule module = getModuleBody();

    if (module != null) {
      List<HaxeClass> list = PsiTreeUtil.getStubChildrenOfTypeAsList(module, HaxeClass.class);
      for (HaxeClass aClass : list) {
        if (Objects.equals(aClass.getName(), name)) {
          return aClass.getModel();
        }
      }
    }
    return null;
  }

  @Nullable
  public HaxeModule getModuleBody() {
    return file.getModule();
  }

  @NotNull
  private  List<PsiElement> getChildren() {
    return getChildren(file);
  }

  @NotNull
  private List<PsiElement> getChildren(HaxeFile file) {
    return getChildrenCached(file);
  }

  private static List<PsiElement> getChildrenCached(HaxeFile file) {
    return CachedValuesManager.getCachedValue(file, () -> {
      PsiElement[] children = file.getChildren();
      return new CachedValueProvider.Result<>(Arrays.asList(children), file);
    });
  }

  @NotNull
  public PsiElement[] getModuleBodyChildren() {
    HaxeModule body = getModuleBody();
    if (body == null) return PsiElement.EMPTY_ARRAY;
    return body.getChildren();
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
    return PsiTreeUtil.getStubChildOfType(file, HaxePackageStatement.class);
  }

  @Nullable
  public String getPackageName() {
    StubElement<?> stub = file.getStub();
    if(stub != null) {
      for (StubElement<?> element : stub.getChildrenStubs()) {
        if (element instanceof HaxePackageStub haxePackageStub) {
          return haxePackageStub.getPackageName();
        }
      }
      // if there's no package stub, then there wont be a result for getPackagePsi either
      return detectPackageName();
    }else {
      HaxePackageStatement value = getPackagePsi();
      if (value != null) {
        String name = value.getPackageName();
        return name == null ? "" : name;
      }
      return detectPackageName();
    }
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
    return getChildren().stream()
      .filter(element -> element instanceof HaxeImportStatement)
      .map(element -> ((HaxeImportStatement)element))
      .collect(Collectors.toList());
  }

  public List<HaxeImportModel> getImportModels() {
    List<HaxeImportModel> result = getChildren().stream()
      .filter(element -> element instanceof HaxeImportStatement)
      .map(element -> ((HaxeImportStatement)element).getModel())
      .collect(Collectors.toList());
    result.addAll(getFragmentStoredImportModels());
    HaxeFileModel context = getContextFileModel();
    if (context != null) {
      result.addAll(context.getImportModels());
    }
    return result;
  }

  public List<HaxeUsingStatement> getUsingStatements() {
    return getChildren().stream()
      .filter(element -> element instanceof HaxeUsingStatement)
      .map(element -> (HaxeUsingStatement)element)
      .collect(Collectors.toList());
  }

  public List<HaxeUsingModel> getUsingModels() {
    List<HaxeUsingModel> result = getChildren().stream()
      .filter(element -> element instanceof HaxeUsingStatement)
       .map(HaxeUsingStatement.class::cast)
      .map(element -> element.getModel())
      .collect(Collectors.toList());
    HaxeFileModel context = getContextFileModel();
    if (context != null) {
      result.addAll(context.getUsingModels());
    }
    return result;
  }
  @NotNull
  public List<HaxeImportableModel> getOrderedImportAndUsingModels() {
    List<HaxeImportableModel> result = new ArrayList<>();
    List<PsiElement> children = getChildrenFromStubOrPsi();
    for (PsiElement child : children) {

      if (child instanceof HaxeImportStatement importStatement) {
        result.add(importStatement.getModel());
      }
      else if (child instanceof HaxeUsingStatement usingStatement) {
        result.add(usingStatement.getModel());
      }
    }
    result.addAll(getFragmentStoredImportModels());
    HaxeFileModel context = getContextFileModel();
    if (context != null) {
      result.addAll(context.getOrderedImportAndUsingModels());
    }

    return result;
  }

  /**
   * Import models for the type names {@linkplain HaxeExpressionCodeFragment#importClass
   * added directly to an evaluate/debugger fragment} (empty for a normal file).
   * Each stored name is turned into an ordinary {@link HaxeImportModel} by
   * synthesizing an {@code import <fqn>;} statement, so it resolves through the
   * same machinery as a written import — but the statement lives outside the
   * fragment's text and thus never reaches the evaluated expression.
   */
  @NotNull
  private List<HaxeImportModel> getFragmentStoredImportModels() {
    if (!(file instanceof HaxeExpressionCodeFragment fragment)) {
      return Collections.emptyList();
    }
    Set<String> names = fragment.getImportedTypeNames();
    if (names.isEmpty()) {
      return Collections.emptyList();
    }
    List<HaxeImportModel> models = new ArrayList<>(names.size());
    for (String qualifiedName : names) {
      HaxeImportStatement statement = HaxeElementGenerator.createImportStatementFromPath(file.getProject(), qualifiedName);
      if (statement != null) {
        models.add(statement.getModel());
      }
    }
    return models;
  }

  /**
   * When this model wraps an evaluate/debugger code fragment, the model of the
   * source file the fragment was created against (its {@code context}); {@code
   * null} for a normal file. A fragment holds no import statements of its own, so
   * a bare class reference typed in the evaluate window would otherwise never
   * resolve — this lets it inherit the imports/usings in scope at the breakpoint,
   * exactly as a Java debugger code fragment inherits its context's imports, and
   * without those imports appearing in the fragment's (evaluated) text. The
   * context file is an ordinary {@link HaxeFile}, so this does not recurse.
   */
  @Nullable
  private HaxeFileModel getContextFileModel() {
    if (!(file instanceof HaxeExpressionCodeFragment)) {
      return null;
    }
    PsiElement context = file.getContext();
    HaxeFileModel contextModel = context != null ? fromElement(context) : null;
    return contextModel == this ? null : contextModel;
  }

  private @NotNull List<PsiElement> getChildrenFromStubOrPsi() {
    StubElement<?> stub = file.getStub();
    if(stub != null) {
      List<PsiElement> list = new ArrayList<>();
      for (StubElement<?> element : stub.getChildrenStubs()) {
        list.add(element.getPsi());
      }
      return list;
    }
    return getChildren();
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
      String className = info.className;
      String memberName = info.memberName;
      if (className == null) return this;

      HaxeModel member = findMember(className, memberName);
      if (member == null && info.moduleName != null) {
        // workaround to handle issue where member name values get "shifted" to the left because they look a class (starts uppercase)
        // (className becomes filename, memberName becomes className)
        // while packages should start lowercase and classes should start with uppercase,
        // it's not obvious whether  `import somePackage.SomeName.SomeOtherName;` is an import of a non-module named class in a module or a static member in a class
        // so we check  for "shifted" values
        member = findMember(info.moduleName, className);
        if(member == null) {
          member = findModuleMember(memberName);
        }
      }
      if (member != null && info.parameterName != null) {
        if (member instanceof HaxeMethodModel methodModel) {
          return methodModel.getParameterWithName(info.parameterName);
        } else {
          return null;
        }
      }
      return member;
    }
    return null;
  }

    private HaxeModel findModuleMember(String memberName) {
      HaxeModule module = getModuleBody();
      if(module != null && module.getModel() instanceof HaxeModuleModel model){
        return model.getMember(memberName, null);
      }

      return null;
    }

  private HaxeModel findMember(String className, String memberName) {
    HaxeClassModel classModel = getClassModel(className);
    if (classModel != null) {
      if (memberName != null) {
        // use self to avoid digging through inherited class members (we want what's in this file after all)
        // (also prevents stackoverflow recursion issues when  2 files uses each others members)
        return classModel.getMemberSelf(memberName, null);
      }
      return classModel;
    }

    return null;
  }

  protected boolean isReferencingCurrentFile(FullyQualifiedInfo info) {
    return info.moduleName != null && info.moduleName.equals(getName());
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