package com.intellij.plugins.haxe.model.evaluator.assign;

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@CustomLog
public class HaxeAnonymousAssignUtil {

  private static RecursionGuard<PsiElement> containsMembersRecursionGuard = RecursionManager.createGuard("containsMembersRecursionGuard");

  static boolean containsAllMembers(@NotNull SpecificHaxeClassReference to,
                                    @NotNull SpecificHaxeClassReference from,
                                    @NotNull HaxeAssignEvaluation context) {
    if (to.isTypeParameter() || from.isTypeParameter() ) return false; // unable to evaluate members when type is not resolved

    // if one of the types is a Class<T>  its was probably wrapped so we unwrap to T
    if (to.isClass() && to.getSpecifics().length == 1) to = to.getSpecifics()[0].getClassType();
    if (from.isClass()&& from.getSpecifics().length == 1) from = from.getSpecifics()[0].getClassType();

    if (to == null || from == null) return false;

    HaxeClassModel toClassModel = to.getHaxeClassModel();
    HaxeClassModel fromClassModel = from.getHaxeClassModel();

    // unable to determine, consider if we should return true or false in this case
    if (toClassModel == null ||  fromClassModel == null)
      return false;

    boolean isStruct = toClassModel.isStructInit();
    boolean isObjectLiteral = fromClassModel.isObjectLiteral();

    HaxeGenericResolver toResolver = to.getGenericResolver();
    HaxeGenericResolver fromResolver = from.getGenericResolver();
    List<HaxeBaseMemberModel> toMembers = toClassModel.getAllMembers(toResolver);
    List<HaxeBaseMemberModel> fromMembers = fromClassModel.getAllMembers(fromResolver);

    boolean allMembersMatches = true;
    for (HaxeBaseMemberModel toMember : toMembers) {
      String name = toMember.getName();

      boolean memberExists = false;
      boolean optional = false;
      boolean ignored = false;

      if (toMember instanceof HaxeMethodModel methodModel){
        if(!isStruct) {
          // check for method declarations in types
          Optional<HaxeMethodModel> modelOptional = fromMembers.stream()
                  .filter(model -> model instanceof HaxeMethodModel)
                  .map(model -> (HaxeMethodModel) model)
                  .filter(mm -> methodModel.getParameters().size() == mm.getParameters().size())
                  .filter(model -> model.getNamePsi().getIdentifier().textMatches(name))
                  .findAny();

          if (modelOptional.isPresent()) {
                memberExists = true;
                HaxeMethodModel fromMember = modelOptional.get();
                PsiElement memberBasePsi = fromMember.getBasePsi();
                SpecificFunctionReference toType = methodModel.getFunctionType(toResolver);

                SpecificFunctionReference fromType = containsMembersRecursionGuard.computePreventingRecursion(memberBasePsi, false, () ->
//                  fromMember.getResultType(isObjectLiteral ? toResolver : fromResolver)
                  fromMember.getFunctionType(isObjectLiteral ? toResolver : fromResolver)
                );

                if(toType == null ||  fromType == null) {
                  log.warn("Unable to evaluate fieldType compatibility");
                  return true;
                }

                if (!toType.canAssign(fromType)) {
                  String fromText = fromMember.getPresentableText(null, isObjectLiteral ? toResolver : fromResolver);
                  String toText = toMember.getPresentableText(null, toResolver);
                  context.explanations.addWrongTypeMember(fromText, toText, memberBasePsi);
                  allMembersMatches = false;
                }
            }
        }else {
          // ignore methods in @:structInit classes
          ignored = true;
        }
      }else if (toMember instanceof HaxeFieldModel fieldModel) {
        optional = fieldModel.isOptional() || fieldModel.hasInitializer();
        ignored = fieldModel.isStatic();

        Optional<HaxeBaseMemberModel> modelOptional = fromMembers.stream()
                .filter(model -> model.getNamePsi().getIdentifier().textMatches(name))
                .findFirst();

        if (modelOptional.isPresent()) {
          memberExists = true;
            HaxeBaseMemberModel fromMember = modelOptional.get();
          PsiElement memberBasePsi = fromMember.getBasePsi();
          ResultHolder toType = containsMembersRecursionGuard.computePreventingRecursion(memberBasePsi, false, () ->
            toMember.getResultType(toResolver)
          );
          ResultHolder fromType = containsMembersRecursionGuard.computePreventingRecursion(memberBasePsi, false, () ->
            fromMember.getResultType(isObjectLiteral ? toResolver : fromResolver)
          );

          if(toType == null ||  fromType == null) {
            log.warn("Unable to evaluate fieldType compatibility");
            return true;
          }

          if (!toType.canAssign(fromType)) {
            String fromText = fromMember.getPresentableText(null, isObjectLiteral ? toResolver : fromResolver);
            String toText = toMember.getPresentableText(null, toResolver);
            context.explanations.addWrongTypeMember(fromText, toText, memberBasePsi);
            allMembersMatches = false;
          }
        }
      }else  {
        memberExists = fromMembers.stream().anyMatch(model -> model.getNamePsi().getIdentifier().textMatches(name));
      }

      if (!ignored && !memberExists && !optional){
        String missingFieldName = toMember.getPresentableText(null, toResolver);
        context.explanations.addMissingMember(missingFieldName);
        allMembersMatches = false;
      }
    }

    // check object literals for too many fields
    if (isStruct && fromClassModel.isObjectLiteral()) {
      for (HaxeBaseMemberModel member : fromMembers) {
        if(toMembers.stream().noneMatch(model -> model.getNamePsi().getIdentifier().textMatches(member.getName()))) {
          //  too many fields
          return false;
        }
      }
    }


    return allMembersMatches;
  }

  static boolean checkStructInitConstructor(SpecificHaxeClassReference to, SpecificHaxeClassReference from,
                                                    @Nullable HaxeAssignEvaluation context) {
    HaxeClassModel toModel = to.getHaxeClassModel();
    HaxeClassModel fromModel = from.getHaxeClassModel();
    if (toModel != null && fromModel != null && toModel.isStructInit() && fromModel.isObjectLiteral()) {

      // looks like haxe is mapping constructor parameter names to anonymous fields names.
      // if names mismatch we get "Object requires field" / "has extra field value"
      // note that parameters can be optional
      List<HaxeObjectLiteralMemberModel> fromMembers = fromModel.getAllMembers(from.getGenericResolver()).stream()
        .filter(model ->  model instanceof  HaxeObjectLiteralMemberModel)
        .map(HaxeObjectLiteralMemberModel.class::cast)
        .toList();
      HaxeGenericResolver toResolver = to.getGenericResolver();
      HaxeMethodModel constructor = toModel.getConstructor(toResolver);
      if (constructor == null) return false;
      boolean allMacteches = true;

      if (context != null ) {
        // remove errors from field check, we are going to add constructor errors if there are any
        context.explanations.clearErrors();
      }


      List<HaxeParameterModel> parameters = constructor.getParameters();
      for (HaxeParameterModel parameter : parameters) {
        String parameterName = parameter.getName();

        Optional<HaxeObjectLiteralMemberModel> fieldWithSameName = fromMembers.stream()
          .filter(m -> m.getNamePsi().getIdentifier().textMatches(parameterName))
          .findFirst();

        if (fieldWithSameName.isPresent()) {
          HaxeObjectLiteralMemberModel fromMemberModel = fieldWithSameName.get();
          ResultHolder fromType = ifEnumValueGetType(fromMemberModel.getResultType(toResolver));
          ResultHolder toType = ifEnumValueGetType(parameter.getType());
          ResultHolder resolved = toResolver.resolve(toType);
          if (resolved != null && !resolved.isUnknown())toType = resolved;
          if(!toType.canAssign(fromType)) {
            allMacteches = false;
            if (context != null) {
              context.explanations.addWrongTypeMember(fromMemberModel.getPresentableText(null),
                                         parameter.getPresentableText(toResolver),
                                         fromMemberModel.getNamePsi());
            }
          }
        } else {
          if (parameter.isOptional()) continue;
          allMacteches = false;
          if (context != null) {
            context.explanations.addMissingMember(parameterName);
          }
        }
      }
      return allMacteches;
    }
    return false;
  }

  private static ResultHolder ifEnumValueGetType(ResultHolder type) {
    if (type.isEnumValueType()) {
      SpecificEnumValueReference enumValueType = type.getEnumValueType();
      if (enumValueType != null) {
        return enumValueType.getEnumClass().createHolder();
      }
    }
    return type;
  }
}
