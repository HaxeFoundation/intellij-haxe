package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class HaxeAnonymousTypeModel extends HaxeClassModel {

  public final HaxeAnonymousType anonymousType;

  public HaxeAnonymousTypeModel(@NotNull HaxeAnonymousType anonymousType) {
    super(anonymousType);
    this.anonymousType = anonymousType;
  }

  //TODO consolidate composite types (`{> type, ...}`) and extension types (`typeX & typeY`)
  public List<ResultHolder> getCompositeTypes() {
    List<HaxeType> typeList  = getCompositeTypesPsi();
    return typeList.stream().map(HaxeTypeResolver::getTypeFromType).toList();
  }
  public List<ResultHolder> getExtendsTypes() {
    List<HaxeType> typeList  = getExtendsList();
    return typeList.stream().map(HaxeTypeResolver::getTypeFromType).toList();
  }

  public  List<HaxeType> getCompositeTypesPsi() {
    return CachedValuesManager.getProjectPsiDependentCache(anonymousType, HaxeAnonymousTypeModel::_getCompositeTypes);
  }
  public  List<HaxeType> getExtensionTypesPsi() {
    return CachedValuesManager.getProjectPsiDependentCache(anonymousType, HaxeAnonymousTypeModel::_getExtendsTypes);
  }
  @NotNull
  public List<HaxeType> getExtendsList() {
    List<HaxeType> extendList = new ArrayList<>(getCompositeTypesPsi());
    extendList.addAll(getExtensionTypesPsi());
    return extendList;
  }

  private static List<HaxeType> _getCompositeTypes(HaxeAnonymousType anonymousType) {
    val items = new ArrayList<HaxeType>();
    if (anonymousType != null) {
      items.addAll(anonymousType.getTypeList());
    }
    return items;
  }
  private static List<HaxeType> _getExtendsTypes(HaxeAnonymousType anonymousType) {
    val items = new ArrayList<HaxeType>();
    List<HaxeAnonymousTypeBody> bodyList = anonymousType.getAnonymousTypeBodyList();
    for (HaxeAnonymousTypeBody anonymousTypeBody : bodyList) {
      if (anonymousTypeBody != null) {
        HaxeTypeExtendsList list = anonymousTypeBody.getTypeExtendsList();
        if (list != null) items.addAll(list.getTypeList());
      }
    }
    return items;
  }

  private List<HaxeAnonymousTypeBody> getAnonymousTypeBodyList() {
    return CachedValuesManager.getProjectPsiDependentCache(anonymousType, HaxeAnonymousTypeModel::_getAnonymousTypeBodyList);
  }

  private static @NotNull List<HaxeAnonymousTypeBody> _getAnonymousTypeBodyList(HaxeAnonymousType anonymousType) {
    return anonymousType.getAnonymousTypeBodyList();
  }

  @Override
  public List<HaxeFieldModel> getFields() {
    List<HaxeFieldModel> inheritedFields = getInheritedFields();

    List<HaxeFieldModel> bodyFieldList = getAnonymousTypeBodyList().stream()
            .map(this::getFieldsFromBody)
            .flatMap(Collection::stream)
            .toList();

    ArrayList<HaxeFieldModel> fields = new ArrayList<>();
    fields.addAll(inheritedFields);
    fields.addAll(bodyFieldList);
    return fields;
  }

  private @NotNull List<HaxeFieldModel> getInheritedFields() {
    return getCompositeTypes().stream()
            .map(ResultHolder::getClassType)
            .filter(Objects::nonNull)
            .map(HaxeAnonymousTypeModel::mapToHaxeClassIfPossible)
            .filter(Objects::nonNull)
            .map(haxeClass -> haxeClass.getModel().getFields())
            .flatMap(Collection::stream)
            .toList();
  }

  private static @Nullable HaxeClass mapToHaxeClassIfPossible(SpecificHaxeClassReference reference) {
    HaxeClass haxeClass = reference.getHaxeClass();
    if (reference.isTypeDefOfClass()) {
      SpecificTypeReference resolvedRef = reference.fullyResolveTypeDefReference();
      if (resolvedRef instanceof SpecificHaxeClassReference classReference) {
        return classReference.getHaxeClass();
      }
      return null;
    }
    return haxeClass;
  }

  @Override
  public List<HaxeMethodModel> getAllMethods(@Nullable HaxeGenericResolver resolver) {
    return getMethods(resolver);
  }
  public List<HaxeMethodModel> getMethods(@Nullable HaxeGenericResolver resolver) {
    List<HaxeMethodModel> inheritedMethods = getInheritedMethods(resolver);

    List<HaxeMethodModel> bodyFieldList = getAnonymousTypeBodyList().stream()
            .map(this::getMethodsFromBody)
            .flatMap(Collection::stream)
            .toList();

    ArrayList<HaxeMethodModel> fields = new ArrayList<>();
    fields.addAll(inheritedMethods);
    fields.addAll(bodyFieldList);
    return fields;
  }

  @NotNull
  private List<HaxeMethodModel> getInheritedMethods(@Nullable HaxeGenericResolver resolver) {
    return getCompositeTypes().stream()
            .map(ResultHolder::getClassType)
            .filter(Objects::nonNull)
            .map(HaxeAnonymousTypeModel::mapToHaxeClassIfPossible)
            .filter(Objects::nonNull)
            .map(haxeClass -> haxeClass.getModel().getMethods(resolver))
            .flatMap(Collection::stream)
            .toList();
  }

  @Override
  public @Nullable HaxeBaseMemberModel getMember(String name, @Nullable HaxeGenericResolver resolver) {
    return getAllMembers(resolver).stream().filter(model -> model.getName().equals(name)).findFirst().orElse(null);
  }

  public List<HaxeBaseMemberModel> getAllMembers(@Nullable HaxeGenericResolver resolver) {
    return getMembers(resolver);
  }
  @Override
  public List<HaxeBaseMemberModel> getMembers(@Nullable HaxeGenericResolver resolver) {
    List<HaxeFieldModel> fields = getFields();
    List<HaxeMethodModel> methods = getMethods(resolver);

    ArrayList<HaxeBaseMemberModel> memberModels = new ArrayList<>();
    memberModels.addAll(fields);
    memberModels.addAll(methods);
    return memberModels;
  }

  private List<HaxeFieldModel> getFieldsFromBody(HaxeAnonymousTypeBody body) {
    if (body != null) {
      List<HaxeFieldModel> list = new ArrayList<>();
      List<HaxePsiField> children = PsiTreeUtil.getChildrenOfAnyType(body, HaxeFieldDeclaration.class, HaxeAnonymousTypeField.class, HaxeEnumValueDeclarationField.class);
      for (HaxePsiField field : children) {
        HaxeFieldModel model = (HaxeFieldModel)field.getModel();
        list.add(model);
      }

      List<HaxeAnonymousTypeFieldList> anonymList = PsiTreeUtil.getChildrenOfAnyType(body, HaxeAnonymousTypeFieldList.class);
      for (HaxeAnonymousTypeFieldList fieldList : anonymList) {
        for (HaxeAnonymousTypeField field : fieldList.getAnonymousTypeFieldList()) {
          HaxeFieldModel model = (HaxeFieldModel)field.getModel();
          list.add(model);
        }
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
