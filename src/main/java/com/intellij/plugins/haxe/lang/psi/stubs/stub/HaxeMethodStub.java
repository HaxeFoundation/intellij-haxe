package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stub for method declarations (including constructors via flag, and
 * module-level methods which get their own element type but share this stub class).
 */
public class HaxeMethodStub extends StubBase<HaxeMethod> implements StubWithModifiers, StubWithName {

  // Keyword-modifier flags (stored in `flags`)
  public static final int IS_STATIC       = 0b00000001;
  public static final int IS_PUBLIC       = 0b00000010;
  public static final int IS_OVERRIDE     = 0b00000100;
  public static final int IS_ABSTRACT     = 0b00001000;
  public static final int IS_INLINE       = 0b00010000;
  public static final int IS_MACRO        = 0b00100000;
  public static final int IS_OVERLOAD     = 0b01000000;
  public static final int IS_DYNAMIC      = 0b10000000;

  // Metadata-modifier flags (stored in `metaFlags`).
  public static final int META_ABSTRACT      = 0b000000001;  // @:abstract
  public static final int META_FINAL         = 0b000000010;  // @:final
  public static final int META_NATIVE        = 0b000000100;  // @:native
  public static final int META_INLINE        = 0b000001000;  // @:inline
  public static final int META_MACRO         = 0b000010000;  // @:macro
  public static final int META_DEPRECATED    = 0b000100000;  // @:deprecated
  // TODO (needs entries  in HaxePsiModifier ?)
  public static final int META_NO_COMPLETION = 0b001000000;  // @:noCompletion
  public static final int META_NO_USING      = 0b010000000;  // @:noCompletion
  public static final int META_KEEP          = 0b100000000;  // @:keep


  // properties
  public static final int IS_CONSTRUCTOR     = 0b01000000;
  public static final int HAS_PARAMETERS     = 0b01000000;
  public static final int HAS_VARARG_PARAMETERS     = 0b01000000;


  private final String name;

  @Getter private final int keywordFlags;
  @Getter private final int metaFlags;
  @Getter private final int propertyFlags;



  public HaxeMethodStub(StubElement parent,
                         @NotNull IStubElementType elementType,
                         @Nullable String name,
                         int keywordFlags,
                         int metaFlags,
                         int propertyFlags
  ) {
    super(parent, elementType);
    this.name = name;
    this.keywordFlags = keywordFlags;
    this.metaFlags = metaFlags;
    this.propertyFlags = propertyFlags;
  }

  @Nullable
  public String getName() {
    return name;
  }



  public boolean isStatic() {
    return (keywordFlags & IS_STATIC) != 0;
  }

  public boolean isPublic() {
    return (keywordFlags & IS_PUBLIC) != 0;
  }

  public boolean isOverride() {
    return (keywordFlags & IS_OVERRIDE) != 0;
  }

  public boolean isAbstract() {
    return (keywordFlags & IS_ABSTRACT) != 0;
  }

  public boolean isInline() {
    return (keywordFlags & IS_INLINE) != 0;
  }

  public boolean isOverload() {
    return (keywordFlags & IS_OVERLOAD) != 0;
  }

  public boolean isMacro() {
    return (keywordFlags & IS_MACRO) != 0;
  }

  public boolean isDynamic() {
    return (keywordFlags & IS_DYNAMIC) != 0;
  }

  // properties

  public boolean isConstructor() {
    return (propertyFlags & IS_CONSTRUCTOR) != 0;
  }
  public boolean hasParameters() {
    return (propertyFlags & HAS_PARAMETERS) != 0;
  }

  public boolean hasVarargs() {
    return (propertyFlags & HAS_VARARG_PARAMETERS) != 0;
  }


  /**
   * Returns whether the given modifier string is present as a metadata annotation on this method,
   * or {@code null} if the modifier is not tracked in the metadata flags.
   * This allows fast stub-based lookup without PSI tree traversal.
   */
  @Nullable
  public Boolean hasMetaModifier(@HaxePsiModifier.ModifierConstant String modifier) {
    return switch (modifier) {
      case HaxePsiModifier.ABSTRACT     -> (metaFlags & META_ABSTRACT)      != 0;
      case HaxePsiModifier.FINAL_META   -> (metaFlags & META_FINAL)         != 0;
      case HaxePsiModifier.NATIVE       -> (metaFlags & META_NATIVE)        != 0;
      case HaxePsiModifier.INLINE_META  -> (metaFlags & META_INLINE)        != 0;
      case HaxePsiModifier.MACRO_META   -> (metaFlags & META_MACRO)         != 0;
      case HaxePsiModifier.DEPRECATED   -> (metaFlags & META_DEPRECATED)    != 0;
      default -> null;
    };
  }
  @Nullable
  public Boolean hasKeywordModifier(@HaxePsiModifier.ModifierConstant String modifier) {
    return switch (modifier) {
      case HaxePsiModifier.ABSTRACT     -> (keywordFlags & IS_ABSTRACT) != 0;
      case HaxePsiModifier.PUBLIC       -> (keywordFlags & IS_PUBLIC)   != 0;
      case HaxePsiModifier.STATIC       -> (keywordFlags & IS_STATIC)   != 0;
      case HaxePsiModifier.INLINE       -> (keywordFlags & IS_INLINE)   != 0;
      case HaxePsiModifier.OVERRIDE     -> (keywordFlags & IS_OVERRIDE) != 0;
      case HaxePsiModifier.OVERLOAD     -> (keywordFlags & IS_OVERLOAD) != 0;
      case HaxePsiModifier.MACRO        -> (keywordFlags & IS_MACRO)    != 0;
      case HaxePsiModifier.DYNAMIC      -> (keywordFlags & IS_DYNAMIC)  != 0;
      default -> null;
    };
  }
}

