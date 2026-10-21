package com.intellij.plugins.haxe.display.client;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.intellij.plugins.haxe.display.protocol.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code display/completion} answer as the wire carries it. An item is
 * {@code {kind, args, type}}, where {@code args} has the shape of the kind.
 * Each kind the plugin offers is a record that turns itself into a
 * {@link CompletionItem}; any other kind, such as an anonymous structure or
 * an expression, reads as {@link Unlisted} and is left out.
 */
final class CompletionWire {

  private CompletionWire() {
  }

  /** The answer: the items, the completion mode, the text the items replace and whether the list is cut short. */
  record Result(List<Item> items, Mode mode, Range replaceRange, boolean isIncomplete) {

    CompletionList toList() {
      List<CompletionItem> listed = new ArrayList<>();
      int position = 0;
      for (Item item : items) {
        // a resolve request names an item by its place in this list; the item's own index repeats it when present
        CompletionItem converted = item.toItem(position++);
        if (converted != null) listed.add(converted);
      }
      Mode completionMode = mode != null ? mode : Mode.NONE;
      return new CompletionList(List.copyOf(listed), completionMode.kind(), completionMode.expectedType(), replaceRange, isIncomplete);
    }
  }

  /** A {@code display/completionItem/resolve} answer: the one item, with its doc comment. */
  record Resolved(Item item) {
  }

  /** The completion mode; at a position that expects a type, its args carry it. */
  record Mode(int kind, ModeArgs args) {

    static final Mode NONE = new Mode(-1, null);

    /** The expected type with typedefs followed, else the raw one; null when the position expects nothing. */
    JsonTypeRef expectedType() {
      if (args == null) return null;
      return args.expectedTypeFollowed() != null ? args.expectedTypeFollowed() : args.expectedType();
    }
  }

  record ModeArgs(JsonTypeRef expectedType, JsonTypeRef expectedTypeFollowed) {
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true, defaultImpl = Unlisted.class)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = NamedItem.class, names = {"Local", "Literal", "Keyword", "Metadata", "Define", "TypeParameter"}),
    @JsonSubTypes.Type(value = FieldItem.class, names = {"ClassField", "EnumAbstractField", "EnumField"}),
    @JsonSubTypes.Type(value = TypeItem.class, name = "Type"),
    @JsonSubTypes.Type(value = PackageItem.class, name = "Package"),
    @JsonSubTypes.Type(value = ModuleItem.class, name = "Module")})
  sealed interface Item {
    /** The item at {@code position} in the answer's list; null for a kind the plugin does not offer. */
    CompletionItem toItem(int position);
  }

  /** A kind whose args name it directly; a local carries its type in its args, metadata and defines their doc. */
  record NamedItem(String kind, NamedEntry args, JsonTypeRef type, Integer index) implements Item {

    @Override
    public CompletionItem toItem(int position) {
      NamedEntry named = args != null ? args : NamedEntry.NONE;
      JsonTypeRef itemType = type != null ? type : named.type();
      return new CompletionItem(kind, named.name(), null, null, itemType, docOf(named.doc()), indexOr(index, position), null);
    }
  }

  /** A name with its type and doc comment: a named item's args, or the field a field item wraps. */
  record NamedEntry(String name, JsonTypeRef type, String doc) {
    static final NamedEntry NONE = new NamedEntry("", null, "");
  }

  /** A class field, enum constructor or enum abstract value; its args wrap the field. */
  record FieldItem(String kind, FieldArgs args, JsonTypeRef type, Integer index) implements Item {

    @Override
    public CompletionItem toItem(int position) {
      NamedEntry field = args == null || args.field() == null ? NamedEntry.NONE : args.field();
      JsonTypeRef itemType = type != null ? type : field.type();
      return new CompletionItem(kind, field.name(), null, null, itemType, docOf(field.doc()), indexOr(index, position), null);
    }
  }

  record FieldArgs(NamedEntry field) {
  }

  /** A type: named by its path, its qualified name as the detail, with its declaration kind and import status. */
  record TypeItem(TypeArgs args, JsonTypeRef type, Integer index) implements Item {

    @Override
    public CompletionItem toItem(int position) {
      TypeArgs typeArgs = args != null ? args : TypeArgs.NONE;
      JsonTypePath path = typeArgs.path() != null ? typeArgs.path() : JsonTypePath.NONE;
      String doc = docOf(typeArgs.doc());
      int itemIndex = indexOr(index, position);
      return new CompletionItem("Type", path.typeName(), path.qualifiedName(), typeArgs.kind(), type, doc, itemIndex, path.importStatus());
    }
  }

  record TypeArgs(JsonTypePath path, String kind, String doc) {
    static final TypeArgs NONE = new TypeArgs(null, "", "");
  }

  /** A package: its last segment, with the whole dotted path as the detail. */
  record PackageItem(PathArgs args, JsonTypeRef type, Integer index) implements Item {

    @Override
    public CompletionItem toItem(int position) {
      List<String> pack = PathArgs.pathOf(args).pack();
      String name = pack.isEmpty() ? "" : pack.getLast();
      return new CompletionItem("Package", name, String.join(".", pack), null, type, null, indexOr(index, position), null);
    }
  }

  /** A module: its name, with its package as the detail. */
  record ModuleItem(PathArgs args, JsonTypeRef type, Integer index) implements Item {

    @Override
    public CompletionItem toItem(int position) {
      JsonTypePath path = PathArgs.pathOf(args);
      String packagePath = String.join(".", path.pack());
      return new CompletionItem("Module", path.moduleName(), packagePath, null, type, null, indexOr(index, position), null);
    }
  }

  record PathArgs(JsonTypePath path) {

    /** The path the args name; {@link JsonTypePath#NONE} when there are none. */
    static JsonTypePath pathOf(PathArgs args) {
      return args == null || args.path() == null ? JsonTypePath.NONE : args.path();
    }
  }

  /** A kind the plugin does not offer. */
  record Unlisted(String kind) implements Item {

    @Override
    public CompletionItem toItem(int position) {
      return null;
    }
  }

  /** The item's own index when the wire gives one, else its place in the list. */
  private static int indexOr(Integer index, int position) {
    return index != null ? index : position;
  }

  /** The doc comment, or null when it is blank. */
  private static String docOf(String doc) {
    return doc.isBlank() ? null : doc;
  }
}
