package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.model.HaxeCompilerMetadata;
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
public class HaxeMethodStub extends StubBase<HaxeMethod> implements StubWithMetaAndModifiers, StubWithName {

  // Keyword-modifier flags (stored in `flags`)
  public static final int IS_STATIC       = 1 << 0;
  public static final int IS_PUBLIC       = 1 << 1;
  public static final int IS_OVERRIDE     = 1 << 2;
  public static final int IS_ABSTRACT     = 1 << 3;
  public static final int IS_INLINE       = 1 << 4;
  public static final int IS_MACRO        = 1 << 5;
  public static final int IS_OVERLOAD     = 1 << 6;
  public static final int IS_DYNAMIC      = 1 << 7;

  // Metadata-modifier flags (stored in `metaFlags`).
  public static final int META_ABSTRACT      = 1 << 0;  // @:abstract
  public static final int META_FINAL         = 1 << 1;  // @:final
  public static final int META_NATIVE        = 1 << 2;  // @:native
  public static final int META_INLINE        = 1 << 3;  // @:inline
  public static final int META_MACRO         = 1 << 4;  // @:macro
  public static final int META_DEPRECATED    = 1 << 5;  // @:deprecated
  public static final int META_OVERLOAD      = 1 << 6;  // @:deprecated

  public static final int META_NO_COMPLETION = 1 << 7;  // @:noCompletion
  public static final int META_NO_USING      = 1 << 8;  // @:noUsing
  public static final int META_KEEP          = 1 << 9;  // @:keep

  // TODOS
  // @:allow & @:access / @:arrayAccess / @:overload

  // properties
  public static final int IS_CONSTRUCTOR        = 1 << 0;
  public static final int HAS_PARAMETERS        = 1 << 1;
  public static final int HAS_VARARG_PARAMETERS = 1 << 2;


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
  public Boolean hasMetadata(@HaxePsiModifier.MetaConstant String modifier) {
    return switch (modifier) {
      case HaxeCompilerMetadata.ABSTRACT     -> (metaFlags & META_ABSTRACT)      != 0;
      case HaxeCompilerMetadata.FINAL        -> (metaFlags & META_FINAL)         != 0;
      case HaxeCompilerMetadata.NATIVE       -> (metaFlags & META_NATIVE)        != 0;
      case HaxeCompilerMetadata.INLINE       -> (metaFlags & META_INLINE)        != 0;
      case HaxeCompilerMetadata.OVERLOAD     -> (metaFlags & META_OVERLOAD)      != 0;
      case HaxeCompilerMetadata.MACRO        -> (metaFlags & META_MACRO)         != 0;
      case HaxeCompilerMetadata.DEPRECATED   -> (metaFlags & META_DEPRECATED)    != 0;
      case HaxeCompilerMetadata.NO_USING     -> (metaFlags & META_NO_USING)      != 0;
      default -> null;
    };
  }
  @Nullable
  public Boolean hasKeyword(@HaxePsiModifier.KeywordConstant String modifier) {
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

