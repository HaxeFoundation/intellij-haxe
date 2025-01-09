package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeAnonymousTypeModel;
import com.intellij.plugins.haxe.model.HaxeConstraintTypeListModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

//  TODO mlo:
// This class probably needs or should override a methods  in AbstractHaxePsiClass, but we should probably
// start by cleaning up AbstractHaxePsiClass, move methods and instead use interfaces.

public abstract class HaxeConstraintTypeListImplMixin extends AbstractHaxePsiClass  implements HaxeAnonymousType {
  public HaxeConstraintTypeListImplMixin(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public List<HaxeType> getHaxeExtendsList() {
    // TODO fix
    HaxeConstraintTypeListModel model = (HaxeConstraintTypeListModel) getModel();
//    return model.getExtensionTypesPsi();
    return List.of(); // TODO
  }

  @Override
  @NotNull
  public PsiClass[] getSupers() {
    HaxeAnonymousTypeModel model = (HaxeAnonymousTypeModel) getModel();
    return model.getExtendsTypes().stream()
      .map(ResultHolder::getClassType)
      .filter(Objects::nonNull)
      .map(SpecificHaxeClassReference::getHaxeClass)
      .filter(Objects::nonNull)
      .toArray(PsiClass[]::new);
  }

  @Override
  public HaxeComponentName getComponentName() {
    return null;
  }

  @Override
  public HaxeGenericParam getGenericParam() {
    // generic param does not contain direct children of its own type
    return null;
  }

  @Nullable
  @Override
  public PsiIdentifier getNameIdentifier() {
    //TODO fix
    return new HaxeIdentifierImpl(new HaxeDummyASTNode("Tmp Name TODO", HaxeConstraintTypeListImplMixin.this.getProject())) {
      @NotNull
      @Override
      public Project getProject() {
        return ((HaxeDummyASTNode)getNode()).getProject();
      }
    };
  }

  @Override
  public boolean isAnonymousType() {
    return true;
  }

  @NotNull
  public List<HaxeAnonymousTypeBody> getAnonymousTypeBodyList() {
    return List.of();
  }

  @NotNull
 public List<HaxeType> getTypeList() {
    return List.of();
  }

}
