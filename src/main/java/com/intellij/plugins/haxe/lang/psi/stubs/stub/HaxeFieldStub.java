package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stub for field declarations (including module-level fields and optional fields,
 * which get their own element types but share this stub class).
 */
public class HaxeFieldStub extends StubBase<HaxePsiField> implements StubWithName {

  // Keyword-modifier flags (stored in `flags`)
  private static final int IS_STATIC = 0b0001;
  private static final int IS_PUBLIC = 0b0010;
  private static final int IS_FINAL  = 0b0100;
  private static final int IS_INLINE = 0b1000;

  // Metadata-modifier flags (stored in `metaFlags`).
  // Captured at index time to avoid expensive sibling PSI traversal at runtime.
  public static final int META_FINAL         = 0b000001;  // @:final
  public static final int META_NATIVE        = 0b000010;  // @:native
  public static final int META_IS_VAR        = 0b000100;  // @:isVar
  public static final int META_DEPRECATED    = 0b001000;  // @:deprecated
  public static final int META_NO_COMPLETION = 0b010000;  // @:noCompletion
  public static final int META_KEEP          = 0b100000;  // @:keep

  private final String name;
  private final int flags;
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
    this.flags = (isStatic ? IS_STATIC : 0)
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
                        int flags,
                        int metaFlags,
                        @Nullable String getter,
                        @Nullable String setter) {
    super(parent, elementType);
    this.name = name;
    this.flags = flags;
    this.metaFlags = metaFlags;
    this.getter = getter;
    this.setter = setter;
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

  public boolean isFinal() {
    return (flags & IS_FINAL) != 0;
  }

  public boolean isInline() {
    return (flags & IS_INLINE) != 0;
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
  public Boolean hasMetaForModifier(@HaxePsiModifier.ModifierConstant String modifier) {
    return switch (modifier) {
      case HaxePsiModifier.FINAL_META   -> (metaFlags & META_FINAL)         != 0;
      case HaxePsiModifier.NATIVE       -> (metaFlags & META_NATIVE)        != 0;
      case HaxePsiModifier.IS_VAR       -> (metaFlags & META_IS_VAR)        != 0;
      case HaxePsiModifier.DEPRECATED   -> (metaFlags & META_DEPRECATED)    != 0;
      default -> null;
    };
  }
}

