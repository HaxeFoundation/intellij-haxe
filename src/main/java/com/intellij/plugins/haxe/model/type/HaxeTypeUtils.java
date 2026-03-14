package com.intellij.plugins.haxe.model.type;

import org.jetbrains.annotations.NotNull;

import java.util.List;

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

}
