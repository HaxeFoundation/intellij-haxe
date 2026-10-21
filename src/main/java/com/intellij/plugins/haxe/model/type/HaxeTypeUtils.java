package com.intellij.plugins.haxe.model.type;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.IMAP;

public class HaxeTypeUtils {


    public static boolean containsUnknownOrUnresolvedTypes(ResultHolder holder) {
        if(holder.isUnknown()) return true;
        if(holder.isFunctionType()) {
            return containsUnknownOrUnresolvedTypeParameters(holder) || containsUnknownOrUnresolvedTypeParameters(holder.getFunctionType());
        }else if(holder.isTypeParameterWithConstraints()){
            return !hasNoGenericsOrTheOnlyGenericTypeisItSelf(holder, holder);
        }else{
            return containsUnknownOrUnresolvedTypeParameters(holder);
        }
    }

    public static boolean containsUnknownOrUnresolvedTypeParameters(ResultHolder holder) {
        if (holder.isUnknown()) return  false;
        if (holder.isTypeParameter()) return true;
        SpecificTypeReference type = holder.getType();
        if (type instanceof  SpecificHaxeClassReference classReference) {
            for (ResultHolder specific : classReference.getSpecifics()) {
                // ignore unknown if in Dynamic
                if(specific.isDynamic() && containsUnknownOrUnresolvedTypeParameters(specific)) return false;
                if (specific.isUnknown() || containsUnknownOrUnresolvedTypeParameters(specific)) return  true;
            }
        }
        if (type instanceof SpecificFunctionReference  function) {
            List<ResultHolder> parameters = function.getTypeParameters();
            for (ResultHolder parameter : parameters) {
                if(parameter.isUnknown()) return true;
            }
        }
        return false;
    }

    public static boolean hasNoGenericsOrTheOnlyGenericTypeisItSelf(@NotNull ResultHolder selfType, @NotNull ResultHolder currentType) {
        if (currentType.isUnknown()) return  false;
        // plain typeParameter OK
        if (currentType.isTypeParameter() && !currentType.isTypeParameterWithConstraints() ) return true;
        if(currentType.isOrContainsTypeParameters()) {
            SpecificTypeReference type = currentType.getType();
            if (type instanceof SpecificHaxeClassReference classReference) {
                for (ResultHolder specific : classReference.getSpecifics()) {
                    if (specific.isUnknown() || hasNoGenericsOrTheOnlyGenericTypeisItSelf(selfType, specific)) return true;
                }
            }
            if (type instanceof SpecificFunctionReference function) {
                List<ResultHolder> parameters = function.getTypeParameters();
                for (ResultHolder parameter : parameters) {
                    if (parameter.isUnknown()) return true;
                }
            }
            if(currentType.getType().isSameType(selfType.getType())) return true;
        }

        return true;
    }


    public static boolean containsUnknownTypes(ResultHolder holder) {
        if(holder.isUnknown()) return true;
        if(holder.isFunctionType()) {
            return containsUnknownTypeParameters(holder) || containsUnknownTypes(holder.getFunctionType());
        }else if(holder.isTypeParameterWithConstraints()){
            return !hasNoGenericsOrTheOnlyGenericTypeisItSelf(holder, holder);
        }else{
            return containsUnknownTypeParameters(holder);
        }
    }

    public static boolean containsUnknownTypeParameters(ResultHolder holder) {
        if (holder.isUnknown()) return  false;
        if (holder.isTypeParameter()) return false;
        SpecificTypeReference type = holder.getType();
        if (type instanceof  SpecificHaxeClassReference classReference) {
            for (ResultHolder specific : classReference.getSpecifics()) {
                // ignore unknown if in Dynamic
                if(specific.isDynamic() && containsUnknownTypeParameters(specific)) return false;
                if (specific.isUnknown() || containsUnknownTypeParameters(specific)) return  true;
            }
        }
        if (type instanceof SpecificFunctionReference  function) {
            List<ResultHolder> parameters = function.getTypeParameters();
            for (ResultHolder parameter : parameters) {
                if(parameter.isUnknown()) return true;
            }
        }
        return false;
    }

    public static boolean containsUnknownOrUnresolvedTypeParameters(SpecificFunctionReference functionReference) {
        if(functionReference == null) return false;
        for (HaxeArgument argument : functionReference.getArguments()) {
            ResultHolder argumentType = argument.getType();
            if(argumentType.isUnknown() || argumentType.containsUnknownOrUnresolvedTypes()) return true;
        }

        ResultHolder type = functionReference.getReturnType();
        return type.isUnknown() || type.containsUnknownOrUnresolvedTypes();
    }

    public static boolean containsUnknownTypes(SpecificFunctionReference functionReference) {
        if(functionReference == null) return false;
        for (HaxeArgument argument : functionReference.getArguments()) {
            ResultHolder argumentType = argument.getType();
            if(argumentType.isUnknown() || argumentType.containsUnknownTypes()) return true;
        }

        ResultHolder type = functionReference.getReturnType();
        return type.isUnknown() || type.containsUnknownTypes();
    }

    public static boolean isOrContainsTypeParameters(ResultHolder holder) {
        if (holder.isUnknown()) return  false;
        if (holder.isTypeParameter()) return true;
        SpecificTypeReference type = holder.getType();
        if (type instanceof  SpecificHaxeClassReference classReference) {
            for (ResultHolder specific : classReference.getSpecifics()) {
                if (specific.getType() != type && isOrContainsTypeParameters(specific)) return  true;
            }
        }
        if (type instanceof SpecificFunctionReference  function) {
            return !function.getTypeParameters().isEmpty();
        }
        return false;
    }

    public record MapKeyValueTypes(@NotNull ResultHolder key, @NotNull ResultHolder value) {}

    /**
     * The key and value types of a map type: the parameters of its array-access
     * setter, else of set() when the class implements IMap. Null for other types.
     */
    @Nullable
    public static MapKeyValueTypes tryFindMapKeyValueTypes(SpecificTypeReference reference) {
        if (reference instanceof SpecificHaxeClassReference classReference) {
            reference = classReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
        }

        if (reference instanceof SpecificHaxeClassReference classReference) {

            HaxeClass haxeClass = classReference.getHaxeClass();
            if (haxeClass == null) return null;

            HaxeGenericResolver resolver = classReference.getGenericResolver();
            HaxeMethodModel methodModel = findMapsArrayAccessOrSetter(haxeClass, resolver);
            if (methodModel == null) return null;
            List<HaxeParameterModel> parameters = methodModel.getParameters();
            if (parameters.size() != 2) return null;

            ResultHolder key = parameters.get(0).getType(resolver);
            ResultHolder value = parameters.get(1).getType(resolver);
            return new MapKeyValueTypes(key, value);
        }
        return null;
    }

    @Nullable
    private static HaxeMethodModel findMapsArrayAccessOrSetter(@NotNull HaxeClass haxeClass, @NotNull HaxeGenericResolver resolver) {
        HaxeNamedComponent arrayAccessSetter = haxeClass.findArrayAccessSetter(resolver);
        if (arrayAccessSetter instanceof HaxeMethodDeclaration declaration) return declaration.getModel();

        if (!implementsIMap(haxeClass)) return null;
        HaxeBaseMemberModel setMember = haxeClass.getModel().getMember("set", resolver);
        return setMember instanceof HaxeMethodModel methodModel ? methodModel : null;
    }

    private static boolean implementsIMap(@NotNull HaxeClass haxeClass) {
        if (IMAP.equals(haxeClass.getFullyQualifiedName())) return true;
        return haxeClass.getHaxeImplementsList().stream()
          .map(type -> type.getReferenceExpression().resolveHaxeClass().getHaxeClass())
          .filter(Objects::nonNull)
          .anyMatch(implemented -> IMAP.equals(implemented.getFullyQualifiedName()));
    }

}
