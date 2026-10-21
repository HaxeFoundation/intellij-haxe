package com.intellij.plugins.haxe.display.protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A {@code haxe.display.JsonModuleTypes.JsonClassField}, reduced to what the
 * plugin reads: the name, type, visibility, scope, kind and position.
 * {@code scope} is 0 for a static field, 1 for a member and 2 for a
 * constructor.
 */
public record JsonClassField(String name, JsonTypeRef type, boolean isPublic, Integer scope, Kind kind, JsonPos pos) {

  /**
   * The field kind, {@code {kind, args}}: {@code FVar} carries its read and
   * write access in its args; any other kind ({@code FMethod}, whose args are
   * its method kind) keeps only its name.
   */
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true, defaultImpl = Kind.Other.class)
  @JsonSubTypes(@JsonSubTypes.Type(value = Kind.Var.class, name = "FVar"))
  public sealed interface Kind {

    /** FVar or FMethod (a property is an FVar with accessor access). */
    String kind();

    /** A variable's read access kind (AccNormal, AccCall, AccNever, AccNo, ...); empty for anything else. */
    default String readAccess() {
      return "";
    }

    /** A variable's write access kind; empty for anything else. */
    default String writeAccess() {
      return "";
    }

    record Var(String kind, Accesses args) implements Kind {

      @Override
      public String readAccess() {
        return args == null || args.read() == null ? "" : args.read().kind();
      }

      @Override
      public String writeAccess() {
        return args == null || args.write() == null ? "" : args.write().kind();
      }
    }

    record Accesses(Access read, Access write) {
    }

    record Access(String kind) {
    }

    record Other(String kind) implements Kind {
    }
  }

  public boolean isStatic() {
    return scope != null && scope == 0;
  }

  /** The field kind's name; empty when the wire gives none. */
  public String kindName() {
    return kind == null ? "" : kind.kind();
  }
}
