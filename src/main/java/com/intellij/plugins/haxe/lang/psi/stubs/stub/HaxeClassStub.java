package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stub for Haxe class-like declarations: class, interface, enum, abstract, typedef,
 * extern class, extern interface (and their macro variants).
 */
public class HaxeClassStub extends StubBase<HaxeClass> implements StubWithName {

  // Metadata-modifier flags (stored in `metaFlags`).
  // Captured at index time to avoid expensive sibling PSI traversal at runtime.
  public static final int META_FINAL         = 1 <<  0;  // @:final
  public static final int META_NATIVE        = 1 <<  1;  // @:native
  public static final int META_DEPRECATED    = 1 <<  2;  // @:deprecated
  public static final int META_NO_COMPLETION = 1 <<  3;  // @:noCompletion
  public static final int META_KEEP          = 1 <<  4;  // @:keep

  public static final int META_ENUM          = 1 <<  5;  // @:enum
  public static final int META_ABSTRACT      = 1 <<  6;  // @:abstract

  public static final int META_GENERIC_BUILD = 1 <<  7;  // @:genericBuild
  public static final int META_STRUCT_INIT   = 1 <<  8;  // @:structInit
  public static final int META_USING         = 1 <<  9;  // @:using
  public static final int META_FORWARD       = 1 << 11;  // @:forward


  private final String name;

  @Getter private final String qualifiedName;
  @Getter private final int componentTypeKey;
  @Getter private final boolean isPrivate;
  @Getter private final boolean isExtern;
  @Getter private final boolean isEnum; // only relevant for abstracts
  @Getter private final String[] superTypeNames;
  @Getter private final int metaFlags;

  public HaxeClassStub(StubElement parent,
                        @NotNull IStubElementType elementType,
                        @Nullable String name,
                        @Nullable String qualifiedName,
                        int componentTypeKey,
                        boolean isPrivate,
                        boolean isExtern,
                        boolean isEnum,
                        @NotNull String[] superTypeNames,
                        int metaFlags) {
    super(parent, elementType);
    this.name = name;
    this.qualifiedName = qualifiedName;
    this.componentTypeKey = componentTypeKey;
    this.isPrivate = isPrivate;
    this.isExtern = isExtern;
    this.isEnum = isEnum; // only relevant for abstracts
    this.superTypeNames = superTypeNames;
    this.metaFlags = metaFlags;
  }

  @Nullable
  public String getName() {
    return name;
  }

  @Nullable
  public HaxeComponentType getComponentType() {
    return HaxeComponentType.valueOf(componentTypeKey);
  }


  /**
   * Returns whether the given modifier string is present as a metadata annotation on this class,
   * or {@code null} if the modifier is not tracked in the metadata flags.
   */
  @Nullable
  public Boolean hasMetaForModifier(@HaxePsiModifier.ModifierConstant String modifier) {
    return switch (modifier) {
      case HaxePsiModifier.FINAL_META     -> (metaFlags & META_FINAL)         != 0;
      case HaxePsiModifier.NATIVE         -> (metaFlags & META_NATIVE)        != 0;
      case HaxePsiModifier.DEPRECATED     -> (metaFlags & META_DEPRECATED)    != 0;
      case HaxePsiModifier.ENUM_META      -> (metaFlags & META_ENUM)          != 0;
      case HaxePsiModifier.ABSTRACT_META  -> (metaFlags & META_ABSTRACT)      != 0;
      case HaxePsiModifier.GENERIC_BUILD  -> (metaFlags & META_GENERIC_BUILD) != 0;
      case HaxePsiModifier.STRUCT_INIT    -> (metaFlags & META_STRUCT_INIT)   != 0;
      case HaxePsiModifier.USING          -> (metaFlags & META_USING)         != 0;
      case HaxePsiModifier.FORWARD        -> (metaFlags & META_FORWARD)       != 0;
      default -> null;
    };
  }
}

