package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeListPart;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeOrAnonymous;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeConstraintTypeListImpl;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// TODO mlo:
// we should probably get rid of the "extends HaxeAnonymousTypeModel" and implement things our self.
// but to do this we should probably clean up the  HaxeClassModel inheritance

@CustomLog
public class HaxeConstraintTypeListModel extends HaxeAnonymousTypeModel{

  final HaxeConstraintTypeListImpl constraintTypeList;


  public HaxeConstraintTypeListModel(@NotNull HaxeConstraintTypeListImpl constraintTypeList) {
    super(constraintTypeList);
    this.constraintTypeList = constraintTypeList;
  }
//
//  @NotNull
//  public ResultHolder getInstanceType() {
//    // single element, threat as a reference
//    List<HaxeTypeListPart> constraintTypeList = constraintPart.getConstraintTypeList();
//    if (constraintTypeList.size() == 1) {
//      HaxeTypeListPart haxeTypeListPart = constraintTypeList.get(0);
//
//      HaxeTypeOrAnonymous typeOrAnonymous = haxeTypeListPart.getTypeOrAnonymous();
//      if (typeOrAnonymous != null) {
//        return HaxeTypeResolver.getTypeFromTypeOrAnonymous(typeOrAnonymous);
//      }
//
//      HaxeFunctionType functionType = haxeTypeListPart.getFunctionType();
//      if (functionType != null) {
//        return HaxeTypeResolver.getTypeFromFunctionType(functionType);
//      }
//
//      return HaxeExpressionEvaluator.evaluate(haxeTypeListPart.getExpression(), new HaxeExpressionEvaluatorContext(haxeTypeListPart), null).result;
//    }else {
//      // multiple elements, treat as an anonymous structure
//    }
//    return getInstanceReference().createHolder();
//  }
//  public SpecificHaxeClassReference getInstanceReference() {
//    if (reference  == null) {
//      reference = SpecificHaxeClassReference.withGenerics(getReference(),getSpecifics());
//    }
//    return reference;
//  }



  // TODO
  @Override
  public Set<HaxeClassModel> getCompatibleTypes() {
    return super.getCompatibleTypes();
    //TODO
    //return getCompositeTypes().stream().map(resultHolder -> resultHolder.getType().mn)
  }

//  public List<HaxeClassReferenceModel> getExtendingTypes() {
//    getCompositeTypes().stream().filter(ResultHolder::isClassType)
//            .map(ResultHolder::getClassType)
//            .map(SpecificHaxeClassReference::getHaxeClassModel)
//            .toList()
//  }

  @Override
  public void writeCompatibleTypes(Set<HaxeClassModel> output) {
    // TODO
    super.writeCompatibleTypes(output);
  }

  public List<ResultHolder> getCompositeTypes() {
    List<HaxeTypeListPart> typeList = constraintTypeList.getConstraintTypes();
    if (typeList.isEmpty()) return List.of();// should not happen

    if(typeList.size() == 1) {
      HaxeTypeListPart first = typeList.getFirst();
      if(first.getFunctionType() != null) {
        return List.of(HaxeTypeResolver.getTypeFromFunctionType(first.getFunctionType()));
      }else if (first.getTypeOrAnonymous() != null){
        return List.of(HaxeTypeResolver.getTypeFromTypeOrAnonymous(first.getTypeOrAnonymous()));
      } else {
        log.error("Unknown type for TypeParameter constraint");
        return List.of();
      }
    }else {
      // expects only types/classes no functions here, but might e wrong
      return typeList.stream()
              .map(HaxeTypeListPart::getTypeOrAnonymous)
              .filter(Objects::nonNull)
              .map(HaxeTypeResolver::getTypeFromTypeOrAnonymous)
              .toList();
    }
    // TODO cache ?
  }
}
