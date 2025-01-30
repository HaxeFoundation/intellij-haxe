package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.util.HaxeNamedSubComponentUtil;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HaxeModuleModel implements HaxeCommonMembersModel {

  public final HaxeModule module;

  private FullyQualifiedInfo myQualifiedInfo;

  public HaxeModuleModel(HaxeModule module) {
    this.module = module;
  }

  @Override
  public List<HaxeModel> getExposedMembers() {
    ArrayList<HaxeModel> members = new ArrayList<>();
    members.addAll(findFieldsAndMethods());
    // TODO add classes enums etc
    return members;
  }

  private ArrayList<HaxeModel> findFieldsAndMethods() {
    ArrayList<HaxeModel> members = new ArrayList<>();
    for (HaxeNamedComponent declaration : PsiTreeUtil.getChildrenOfAnyType(module, HaxeFieldDeclaration.class, HaxeMethod.class)) {
      if (!(declaration instanceof PsiMember)) continue;
      if (declaration instanceof HaxeFieldDeclaration varDeclaration) {
        if (varDeclaration.isPublic() && varDeclaration.isStatic()) {
          members.add(varDeclaration.getModel());
        }
      } else {
        HaxeMethodDeclaration method = (HaxeMethodDeclaration)declaration;
        if (method.isStatic() && method.isPublic()) {
          members.add(method.getModel());
        }
      }
    }
    return members;
  }

  @Override
  public String getName() {
    HaxePackageStatement type = PsiTreeUtil.getParentOfType(module, HaxePackageStatement.class);
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
          myQualifiedInfo = new FullyQualifiedInfo(containerInfo.packagePath, containerInfo.fileName, null, null);
        }
      }
    }
    return myQualifiedInfo;
  }

  public HaxeMethodModel getMethod(String name, @Nullable HaxeGenericResolver resolver) {
    List<HaxeNamedComponent> components = getAllHaxeNamedComponents(HaxeComponentType.METHOD);
    HaxeNamedComponent match = ContainerUtil.find(components, component -> name.equals(component.getName()));
    if (match == null) return null;
    return (HaxeMethodModel)HaxeBaseMemberModel.fromPsi(match);

  }
  @Override
  public HaxeFieldModel getField(String name, @Nullable HaxeGenericResolver resolver) {
    List<HaxeNamedComponent> components = getAllHaxeNamedComponents(HaxeComponentType.FIELD );
    HaxeNamedComponent match = ContainerUtil.find(components, component -> name.equals(component.getName()));
    if (match == null) return null;
    return (HaxeFieldModel)HaxeBaseMemberModel.fromPsi(match);
  }

  public HaxeBaseMemberModel getMember(String name, @Nullable HaxeGenericResolver resolver) {

    final List<HaxeNamedComponent> allNamedComponents = HaxeNamedSubComponentUtil.getNamedComponentsInModule(module);
    HaxeNamedComponent match = ContainerUtil.find(allNamedComponents, component -> name.equals(component.getName()));
    if (match == null) return null;
    return HaxeBaseMemberModel.fromPsi(match);
  }


  @NotNull
  public List<HaxeNamedComponent>getAllHaxeNamedComponents(HaxeComponentType componentType) {
    final List<HaxeNamedComponent> allNamedComponents = HaxeNamedSubComponentUtil.getNamedComponentsInModule(module);
    return HaxeNamedSubComponentUtil.filterNamedComponentsByType(allNamedComponents, componentType);
  }

}
