package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.model.HaxeCompilerMetadata;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stub for field declarations (including module-level fields and optional fields,
 * which get their own element types but share this stub class).
 */
public class HaxeFieldStub extends StubBase<HaxePsiField> implements StubWithName, StubWithMetaAndModifiers {

  // Keyword-modifier flags (stored in `flags`)
  private static final int IS_STATIC = 1 << 0;
  private static final int IS_PUBLIC = 1 << 1;
  private static final int IS_FINAL  = 1 << 2;
  private static final int IS_INLINE = 1 << 3;

  // Metadata-modifier flags (stored in `metaFlags`).
  // Captured at index time to avoid expensive sibling PSI traversal at runtime.
  public static final int META_FINAL         = 1 << 0;  // @:final
  public static final int META_NATIVE        = 1 << 1;  // @:native
  public static final int META_IS_VAR        = 1 << 2;  // @:isVar
  public static final int META_DEPRECATED    = 1 << 3;  // @:deprecated
  public static final int META_NO_COMPLETION = 1 << 4;  // @:noCompletion
  public static final int META_INLINE        = 1 << 5;  // @:inline
  public static final int META_KEEP          = 1 << 6;  // @:keep

  private final String name;
  private final int keywordFlags;
  private final int metaFlags;
  /** Text of the getter accessor, e.g. {@code "get"}, {@code "null"}, {@code "default"}, or {@code null} if not a property. */
  @Nullable private final String getter;
  /** Text of the setter accessor, e.g. {@code "set"}, {@code "null"}, {@code "never"}, or {@code null} if not a property. */
  @Nullable private final String setter;

  public HaxeFieldStub(StubElement parent,
                        @NotNull IStubElementType elementType,
                        @Nullable String name,
                        boolean isStatic,
                        boolean isPublic,
                        boolean isFinal,
                        boolean isInline,
                        int metaFlags,
                        @Nullable String getter,
                        @Nullable String setter) {
    super(parent, elementType);
    this.name = name;
    this.keywordFlags = (isStatic ? IS_STATIC : 0)
               | (isPublic ? IS_PUBLIC : 0)
               | (isFinal ? IS_FINAL : 0)
               | (isInline ? IS_INLINE : 0);
    this.metaFlags = metaFlags;
    this.getter = getter;
    this.setter = setter;
  }

  public HaxeFieldStub(StubElement parent,
                        @NotNull IStubElementType elementType,
                        @Nullable String name,
                        int keywordFlags,
                        int metaFlags,
                        @Nullable String getter,
                        @Nullable String setter) {
    super(parent, elementType);
    this.name = name;
    this.keywordFlags = keywordFlags;
    this.metaFlags = metaFlags;
    this.getter = getter;
    this.setter = setter;
  }

  @Nullable
  public String getName() {
    return name;
  }

  public int getKeywordFlags() {
    return keywordFlags;
  }

  public int getMetaFlags() {
    return metaFlags;
  }

  public boolean isStatic() {
    return (keywordFlags & IS_STATIC) != 0;
  }

  public boolean isPublic() {
    return (keywordFlags & IS_PUBLIC) != 0;
  }

  public boolean isFinal() {
    return (keywordFlags & IS_FINAL) != 0;
  }

  public boolean isInline() {
    return (keywordFlags & IS_INLINE) != 0;
  }

  /** Returns the getter accessor text (e.g. {@code "get"}, {@code "null"}), or {@code null} if this is not a property field. */
  @Nullable
  public String getGetter() {
    return getter;
  }

  /** Returns the setter accessor text (e.g. {@code "set"}, {@code "never"}), or {@code null} if this is not a property field. */
  @Nullable
  public String getSetter() {
    return setter;
  }

  /** Returns {@code true} if this field has a property declaration (getter/setter accessors). */
  public boolean isProperty() {
    return getter != null;
  }

  /**
   * Returns whether the given modifier string is present as a metadata annotation on this field,
   * or {@code null} if the modifier is not tracked in the metadata flags.
   */
  @Nullable
  public Boolean hasMetadata(@StubMetaConstant String modifier) {
    return switch (modifier) {
      case FINAL_META   -> (metaFlags & META_FINAL)         != 0;
      case IS_VAR_META  -> (metaFlags & META_IS_VAR)        != 0;
      case INLINE_META  -> (metaFlags & META_INLINE)        != 0;
      case NATIVE       -> (metaFlags & META_NATIVE)        != 0;
      case DEPRECATED   -> (metaFlags & META_DEPRECATED)    != 0;
      case NO_COMPLETION-> (metaFlags & META_NO_COMPLETION) != 0;
      case KEEP_META    -> (metaFlags & META_KEEP)          != 0;
      default -> null;
    };
  }

  @Override
  public Boolean hasKeyword(String modifier) {
    return switch (modifier) {
      case HaxePsiModifier.PUBLIC       -> (keywordFlags & IS_PUBLIC)   != 0;
      case HaxePsiModifier.FINAL        -> (keywordFlags & IS_FINAL)   != 0;
      case HaxePsiModifier.STATIC       -> (keywordFlags & IS_STATIC)   != 0;
      case HaxePsiModifier.INLINE       -> (keywordFlags & IS_INLINE)   != 0;
      default -> null;
    };
  }

  public static final String FINAL_META     = HaxeCompilerMetadata.FINAL;
  public static final String IS_VAR_META    = HaxeCompilerMetadata.IS_VAR;
  public static final String INLINE_META    = HaxeCompilerMetadata.INLINE;
  public static final String KEEP_META      = HaxeCompilerMetadata.KEEP;
  public static final String NATIVE         = HaxeCompilerMetadata.NATIVE;
  public static final String DEPRECATED     = HaxeCompilerMetadata.DEPRECATED;
  public static final String NO_COMPLETION  = HaxeCompilerMetadata.NO_COMPLETION;

  @MagicConstant(stringValues = {
          FINAL_META,
          IS_VAR_META,
          INLINE_META,
          KEEP_META,
          NATIVE,
          DEPRECATED,
          NO_COMPLETION

  })
  @interface StubMetaConstant {}

}

