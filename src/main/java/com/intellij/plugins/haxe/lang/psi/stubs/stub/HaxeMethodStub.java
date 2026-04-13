package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stub for method declarations (including constructors via flag, and
 * module-level methods which get their own element type but share this stub class).
 */
public class HaxeMethodStub extends StubBase<HaxeMethod> implements StubWithModifiers, StubWithName {

  // Keyword-modifier flags (stored in `flags`)
  public static final int IS_STATIC       = 0b000001;
  public static final int IS_PUBLIC       = 0b000010;
  public static final int IS_OVERRIDE     = 0b000100;
  public static final int IS_ABSTRACT     = 0b001000;
  public static final int IS_INLINE       = 0b010000;

  public static final int HAS_PARAMETERS  = 0b100000;

  // TODO overload ?

  // Metadata-modifier flags (stored in `metaFlags`).
  // Captured at index time to avoid expensive sibling PSI traversal at runtime.
  public static final int META_ABSTRACT      = 0b00000001;  // @:abstract
  public static final int META_FINAL         = 0b00000010;  // @:final
  public static final int META_NATIVE        = 0b00000100;  // @:native
  public static final int META_INLINE        = 0b00001000;  // @:inline
  public static final int META_MACRO         = 0b00010000;  // @:macro
  public static final int META_DEPRECATED    = 0b00100000;  // @:deprecated
  // TODO (needs entries  in HaxePsiModifier ?)
  public static final int META_NO_COMPLETION = 0b01000000;  // @:noCompletion
  public static final int META_KEEP          = 0b10000000;  // @:keep

  private final String name;

  private final int flags;
  private final int metaFlags;

  public HaxeMethodStub(StubElement parent,
                         @NotNull IStubElementType elementType,
                         @Nullable String name,
                         boolean isStatic,
                         boolean isPublic,
                         boolean isOverride,
                         boolean isAbstract,
                         boolean isInline,
                         boolean hasParameters,
                         int metaFlags) {
    super(parent, elementType);
    this.name = name;
    this.flags = (isStatic      ? IS_STATIC   : 0)
               | (isPublic      ? IS_PUBLIC   : 0)
               | (isOverride    ? IS_OVERRIDE : 0)
               | (isAbstract    ? IS_ABSTRACT : 0)
               | (isInline      ? IS_INLINE   : 0)
               | (hasParameters ? HAS_PARAMETERS : 0);
    this.metaFlags = metaFlags;
  }

  public HaxeMethodStub(StubElement parent,
                         @NotNull IStubElementType elementType,
                         @Nullable String name,
                         int flags,
                         int metaFlags) {
    super(parent, elementType);
    this.name = name;
    this.flags = flags;
    this.metaFlags = metaFlags;
  }

  @Nullable
  public String getName() {
    return name;
  }

  public int getFlags() {
    return flags;
  }

  public int getMetaFlags() {
    return metaFlags;
  }

  public boolean isStatic() {
    return (flags & IS_STATIC) != 0;
  }

  public boolean isPublic() {
    return (flags & IS_PUBLIC) != 0;
  }

  public boolean isOverride() {
    return (flags & IS_OVERRIDE) != 0;
  }

  public boolean isAbstract() {
    return (flags & IS_ABSTRACT) != 0;
  }

  public boolean isInline() {
    return (flags & IS_INLINE) != 0;
  }

  public boolean hasParameters() {
    return (flags & HAS_PARAMETERS) != 0;
  }

  public boolean isConstructor() {
    return !isStatic() && "new".equals(name);
  }

  /**
   * Returns whether the given modifier string is present as a metadata annotation on this method,
   * or {@code null} if the modifier is not tracked in the metadata flags.
   * This allows fast stub-based lookup without PSI tree traversal.
   */
  @Nullable
  public Boolean hasMetaForModifier(@HaxePsiModifier.ModifierConstant String modifier) {
    return switch (modifier) {
      case HaxePsiModifier.ABSTRACT     -> (metaFlags & META_ABSTRACT)      != 0;
      case HaxePsiModifier.FINAL_META   -> (metaFlags & META_FINAL)         != 0;
      case HaxePsiModifier.NATIVE       -> (metaFlags & META_NATIVE)        != 0;
      case HaxePsiModifier.INLINE_META  -> (metaFlags & META_INLINE)        != 0;
      case HaxePsiModifier.MACRO2       -> (metaFlags & META_MACRO)         != 0;
      case HaxePsiModifier.DEPRECATED   -> (metaFlags & META_DEPRECATED)    != 0;
      default -> null;
    };
  }
  @Nullable
  public Boolean hasKeywordModifier(@HaxePsiModifier.ModifierConstant String modifier) {
    return switch (modifier) {
      case HaxePsiModifier.ABSTRACT     -> (flags & IS_ABSTRACT)      != 0;
      case HaxePsiModifier.PUBLIC       -> (flags & IS_PUBLIC)        != 0;
      case HaxePsiModifier.STATIC       -> (flags & IS_STATIC)        != 0;
      case HaxePsiModifier.INLINE       -> (flags & IS_INLINE)        != 0;
      case HaxePsiModifier.OVERRIDE     -> (flags & IS_OVERRIDE)      != 0;
      default -> null;
    };
  }
}

