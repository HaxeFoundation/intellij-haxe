package com.intellij.plugins.haxe.display.protocol;

import java.util.List;

/**
 * What a MISSING_FIELDS diagnostic reports: the type that lacks the fields,
 * its file, and one entry per cause. The cause kinds are:
 * <ul>
 *   <li>{@code ImplementedInterface} and {@code AbstractParent}: members the
 *   interface or parent declares and the type lacks.</li>
 *   <li>{@code PropertyAccessor}: an accessor that a property names.</li>
 *   <li>{@code FieldAccess}: a member the code calls or accesses that the
 *   type does not have.</li>
 *   <li>{@code FinalFields}: final fields that no constructor initializes.
 *   The entry's own field list is then empty, and the fields sit in the
 *   cause.</li>
 * </ul>
 */
public record MissingFields(String typeName, String moduleFile, List<Entry> entries) {

  public record Entry(String causeKind, List<MissingField> fields, List<MissingField> causeFields) {
    public boolean isFinalFields() {
      return "FinalFields".equals(causeKind);
    }

    public boolean isFieldAccess() {
      return "FieldAccess".equals(causeKind);
    }
  }

  /**
   * One missing class field, as the compiler typed it. {@code fieldKind} is
   * FMethod or FVar (a property is an FVar with accessor access). A
   * variable's {@code readAccess} and
   * {@code writeAccess} hold the wire access kinds (AccNormal, AccCall,
   * AccNever, AccNo, ...). {@code declarationOffset} is the start offset of
   * the declaration in its own file, or -1 when unknown.
   */
  public record MissingField(String name, JsonTypeRef type, String fieldKind, boolean isPublic, boolean isStatic,
                             boolean unique, String readAccess, String writeAccess, int declarationOffset) {
    public boolean isMethod() {
      return "FMethod".equals(fieldKind);
    }
  }

  /** The MISSING_FIELDS args as the wire carries them: the type lacking the fields, its file and one entry per cause. */
  public record Args(ModuleType moduleType, String moduleFile, List<WireEntry> entries) implements DiagnosticArgs {

    MissingFields toMissingFields() {
      String typeName = moduleType == null ? "" : moduleType.name();
      List<Entry> converted = entries.stream()
        .map(WireEntry::toEntry)
        .toList();
      return new MissingFields(typeName, moduleFile, converted);
    }
  }

  record ModuleType(String name) {
  }

  /** One cause's missing fields. */
  record WireEntry(List<WrappedField> fields, Cause cause) {

    Entry toEntry() {
      List<MissingField> missing = fields.stream()
        .filter(wrapped -> wrapped.field() != null)
        .map(WrappedField::toMissingField)
        .toList();
      String causeKind = cause == null ? "" : cause.kind();
      return new Entry(causeKind, missing, causeFieldsOf(cause));
    }

    /** The fields the cause names itself, such as the final fields no constructor initializes. */
    private static List<MissingField> causeFieldsOf(Cause cause) {
      if (cause == null || cause.args() == null) return List.of();
      return cause.args().fields().stream()
        .map(field -> missingField(field, true))
        .toList();
    }
  }

  /** A missing field and whether it is the only candidate; absent {@code unique} counts as unique. */
  record WrappedField(JsonClassField field, Boolean unique) {

    MissingField toMissingField() {
      return missingField(field, unique == null || unique);
    }
  }

  /** Why the fields are missing; only some causes, such as FinalFields, name fields of their own. */
  record Cause(String kind, CauseArgs args) {
  }

  record CauseArgs(List<JsonClassField> fields) {
  }

  private static MissingField missingField(JsonClassField field, boolean unique) {
    JsonClassField.Kind kind = field.kind();
    String readAccess = kind == null ? "" : kind.readAccess();
    String writeAccess = kind == null ? "" : kind.writeAccess();
    int declarationOffset = field.pos() == null ? -1 : field.pos().min();
    return new MissingField(field.name(), field.type(), field.kindName(), field.isPublic(), field.isStatic(),
                            unique, readAccess, writeAccess, declarationOffset);
  }
}
