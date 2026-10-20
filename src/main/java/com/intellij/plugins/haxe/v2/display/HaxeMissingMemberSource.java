package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import com.intellij.plugins.haxe.display.protocol.MissingFields;
import com.intellij.plugins.haxe.ide.generation.HaxeMemberStubs;
import com.intellij.plugins.haxe.model.HaxeAccessorType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Source text for the members a MISSING_FIELDS entry asks for, with the
 * types the compiler reported. It writes method stubs with the body
 * {@link HaxeMemberStubs} prescribes, properties with their access pair,
 * and plain variables. For a class whose final fields are never initialized,
 * it writes a constructor that takes and assigns each of them in declaration
 * order. An abstract parent's methods are implemented without
 * {@code override}, because the compiler rejects it on an abstract parent
 * field.
 */
final class HaxeMissingMemberSource {

  private HaxeMissingMemberSource() {
  }

  /** One declaration per member, in the entry's order. */
  @NotNull
  static List<String> declarationsOf(@NotNull MissingFields.Entry entry) {
    if (entry.isFinalFields()) return List.of(constructorText(entry.causeFields()));
    List<String> declarations = new ArrayList<>();
    for (MissingFields.MissingField field : entry.fields()) {
      declarations.add(field.isMethod() ? methodText(field) : variableText(field));
    }
    return declarations;
  }

  @NotNull
  private static String methodText(@NotNull MissingFields.MissingField field) {
    String returnType = HaxeTypeSyntax.returnTypeText(field.type());
    JsonTypeRef wireReturnType = field.type() == null ? null : field.type().returnType();
    String body = HaxeMemberStubs.body(wireReturnType == null || !wireReturnType.isVoid());
    String parameters = field.type() != null && field.type().isFunction() ? HaxeTypeSyntax.parameterListText(field.type()) : "";
    return modifiers(field) + "function " + field.name() + "(" + parameters + "):" + returnType + " " + body;
  }

  @NotNull
  private static String variableText(@NotNull MissingFields.MissingField field) {
    String access = accessPair(field);
    return modifiers(field) + "var " + field.name() + access + ":" + HaxeTypeSyntax.safeTypeText(field.type()) + ";";
  }

  @NotNull
  private static String constructorText(@NotNull List<MissingFields.MissingField> finalFields) {
    List<MissingFields.MissingField> ordered = new ArrayList<>(finalFields);
    ordered.sort(Comparator.comparingInt(MissingFields.MissingField::declarationOffset));
    List<String> parameters = new ArrayList<>();
    StringBuilder assignments = new StringBuilder();
    for (MissingFields.MissingField field : ordered) {
      parameters.add(field.name() + ":" + HaxeTypeSyntax.safeTypeText(field.type()));
      assignments.append("\tthis.").append(field.name()).append(" = ").append(field.name()).append(";\n");
    }
    return "public function new(" + String.join(", ", parameters) + ") {\n" + assignments + "}";
  }

  @NotNull
  private static String modifiers(@NotNull MissingFields.MissingField field) {
    return (field.isPublic() ? "public " : "") + (field.isStatic() ? "static " : "");
  }

  /** {@code (get, never)} for a property; nothing for a plain variable. */
  @NotNull
  private static String accessPair(@NotNull MissingFields.MissingField field) {
    String read = accessText(field.readAccess(), HaxeAccessorType.GET);
    String write = accessText(field.writeAccess(), HaxeAccessorType.SET);
    if (read == null && write == null) return "";
    String plain = HaxeAccessorType.DEFAULT.text;
    return "(" + (read != null ? read : plain) + ", " + (write != null ? write : plain) + ")";
  }

  /** The source spelling of an access kind the compiler reported; null for the plain access that needs no pair. */
  @Nullable
  private static String accessText(@NotNull String wireKind, @NotNull HaxeAccessorType accessor) {
    return switch (wireKind) {
      case "AccCall" -> accessor.text;
      case "AccNever" -> HaxeAccessorType.NEVER.text;
      case "AccNo" -> HaxeAccessorType.NULL.text;
      default -> null;
    };
  }
}
