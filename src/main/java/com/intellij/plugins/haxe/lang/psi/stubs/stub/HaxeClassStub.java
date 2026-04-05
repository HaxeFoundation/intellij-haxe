package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stub for Haxe class-like declarations: class, interface, enum, abstract, typedef,
 * extern class, extern interface (and their macro variants).
 */
public class HaxeClassStub extends StubBase<HaxeClass> implements StubWithName {

  // Metadata-modifier flags (stored in `metaFlags`).
  // Captured at index time to avoid expensive sibling PSI traversal at runtime.
  public static final int META_FINAL         = 0b000001;  // @:final
  public static final int META_NATIVE        = 0b000010;  // @:native
  public static final int META_DEPRECATED    = 0b000100;  // @:deprecated
  public static final int META_NO_COMPLETION = 0b001000;  // @:noCompletion
  public static final int META_KEEP          = 0b010000;  // @:keep
  public static final int META_ABSTRACT      = 0b100000;  // @:abstract

  private final String name;
  private final String qualifiedName;
  private final int componentTypeKey;
  private final boolean isPrivate;
  private final boolean isExtern;
  private final String[] superTypeNames;
  private final int metaFlags;

  public HaxeClassStub(StubElement parent,
                        @NotNull IStubElementType elementType,
                        @Nullable String name,
                        @Nullable String qualifiedName,
                        int componentTypeKey,
                        boolean isPrivate,
                        boolean isExtern,
                        @NotNull String[] superTypeNames,
                        int metaFlags) {
    super(parent, elementType);
    this.name = name;
    this.qualifiedName = qualifiedName;
    this.componentTypeKey = componentTypeKey;
    this.isPrivate = isPrivate;
    this.isExtern = isExtern;
    this.superTypeNames = superTypeNames;
    this.metaFlags = metaFlags;
  }

  @Nullable
  public String getName() {
    return name;
  }

  @Nullable
  public String getQualifiedName() {
    return qualifiedName;
  }

  public int getComponentTypeKey() {
    return componentTypeKey;
  }

  @Nullable
  public HaxeComponentType getComponentType() {
    return HaxeComponentType.valueOf(componentTypeKey);
  }

  public boolean isPrivate() {
    return isPrivate;
  }
  public boolean isExtern() {
    return isExtern;
  }

  @NotNull
  public String[] getSuperTypeNames() {
    return superTypeNames;
  }

  public int getMetaFlags() {
    return metaFlags;
  }

  /**
   * Returns whether the given modifier string is present as a metadata annotation on this class,
   * or {@code null} if the modifier is not tracked in the metadata flags.
   */
  @Nullable
  public Boolean hasMetaForModifier(@HaxePsiModifier.ModifierConstant String modifier) {
    return switch (modifier) {
      case HaxePsiModifier.FINAL_META   -> (metaFlags & META_FINAL)         != 0;
      case HaxePsiModifier.NATIVE       -> (metaFlags & META_NATIVE)        != 0;
      case HaxePsiModifier.DEPRECATED   -> (metaFlags & META_DEPRECATED)    != 0;
      case HaxePsiModifier.ABSTRACT     -> (metaFlags & META_ABSTRACT)      != 0;
      default -> null;
    };
  }
}

