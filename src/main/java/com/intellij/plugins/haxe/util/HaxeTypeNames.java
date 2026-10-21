package com.intellij.plugins.haxe.util;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Names for a value known by its type: raw lowerCamel words, best first. */
public final class HaxeTypeNames {
  /** The conventional short name of a value of each standard type, by the type's simple name. */
  private static final Map<String, String> CONVENTIONAL_TYPE_NAMES = Map.of(
    "Dynamic", "obj",
    "Void", "v",
    "Int", "i",
    "Bool", "b",
    "Float", "f",
    "String", "str",
    "Array", "arr",
    "Map", "map");
  private static final String FUNCTION_VALUE_NAME = "func";
  private static final String EXCEPTION_VALUE_NAME = "e";
  private static final String IMPLEMENTATION_SUFFIX = "Impl";
  private static final String EXCEPTION_SUFFIX = "Exception";

  private HaxeTypeNames() {
  }

  /**
   * Names for a value of the type: the conventional short name of a standard
   * type, the plural of an array's element type ({@code users}), a map's key
   * and value types combined ({@code stringIntMap}), and the type's own name.
   * Typedefs and {@code Null<T>} are looked through.
   */
  @NotNull
  public static List<String> of(@Nullable ResultHolder holder) {
    if (holder == null || holder.isUnknown() || holder.isInvalid()) return List.of();
    return of(unwrapped(holder.getType()));
  }

  /** Names from a type known only by its simple name, as a compiler answer gives it. */
  @NotNull
  public static List<String> ofTypeName(@Nullable String typeName, boolean isFunction) {
    if (isFunction) return List.of(FUNCTION_VALUE_NAME);
    if (typeName == null || typeName.isEmpty()) return List.of();
    Set<String> names = new LinkedHashSet<>();
    String conventional = CONVENTIONAL_TYPE_NAMES.get(typeName);
    if (conventional != null) names.add(conventional);
    names.add(StringUtil.decapitalize(typeName));
    return new ArrayList<>(names);
  }

  /** The conventional short name of a value of the type, or null when the type gives no lead. */
  @Nullable
  public static String conventionalName(@NotNull SpecificTypeReference type) {
    if (type.isDynamic()) return CONVENTIONAL_TYPE_NAMES.get("Dynamic");
    if (type.isVoid()) return CONVENTIONAL_TYPE_NAMES.get("Void");
    if (type.isInt()) return CONVENTIONAL_TYPE_NAMES.get("Int");
    if (type.isBool()) return CONVENTIONAL_TYPE_NAMES.get("Bool");
    if (type.isFloat()) return CONVENTIONAL_TYPE_NAMES.get("Float");
    if (type.isString()) return CONVENTIONAL_TYPE_NAMES.get("String");
    if (type.isFunction()) return FUNCTION_VALUE_NAME;
    return null;
  }

  @NotNull
  private static List<String> of(@NotNull SpecificTypeReference type) {
    Set<String> names = new LinkedHashSet<>();
    String conventional = conventionalName(type);
    if (conventional != null) names.add(conventional);
    if (!(type instanceof SpecificHaxeClassReference classType)) return new ArrayList<>(names);
    if (type.isArray()) {
      names.addAll(pluralElementNames(classType));
      names.add(CONVENTIONAL_TYPE_NAMES.get("Array"));
      return new ArrayList<>(names);
    }
    if (type.isMapType()) {
      String keyed = keyValueMapName(classType);
      if (keyed != null) names.add(keyed);
      names.add(CONVENTIONAL_TYPE_NAMES.get("Map"));
      return new ArrayList<>(names);
    }
    String typeName = simpleName(classType);
    if (typeName == null) return new ArrayList<>(names);
    names.add(StringUtil.decapitalize(typeName));
    if (typeName.endsWith(EXCEPTION_SUFFIX)) names.add(EXCEPTION_VALUE_NAME);
    return new ArrayList<>(names);
  }

  /** {@code users} for {@code Array<User>}, {@code ints} for {@code Array<Int>}. */
  @NotNull
  private static List<String> pluralElementNames(@NotNull SpecificHaxeClassReference arrayType) {
    ResultHolder[] specifics = arrayType.getSpecifics();
    if (specifics.length != 1) return List.of();
    String element = simpleNameOf(specifics[0]);
    return element == null ? List.of() : List.of(StringUtil.pluralize(StringUtil.decapitalize(element)));
  }

  /** {@code stringIntMap} for {@code Map<String, Int>}. */
  @Nullable
  private static String keyValueMapName(@NotNull SpecificHaxeClassReference mapType) {
    ResultHolder[] specifics = mapType.getSpecifics();
    if (specifics.length != 2) return null;
    String key = simpleNameOf(specifics[0]);
    String value = simpleNameOf(specifics[1]);
    if (key == null || value == null) return null;
    return StringUtil.decapitalize(key) + StringUtil.capitalize(value) + "Map";
  }

  /** The simple name of the type, typedefs and {@code Null<T>} looked through, {@code Impl} dropped. */
  @Nullable
  private static String simpleNameOf(@NotNull ResultHolder holder) {
    if (holder.isUnknown() || holder.isInvalid()) return null;
    SpecificTypeReference type = unwrapped(holder.getType());
    return type instanceof SpecificHaxeClassReference classType ? simpleName(classType) : null;
  }

  @Nullable
  private static String simpleName(@NotNull SpecificHaxeClassReference classType) {
    HaxeClass haxeClass = classType.getHaxeClass();
    String name = haxeClass != null ? haxeClass.getName() : classType.getClassName();
    if (name == null || name.isEmpty()) return null;
    String withoutSuffix = StringUtil.trimEnd(name, IMPLEMENTATION_SUFFIX);
    return withoutSuffix.isEmpty() ? name : withoutSuffix;
  }

  @NotNull
  private static SpecificTypeReference unwrapped(@NotNull SpecificTypeReference type) {
    if (type instanceof SpecificHaxeClassReference classType && (classType.isTypeDef() || classType.isNullType())) {
      return classType.fullyResolveTypeDefAndUnwrapNullTypeReference();
    }
    return type;
  }
}
