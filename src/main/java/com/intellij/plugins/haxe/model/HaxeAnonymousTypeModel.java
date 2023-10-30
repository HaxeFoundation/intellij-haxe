package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HaxeAnonymousTypeModel extends HaxeClassModel {

  public final HaxeAnonymousType anonymousType;

  public HaxeAnonymousTypeModel(@NotNull HaxeAnonymousType anonymousType) {
    super(anonymousType);
    this.anonymousType = anonymousType;
  }

  public List<ResultHolder> getCompositeTypes() {
    List<HaxeType> typeList  = CachedValuesManager.getProjectPsiDependentCache(anonymousType, HaxeAnonymousTypeModel::_getCompositeTypes);
    return typeList.stream().map(HaxeTypeResolver::getTypeFromType).toList();
  }

  private static List<HaxeType> _getCompositeTypes(HaxeAnonymousType anonymousType) {
    return anonymousType.getTypeList();
  }

  public List<HaxeAnonymousTypeBody> getAnonymousTypeBodyList() {
    return CachedValuesManager.getProjectPsiDependentCache(anonymousType, HaxeAnonymousTypeModel::_getAnonymousTypeBodyList);
  }

  private static @NotNull List<HaxeAnonymousTypeBody> _getAnonymousTypeBodyList(HaxeAnonymousType anonymousType) {
    return anonymousType.getAnonymousTypeBodyList();
  }

  @Override
  public List<HaxeFieldModel> getFields() {
    List<HaxeFieldModel> inheritedFields = getCompositeTypes().stream()
      .map(ResultHolder::getClassType).filter(Objects::nonNull)
      .filter(SpecificHaxeClassReference::isTypeDefOfClass)
      .map(classReference -> classReference.resolveTypeDefClass().getHaxeClassModel().getFields())
      .flatMap(Collection::stream)
      .toList();

    List<HaxeFieldModel> bodyFieldList = getAnonymousTypeBodyList().stream()
      .map(this::getFieldsFromBody)
      .flatMap(Collection::stream)
      .toList();

    ArrayList<HaxeFieldModel> fields = new ArrayList<>();
    fields.addAll(inheritedFields);
    fields.addAll(bodyFieldList);
    return fields;
  }

  @Override
  public List<HaxeMethodModel> getMethods(@Nullable HaxeGenericResolver resolver) {
    List<HaxeMethodModel> inheritedFields = getCompositeTypes().stream()
      .map(ResultHolder::getClassType)
      .filter(Objects::nonNull)
      .filter(SpecificHaxeClassReference::isTypeDefOfClass)
      .map(classReference -> classReference.resolveTypeDefClass().getHaxeClassModel().getMethods(resolver))
      .flatMap(Collection::stream)
      .toList();

    List<HaxeMethodModel> bodyFieldList = getAnonymousTypeBodyList().stream()
      .map(this::getMethodsFromBody)
      .flatMap(Collection::stream)
      .toList();

    ArrayList<HaxeMethodModel> fields = new ArrayList<>();
    fields.addAll(inheritedFields);
    fields.addAll(bodyFieldList);
    return fields;
  }

  @Override
  public List<HaxeMemberModel> getMembers(@Nullable HaxeGenericResolver resolver) {
    List<HaxeFieldModel> fields = getFields();
    List<HaxeMethodModel> methods = getMethods(resolver);

    ArrayList<HaxeMemberModel> memberModels = new ArrayList<>();
    memberModels.addAll(fields);
    memberModels.addAll(methods);
    return memberModels;
  }

  private List<HaxeFieldModel> getFieldsFromBody(HaxeAnonymousTypeBody body) {
    if (body != null) {
      List<HaxeFieldModel> list = new ArrayList<>();
      List<HaxePsiField> children = PsiTreeUtil.getChildrenOfAnyType(body, HaxeFieldDeclaration.class, HaxeAnonymousTypeField.class, HaxeEnumValueDeclaration.class);
      for (HaxePsiField field : children) {
        HaxeFieldModel model = (HaxeFieldModel)field.getModel();
        list.add(model);
      }
      return list;
    } else {
      return Collections.emptyList();
    }
  }
  private List<HaxeMethodModel> getMethodsFromBody(HaxeAnonymousTypeBody body) {
    if (body != null) {
      List<HaxeMethodModel> list = new ArrayList<>();
      List<HaxeMethodDeclaration> children = PsiTreeUtil.getChildrenOfAnyType(body, HaxeMethodDeclaration.class);
      for (HaxeMethodDeclaration methodDeclaration : children) {
        HaxeMethodModel model = methodDeclaration.getModel();
        list.add(model);
      }
      return list;
    } else {
      return Collections.emptyList();
    }
  }

}
