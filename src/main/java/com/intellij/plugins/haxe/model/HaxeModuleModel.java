package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.util.HaxeNamedSubComponentUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HaxeModuleModel implements HaxeCommonMembersModel {

  public final HaxeModule module;

  private FullyQualifiedInfo myQualifiedInfo;

  public HaxeModuleModel(HaxeModule module) {
    this.module = module;
  }

  @Override
  public List<HaxeModel> getExposedMembers() {
      List<HaxeModel> list = new ArrayList<>();
      for (HaxeNamedComponent component : HaxeNamedSubComponentUtil.getNamedComponentsInModule(module)) {
          if (component instanceof HaxeModelTarget haxeModelTarget) {
              HaxeModel model = haxeModelTarget.getModel();
              list.add(model);
          }
      }
      return list;
  }

  private ArrayList<HaxeModel> findFieldsAndMethods() {
    ArrayList<HaxeModel> members = new ArrayList<>();
    List<? extends HaxeNamedComponent> namedComponents = PsiTreeUtil.getChildrenOfAnyType(module, HaxeModuleFieldDeclaration.class, HaxeModuleMethodDeclaration.class);
    for (HaxeNamedComponent declaration : namedComponents) {
      if (declaration instanceof HaxeModuleFieldDeclaration fieldDeclaration) {
          members.add(fieldDeclaration.getModel());
      } else if (declaration instanceof HaxeModuleMethodDeclaration methodDeclaration) {
          members.add(methodDeclaration.getModel());
      }
    }
    return members;
  }

  @Override
  public String getName() {
    return getShortName();
  }

  public String getShortName() {
    PsiFile containingFile = module.getContainingFile();
    String fileName = containingFile.getName();
    if(fileName.endsWith(".hx")) {
      fileName = fileName.substring(0, fileName.length() -3);
    }
    return fileName;
  }

  public String getQName() {
    String packageName = getPackageName();
    if(packageName.isEmpty()) return  getName();
    return packageName + "." + getName();
  }

  public String getPackageName() {
    HaxePackageStatement type = PsiTreeUtil.getChildOfType(module.getContainingFile(), HaxePackageStatement.class);
    if (type == null) return ""; // no package statement is either an error or just root/default
    return type.getPackageName();
  }

  @Override
  public @NotNull PsiElement getBasePsi() {
    return module;
  }

  @Override
  public HaxeExposableModel getExhibitor() {
    return HaxeFileModel.fromElement(module.getContainingFile());
  }

  @Override
  public @Nullable FullyQualifiedInfo getQualifiedInfo() {
    if (myQualifiedInfo == null) {
      HaxeExposableModel exhibitor = getExhibitor();
      if (exhibitor != null) {
        FullyQualifiedInfo containerInfo = exhibitor.getQualifiedInfo();
        if (containerInfo != null) {
          myQualifiedInfo = new FullyQualifiedInfo(containerInfo.packagePath, containerInfo.moduleName, null, null);
        }
      }
    }
    return myQualifiedInfo;
  }

  @Override
  public boolean isValid() {
    return module.isValid();
  }

  public HaxeMethodModel getMethod(String name, @Nullable HaxeGenericResolver resolver) {
    if(name == null) return null;
    List<HaxeNamedComponent> components = getAllHaxeNamedComponents(HaxeComponentType.METHOD);
    HaxeNamedComponent match = ContainerUtil.find(components, component -> name.equals(component.getName()));
    if (match == null) return null;
    return (HaxeMethodModel)HaxeBaseMemberModel.fromPsi(match);

  }
  @Override
  public HaxeFieldModel getField(String name, @Nullable HaxeGenericResolver resolver) {
    if(name == null) return null;
    List<HaxeNamedComponent> components = getAllHaxeNamedComponents(HaxeComponentType.FIELD );
    HaxeNamedComponent match = ContainerUtil.find(components, component -> name.equals(component.getName()));
    if (match == null) return null;
    return (HaxeFieldModel)HaxeBaseMemberModel.fromPsi(match);
  }

  public HaxeBaseMemberModel getMember(String name, @Nullable HaxeGenericResolver resolver) {
    if(name == null) return null;
    final List<HaxeNamedComponent> allNamedComponents = HaxeNamedSubComponentUtil.getNamedComponentsInModule(module);
    HaxeNamedComponent match = ContainerUtil.find(allNamedComponents, component -> Objects.equals(name, component.getName()));
    if (match == null) return null;
    return HaxeBaseMemberModel.fromPsi(match);
  }

  @Nullable
  public HaxeClassModel getClass(String name) {
    if(name == null) return null;
    List<HaxeNamedComponent> allNamedComponents = getAllHaxeNamedComponents(HaxeComponentType.CLASS );
    HaxeNamedComponent match = ContainerUtil.find(allNamedComponents, component -> Objects.equals(name, component.getName()));
    if (match  instanceof HaxeClass haxeClass) return haxeClass.getModel();
    return null;
  }
  public List<HaxeClassModel> getClasses() {
      List<@NotNull HaxeClassModel> list = new ArrayList<>();
      for (HaxeNamedComponent namedComponent : getAllHaxeNamedComponents(HaxeComponentType.CLASS)) {
          if (namedComponent instanceof HaxeClass haxeClass) {
            list.add(haxeClass.getModel());
          }
      }
      return list;
  }


  @NotNull
  public List<HaxeNamedComponent>getAllHaxeNamedComponents(HaxeComponentType componentType) {
    final List<HaxeNamedComponent> allNamedComponents = HaxeNamedSubComponentUtil.getNamedComponentsInModule(module);
    return HaxeNamedSubComponentUtil.filterNamedComponentsByType(allNamedComponents, componentType);
  }

  @Nullable
    public HaxeClassModel getMainClass() {
        return getClass(getName());
    }
}
