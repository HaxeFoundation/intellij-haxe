package com.intellij.plugins.haxe.model.evaluator.assign;

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFunctionType;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterScope;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.CustomLog;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.model.evaluator.assign.HaxeAbstractAssignUtil.isMultiType;
import static com.intellij.plugins.haxe.model.evaluator.assign.HaxeAbstractAssignUtil.isTransitive;
import static com.intellij.plugins.haxe.model.evaluator.assign.HaxeClassAssignUtil.*;
import static com.intellij.plugins.haxe.model.evaluator.assign.HaxeAnonymousAssignUtil.*;
import static com.intellij.plugins.haxe.model.evaluator.assign.HaxeTypeCompatible.canAssignToFromEvaluation;
import static com.intellij.plugins.haxe.model.type.HaxeMacroTypeUtil.isRestClassType;


@CustomLog
public class HaxeAssignEvaluation {

  public String explanation;

  public boolean result = false;
  public boolean completed = false;

  private SpecificTypeReference to;
  private SpecificTypeReference from;

  private final PsiElement toContext;
  private final PsiElement fromContext;

  @Getter
  private final AssignEvaluationSettings config;

  public final AssignExplanation explanations = new AssignExplanation();

  private List<HaxeAssignEvaluation> subEvaluations = new ArrayList<>();

  public HaxeAssignEvaluation(@NotNull ResultHolder to, @NotNull ResultHolder from, @NotNull AssignEvaluationSettings config) {
    this.config = config;
    if(config.contravariance()) {
      this.to = from.getType();
      this.from = to.getType();
    }else {
      this.to = to.getType();
      this.from = from.getType();
    }
    // contexts used to check if we are in a scope with macro keyword
    toContext = to.getElementContext();
    fromContext = from.getElementContext();
  }

  public void fullyResolveTypes() {
    boolean toMacroScope = isMacroScope(toContext);
    boolean fromMacroScope = isMacroScope(fromContext);

    to = fullyResolve(to, toMacroScope);
    from = fullyResolve(from, fromMacroScope);
  }

  private static boolean isMacroScope(PsiElement context) {
    if (context instanceof HaxeMethodDeclaration methodDeclaration) {
      HaxeMethodModel model = methodDeclaration.getModel();
      return model.isMacro();
    }
    return false;
  }

  @NotNull
  private static SpecificTypeReference fullyResolve(SpecificTypeReference typeReference, boolean macroScope) {
    if (typeReference instanceof SpecificHaxeClassReference classReference) {
      return classReference.fullyResolveTypeDefAndUnwrapNullTypeReference(macroScope);
    }
    else {
      return typeReference;
    }
  }

  /**
   * basic assign rules (checks stuff like Dynamic,  and Enum / enumValue)
   * NOTE: use strict flag for typeParameter checks (prevents dynamic assign to known type)
   */
  public void testBasicAssignRules(boolean strict) {
    if (canAssignSimple(strict)) {
      complete(true, "Basic rules allows assign");
    }
  }


  /**
   * A set of the most basic rules
   * - strict flag will not allow dynamic from values to be assigned to a more specific to value. (used when working with typeParameters)
   */
  private boolean canAssignSimple(boolean strict) {
    if(strict) {
      if(!to.isDynamic() && from.isDynamic()) return false;
    }else {
      if (to.isEnumValueClass() && (from.isEnumValue() || from.isEnumReference())) return true;
      // arrays and maps can be assigned to an empty collection `[]`. This expression defaults to array<Unknown>
      // so to make sure it also works for maps we add a simple rule we do a little workaround here to make sure it also works for maps
      if (to.isMapType() && from.isEmptyLiteralCollection()) return true;
    }
    if (to.isExpr() || from.isExpr()) return true;
    if(to.isDynamic() || from.isDynamic()) return true;
    if (to.isUnknown() || from.isUnknown()) return true;
    if (to.isFunction() && from instanceof SpecificFunctionReference) return true;
    // hack: we allow assign to typeParameter without constraints to  simplify stuff when it comes to constructors and enums.
    if (to.isTypeParameter() && !to.isTypeParameterWithConstraints()) return true;

    // hack: workaround to ignore constraints for method typeParameters checking functionType compatibility
    if (from.isTypeParameterWithConstraints()) {
      PsiElement elementContext = from.getElementContext();
      HaxeFunctionType functionType = PsiTreeUtil.getParentOfType(elementContext, HaxeFunctionType.class);
      if (functionType != null) {
        if (from instanceof SpecificHaxeClassReference classReference) {
          if (classReference.getHaxeClass() instanceof HaxeTypeParameterDeclaration typeParameter) {
            HaxeTypeParameterScope typeParameterScope = typeParameter.getTypeParameterScope();
            if (typeParameterScope == HaxeTypeParameterScope.METHOD) return true;
          }
        }
      }
    }

    return false;
  }


  public void testClassAssignRules() {

    if (to instanceof SpecificHaxeClassReference toClassReference && from instanceof SpecificHaxeClassReference fromClassReference) {
      HaxeClassModel toModel = toClassReference.getHaxeClassModel();
      HaxeClassModel fromModel = fromClassReference.getHaxeClassModel();

      if (toModel == null) {
        log.warn("Unable to evaluate class assign due to missing model (code:"+to.context.getText()+")");
        complete(false, "model(s) missing");
        this.explanations.addMissingModel(toClassReference.getClassName());
        return;
      }
      if (fromModel == null) {
        log.warn("Unable to evaluate class assign due to missing model (code:"+from.context.getText()+")");
        this.explanations.addMissingModel(fromClassReference.getClassName());
        complete(false, "model(s) missing");
        return;
      }

      if(toModel.isEnum() || fromModel.isEnum()) return;

      if (sameTypeCheck(this, toClassReference, fromClassReference)) {
        complete(true, "class match");
        return;
      }

      if (testClassHierarchyAssign(this, toClassReference, fromClassReference)) {
        complete(true, "class hierarchy match");
        return;
      }
      testClassTypeParameterConstraints(toClassReference, toModel, fromClassReference, fromModel, config.contravariance(), config.ignoreFromConstraints());
    }
  }

  private void testClassTypeParameterConstraints(SpecificHaxeClassReference toClassReference, HaxeClassModel toModel,
                                                 SpecificHaxeClassReference fromClassReference, HaxeClassModel fromModel,
                                                 boolean contravariance, boolean ignoreFromConstraints) {
    if (toModel instanceof  HaxeGenericParamModel model) {
      ResultHolder constraint = model.getConstraint(toClassReference.getGenericResolver());
      if (constraint == null) {
        complete(true, "No constraints for type parameter");
      }
      else {
        //TODO mlo: pass context / forward returned explanation
        boolean canAssignConstraint = HaxeTypeCompatible.canAssignToFromReference(constraint, from.createHolder());
        if (canAssignConstraint) {
          complete(true, "TypeParameter constraints can assign");
        }
        else {
          complete(false, "TypeParameter constraints can NOT assign");
        }
      }
    }
    if (!ignoreFromConstraints) {
      if (fromModel instanceof HaxeGenericParamModel model) {
        ResultHolder constraint = model.getConstraint(fromClassReference.getGenericResolver());
        if (constraint == null) {
          complete(true, "No constraints for type parameter");
        } else {
          // if from type is a type parameter with constraints we should allow it to be assigned to any  type that matches the constraint.
          boolean canAssignConstraint;
          if (contravariance) {
            canAssignConstraint = HaxeTypeCompatible.canAssignToFromReference(constraint, to.createHolder());
          } else {
            canAssignConstraint = HaxeTypeCompatible.canAssignToFromReference(to.createHolder(), constraint);

          }
          if (canAssignConstraint) {
            complete(true, "TypeParameter constraints can assign");
          } else {
            complete(false, "TypeParameter constraints can NOT assign");
          }
        }
      }
    }
  }





  //

  /**
   * check enum-enum  (including enum-abstract variants, but excluding abstract of enum in abstract method)
   *  Evaluates assign of enum types and enum members. (for enumValueClass assign se basicAssign rules)
   *
   *  checks:
   *  Enum = Enum
   *  Enum = Enum.EnumValue
   */
  public void testEnumAssignRules() {
    if (to instanceof SpecificHaxeClassReference toClassReference) {

      if (from instanceof SpecificHaxeClassReference fromClassReference) {
        HaxeClassModel toModel = toClassReference.getHaxeClassModel();
        HaxeClassModel fromModel = fromClassReference.getHaxeClassModel();

        // if both are enum Type References do sameType check
        if (toModel instanceof HaxeEnumModel && fromModel instanceof HaxeEnumModel) {
          if (sameTypeCheck(this, toClassReference, fromClassReference)) {
            complete(true, "Enum types matches");
          }
        }

      }else if (from instanceof SpecificEnumValueReference enumValueReference ) {

        HaxeEnumValueModel valueModel = enumValueReference.getModel();
        HaxeClassModel anEnum = valueModel.getDeclaringEnum();
        if (sameTypeCheck(this, toClassReference, anEnum.getInstanceReference())) {
          complete(true, "Enum value belongs to enum type");
        }
      }
    }

  }

  /**
   * checks if we can assign a method or function to a function signature
   * Note: The "Function" type is an abstract  (with @:callable) and is handled in abstract rules
   */
  public void testFunctionAssignRules() {
    // function - function ( Function(abstract type)  is handled in abstract method (@:callable))
    if (to instanceof SpecificFunctionReference toFunctionReference && from instanceof SpecificFunctionReference fromFunctionReference ) {
      if(canAssignToFromFunction(toFunctionReference, fromFunctionReference)) {
        complete(true, "FunctionType can assign");
      }
    }
  }

  static private boolean canAssignToFromFunction(
    @NotNull SpecificFunctionReference to,
    @NotNull SpecificFunctionReference from
  ) {

    List<HaxeArgument> toArguments = getArgumentsWithoutVoid(to);
    List<HaxeArgument> fromArguments = getArgumentsWithoutVoid(from);

    int toArgSize = toArguments.size();
    int fromArgSize = fromArguments.size();

    if (toArgSize != fromArgSize) {
      return false;
    }

    for (int n = 0; n < toArgSize; n++) {
      HaxeArgument fromArg = fromArguments.get(n);
      HaxeArgument toArg = toArguments.get(n);

      if (toArg.getType().isClassType() && toArg.getType().isMissingClassModel()) continue;
      if (!toArg.getType().isUnknown()) {
        // TO can accept optional but not the other way around.
        // if TO has optional from and  FROM does not then the assignment should fail.
        if (!fromArg.isOptional() && toArg.isOptional()) return false;

        ResultHolder fromArgType = fromArg.getType();
        ResultHolder toArgtype = toArg.getType();

        if (fromArg.isRest() && toArg.isRest()) {
          if (isRestClassType(fromArgType.getType())) {
            fromArgType = tryExtractRestType(fromArgType);
          }
          if (isRestClassType(toArgtype.getType())) {
            toArgtype = tryExtractRestType(toArgtype);
          }
        }
        boolean argCompatibility = HaxeTypeCompatible.canAssignToFromContravariance(toArgtype, fromArgType, true, false);
        if (!argCompatibility) {
          return false;
        }
      }
    }

    // Void return on the "to" function just means that the value isn't used/cared about. See
    // the Haxe manual, section 3.5.4 at https://haxe.org/manual/type-system-unification-function-return.html
    return to.returnValue == null || (to.returnValue.isVoid() || to.returnValue.canAssign(from.returnValue));
  }

  private static @NotNull ResultHolder tryExtractRestType(ResultHolder fromArgType) {
    SpecificHaxeClassReference classType = fromArgType.getClassType();
    if(classType == null) return fromArgType;
    @NotNull ResultHolder[] specifics = classType.getSpecifics();
    if(specifics.length != 1) return fromArgType;
    return specifics[0];
  }

  private static @NotNull List<HaxeArgument> getArgumentsWithoutVoid(@NotNull SpecificFunctionReference to) {
      List<HaxeArgument> list = new ArrayList<>();
      for (HaxeArgument argument : to.arguments) {
          if (!argument.isVoid()) {
              list.add(argument);
          }
      }
      return list;
  }

  // checks if anonymous type contains all members (also checks  @:struct (constructor))
  public void testAnonymousAssignRules() {
    if (to instanceof SpecificHaxeClassReference toClassReference && from instanceof SpecificHaxeClassReference fromClassReference) {
      HaxeClassModel toModel = toClassReference.getHaxeClassModel();
      if (toModel != null) {
        if(toModel.isAnonymous() || toModel.isObjectLiteral() || toModel.isStructInit() ) {
          if(containsAllMembers(toClassReference, fromClassReference, this)) {
            complete(true, "all members in anonymous structure found");
            return;
          }
        }
        // if struct does not match all members check constructor
        if (toModel.isStructInit()) {
          if(checkStructInitConstructor(toClassReference, fromClassReference, this)) {
            complete(true, "all members in struct found");
            return;
          }
        }
      }
    }
  }
  // if to target is typeParameter with constraints, extract constraint and test
  public void testTypeParameterConstraints(boolean checkDirectCasts, boolean checkImplicitCasts) {
    if (to instanceof SpecificHaxeClassReference toClassReference) {
      // check if to is a typeParameter constraint and extract constraint before checking enums
      if (toClassReference.getHaxeClassModel() instanceof HaxeGenericParamModel model) {
        ResultHolder constraint = model.getConstraint(toClassReference.getGenericResolver());
        if (constraint != null) {
          SpecificHaxeClassReference constraintClassType = constraint.getClassType();
          if (constraintClassType != null) {
            HaxeAssignEvaluation constraintAssign = canAssignToFromEvaluation(constraintClassType.createHolder(), from.createHolder(), false, checkDirectCasts, checkImplicitCasts);
            if(constraintAssign.result) {
              complete(true, "typeParameter constraint could assign");
            }
          }
        }else {
            complete(true, "No constraints for type parameter");
        }
      }
    }
    if (from instanceof SpecificHaxeClassReference fromClassReference) {
      if (fromClassReference.getHaxeClassModel() instanceof HaxeGenericParamModel model) {
        ResultHolder constraint = model.getConstraint(fromClassReference.getGenericResolver());
        if (constraint != null) {
          SpecificHaxeClassReference constraintClassType = constraint.getClassType();
          if (constraintClassType != null) {
            HaxeAssignEvaluation constraintAssign = canAssignToFromEvaluation(constraintClassType.createHolder(), to.createHolder(), false, checkDirectCasts, checkImplicitCasts);
            if(constraintAssign.result) {
              complete(true, "typeParameter constraint could assign");
            }
          }
        }else {
          complete(true, "No constraints for type parameter");
        }
      }
    }

  }


  private static final RecursionGuard<PsiElement> implicitCastRecursionGuard = RecursionManager.createGuard("implicitCastRecursionGuard");
  private static final RecursionGuard<PsiElement> directCastRecursionGuard = RecursionManager.createGuard("directCastRecursionGuard");

  /**
   * Checks direct cast (to/from), implicit casts (@:to / @:From), @:transient, @:multivalue etc
   * Note: abstracts can be of all kinds of types(class, function enum, anonymous structures etc.) and can also be cased to these types
   * So there's a lot to check for here.
   */
  public void testAbstractAssignRules(boolean checkDirectCasts, boolean checkImplicitCasts) {
    if (to instanceof SpecificHaxeClassReference toClassReference && from instanceof SpecificHaxeClassReference fromClassReference ) {

      HaxeClassModel toModel = toClassReference.getHaxeClassModel();
      HaxeClassModel fromModel = fromClassReference.getHaxeClassModel();

      if (toModel == null || fromModel == null) {
        log.warn("Unable to evaluate abstract assign due to missing model(s)");
        complete(true, "model(s) missing");
        return;
      }

      boolean fromIsAbstract = fromClassReference.isAbstractType();
      boolean toIsAbstract = toClassReference.isAbstractType();

      if(!fromIsAbstract && !toIsAbstract) return;

      if(fromIsAbstract && toIsAbstract) {
        // is from abstract (same type check)
        if (sameTypeCheck(this, toClassReference, fromClassReference)) {
          complete(true, "Abstract type match");
          return;
        }
      }

      boolean transitiveFrom = isTransitive(fromClassReference);
      boolean isMultiType =  isMultiType(fromClassReference); // if multi type, use underlying instead of  current,  to avoid recursion ?
      // match check (to/from and @:to/@:from)

      if(isMultiType) {
        SpecificTypeReference fromType = fromModel.getUnderlyingType();
        HaxeGenericResolver genericResolver = fromClassReference.getGenericResolver();

        // make sure we fully resolve underlying type before trying to resolve typeParameters
        if (fromType instanceof SpecificHaxeClassReference classReference && classReference.isTypeDef()) {
          fromType = classReference.fullyResolveTypeDefReference();
        }
        if (fromType instanceof SpecificHaxeClassReference classReference) {
          HaxeClass targetClass = classReference.getHaxeClass();
          HaxeClass sourceClass = fromClassReference.getHaxeClass();
          HaxeGenericResolver tmp = genericResolver.translateFromTo(sourceClass, targetClass);
          fromType = fromModel.getUnderlyingClassReference(tmp);
        }

        // TODO the correct way is probably checking all the @:to methods but for now we just accept any
        if (fromType!= null && fromType.canAssign(toClassReference)) {
          complete(true, "Abstract (multiType-shortcut) match");
          return;
        }
      }


      if(fromIsAbstract) {

        if(checkDirectCasts) {
          List<SpecificTypeReference> directCasts = fromClassReference.getDirectCastToTypes();
          for (SpecificTypeReference directCastType : directCasts) {
            if (HaxeTypeCompatible.canAssignToFromReference(toClassReference, directCastType, true, false)) {
              complete(true, "Abstract (to) direct cast match");
              return;
            }
          }
        }
        if(checkImplicitCasts) {
          List<SpecificTypeReference> implicitCasts = fromClassReference.getImplicitCastToTypes(toClassReference);
          for (SpecificTypeReference implicitCast : implicitCasts) {
            if (HaxeTypeCompatible.canAssignToFromReference(toClassReference, implicitCast)) {
              complete(true, "Abstract (@:to) implicit cast match");
              return;
            }
          }
        }

          // when  from is different target check from casts
          // is from Function, (to/from and @:to/@:from)
          // is from Enum (to/from and @:to/@:from)
          // is from class (to/from and @:to/@:from)

      }
      if (toIsAbstract) {

        if(checkDirectCasts) {
          Boolean match = canAssignUsingDirectCastFrom(toClassReference, fromClassReference);
          if (match == Boolean.TRUE) return;
        }
        if(checkImplicitCasts) {
          Boolean match = canAssignUsingImplicitCastFrom(toClassReference, fromClassReference);
          if (match == Boolean.TRUE) return;
        }

        //Hack
        // workaround for abstracts with explicit from dynamic (ignoring checkImplicitCasts)
        // this workaround is here so that we dont need to make special logic for typeParameters with abstracts
        // that contains "from Dynamic" like for instance Any.
        // TODO look into @:forward.variance
        SpecificTypeReference underlyingType = toModel.getUnderlyingType();
        if(underlyingType != null && fullyResolve(underlyingType, false).isDynamic()) {

          boolean hasDirectCastFromDynamic = toClassReference.getDirectCastFromTypes().stream()
                  .filter(SpecificHaxeClassReference.class::isInstance)
                  .map(SpecificHaxeClassReference.class::cast)
                  .map(SpecificHaxeClassReference::fullyResolveTypeDefAndUnwrapNullTypeReference)
                  .anyMatch(SpecificTypeReference::isDynamic);

          if (hasDirectCastFromDynamic) {
            complete(true, "Abstract has direct from Dynamic cast");
          }
        }


        // when  to is different target check to casts
        // is from Function, (to/from and @:to/@:from)
        // is from Enum (to/from and @:to/@:from)
        // is from class (to/from and @:to/@:from)
      }
    }

    // check function rules
    else if (to instanceof SpecificHaxeClassReference toClassReference) {
      boolean toIsAbstract = toClassReference.isAbstractType();
      if (toIsAbstract) {
        if (checkDirectCasts) {
          Boolean match = canAssignUsingDirectCastFrom(toClassReference, from);
          if (match == Boolean.TRUE) return;
        }
        if (checkImplicitCasts) {
          Boolean match = canAssignUsingImplicitCastFrom(toClassReference, from);
          if (match == Boolean.TRUE) return;
        }
      }
    }
    else if (from instanceof SpecificHaxeClassReference fromClassReference) {
      boolean fromIsAbstract = fromClassReference.isAbstractType();
      if(fromIsAbstract) {

        //TODO extract to method  to avoid duplication
        if (checkDirectCasts) {
          List<SpecificTypeReference> directCasts = fromClassReference.getDirectCastToTypes();
          for (SpecificTypeReference directCastType : directCasts) {
            if (HaxeTypeCompatible.canAssignToFromReference(to, directCastType, true, false)) {
              complete(true, "Abstract (to) direct cast match");
              return;
            }
          }
        }
        if (checkImplicitCasts) {
          List<SpecificTypeReference> implicitCasts = fromClassReference.getImplicitCastToTypes(to);
          for (SpecificTypeReference implicitCast : implicitCasts) {
            if (HaxeTypeCompatible.canAssignToFromReference(to, implicitCast)) {
              complete(true, "Abstract (@:to) implicit cast match");
              return;
            }
          }
        }
      }
    }


  }

  /**
   *  toReference(assign target) is abstract, find @:from and see if any of them accepts the fromreference Type
   */
  private @Nullable Boolean canAssignUsingImplicitCastFrom(SpecificHaxeClassReference toClassReference, SpecificTypeReference fromClassReference) {
    return implicitCastRecursionGuard.computePreventingRecursion(toClassReference.context, true, () -> {
      List<SpecificTypeReference> implicitCasts = toClassReference.getImplicitCastFromTypes(fromClassReference);
      for (SpecificTypeReference implicitCast : implicitCasts) {
        boolean transitive = isTransitive(fromClassReference);
        if (HaxeTypeCompatible.canAssignToFromReference(implicitCast, fromClassReference, true, transitive)) {
          complete(true, "Abstract (from) implicit cast match");
          return true;
        }
      }
      return false;
    });
  }

  /**
   * toReference (assign target) is abstract, check from-types from the abstract declaration and see if any of them accepts the fromReference type
   */
  private @Nullable Boolean canAssignUsingDirectCastFrom(SpecificHaxeClassReference toClassReference, SpecificTypeReference fromClassReference) {
    return directCastRecursionGuard.computePreventingRecursion(toClassReference.context, true, () -> {
      List<SpecificTypeReference> directCasts = toClassReference.getDirectCastFromTypes();
      for (SpecificTypeReference directCastType : directCasts) {
        // direct casts  can be "chained" (ex. Int -> Float -> Single)
        if (HaxeTypeCompatible.canAssignToFromReference(directCastType, fromClassReference, true, false)) {
          complete(true, "Abstract (from) direct cast match");
          return true;
        }
      }
      return false;
    });
  }


  static boolean canAssignTypeParameters(HaxeAssignEvaluation context, @NotNull ResultHolder[] toSpecifics, @NotNull ResultHolder[] fromSpecifics) {
   return canAssignTypeParameters(context,toSpecifics,fromSpecifics, false);
  }
  static boolean canAssignTypeParameters(HaxeAssignEvaluation context, @NotNull ResultHolder[] toSpecifics, @NotNull ResultHolder[] fromSpecifics, boolean ignoreFromConstraints) {
    if (toSpecifics.length != fromSpecifics.length) return false;
    for (int i = 0, length = toSpecifics.length; i < length; i++) {
      ResultHolder toSpecific = toSpecifics[i];
      ResultHolder fromSpecific = fromSpecifics[i];
      if (!HaxeTypeCompatible.canAssignToFromTypeParameter(context, toSpecific, fromSpecific, ignoreFromConstraints)) {
        return false;
      }
    }
    return true;
  }

  public void addChild(HaxeAssignEvaluation evaluation) {
    subEvaluations.add(evaluation);
  }


  public void complete(boolean result, String explanation) {
    this.explanation = explanation;
    this.result = result;
    completed = true;
  }

  public recursionGuardKey recursionGuardKey() {
    return new recursionGuardKey(toContext, fromContext);
  }

  private record recursionGuardKey(PsiElement to, PsiElement from) {

  }
}
