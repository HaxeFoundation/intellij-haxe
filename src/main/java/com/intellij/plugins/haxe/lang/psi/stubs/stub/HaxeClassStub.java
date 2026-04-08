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
  public static final int META_FINAL         = 0b0000001;  // @:final
  public static final int META_NATIVE        = 0b0000010;  // @:native
  public static final int META_DEPRECATED    = 0b0000100;  // @:deprecated
  public static final int META_NO_COMPLETION = 0b0001000;  // @:noCompletion
  public static final int META_KEEP          = 0b0010000;  // @:keep

  public static final int META_ENUM          = 0b0100000;  // @:enum
  public static final int META_ABSTRACT      = 0b1000000;  // @:abstract

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
      default -> null;
    };
  }
}

