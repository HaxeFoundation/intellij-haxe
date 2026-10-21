package com.intellij.plugins.haxe.display.haxe3;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.intellij.plugins.haxe.display.protocol.CompletionItem;
import java.util.List;
import tools.jackson.dataformat.xml.annotation.JacksonXmlText;

/**
 * The XML answers of haxe 3's display modes as records, which {@link Haxe3Xml}
 * reads. Attributes and child elements bind alike, by name, and an element's
 * own text binds to its {@link JacksonXmlText} component; what an answer
 * leaves out reads as {@code ""} or an empty list. A completion entry binds by
 * its {@code k} attribute to the record of the item it becomes, and turns
 * itself into that {@link CompletionItem}; any other kind reads as an
 * unlisted record and is left out.
 */
final class Haxe3Wire {

  private static final String UNKNOWN_TYPE_PREFIX = "Unknown<";

  private Haxe3Wire() {
  }

  /** One answer, under the root {@link Haxe3Xml} adds: a {@code <list>}, a toplevel {@code <il>}, or one {@code <type>} per overload. */
  record Answer(Listing list, @JsonProperty("il") Toplevel toplevel, @JsonProperty("type") List<TypeEntry> types) {

    List<FieldEntry> fields() {
      return list == null ? List.of() : list.fields();
    }

    List<String> positions() {
      return list == null ? List.of() : list.positions();
    }

    List<ToplevelEntry> toplevelEntries() {
      return toplevel == null ? List.of() : toplevel.entries();
    }
  }

  /** A {@code <list>}: the field mode's entries, or the {@code <pos>} texts of a {@code @position} or {@code @usage} answer. */
  record Listing(@JsonProperty("i") List<FieldEntry> fields, @JsonProperty("pos") List<String> positions) {
  }

  /** A field-mode {@code <i n="name" k="kind"><t>Type</t><d>doc</d></i>}: a value's field, or a type or package of a package path. */
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "k", defaultImpl = UnlistedField.class)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = MemberField.class, names = {"var", "method"}),
    @JsonSubTypes.Type(value = TypeField.class, name = "type"),
    @JsonSubTypes.Type(value = PackageField.class, name = "package"),
    @JsonSubTypes.Type(value = MetadataField.class, name = "metadata")})
  sealed interface FieldEntry {
    /** The item at {@code index}, {@code qualifier} being the dotted path before the dot; null for a kind the plugin does not offer. */
    CompletionItem toItem(String qualifier, int index);
  }

  record MemberField(@JsonProperty("n") String name, @JsonProperty("t") String type, @JsonProperty("d") String doc) implements FieldEntry {

    @Override
    public CompletionItem toItem(String qualifier, int index) {
      return item("ClassField", name, null, type, doc, index);
    }
  }

  record TypeField(@JsonProperty("n") String name, @JsonProperty("d") String doc) implements FieldEntry {

    @Override
    public CompletionItem toItem(String qualifier, int index) {
      return item("Type", name, qualified(qualifier, name), null, doc, index);
    }
  }

  record PackageField(@JsonProperty("n") String name) implements FieldEntry {

    @Override
    public CompletionItem toItem(String qualifier, int index) {
      return item("Package", name, qualified(qualifier, name), null, null, index);
    }
  }

  record MetadataField(@JsonProperty("n") String name, @JsonProperty("d") String doc) implements FieldEntry {

    @Override
    public CompletionItem toItem(String qualifier, int index) {
      return item("Metadata", name, null, null, doc, index);
    }
  }

  /** A kind the plugin does not offer, such as haxe 3's timer pseudo-items. */
  record UnlistedField() implements FieldEntry {

    @Override
    public CompletionItem toItem(String qualifier, int index) {
      return null;
    }
  }

  /** The {@code @toplevel} answer's {@code <il>}. */
  record Toplevel(@JsonProperty("i") List<ToplevelEntry> entries) {
  }

  /** A toplevel {@code <i k="kind" t="Type" p="path" d="doc">name</i>}: something in scope. */
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "k", defaultImpl = UnlistedToplevel.class)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = LocalToplevel.class, name = "local"),
    @JsonSubTypes.Type(value = MemberToplevel.class, names = {"member", "static", "global"}),
    @JsonSubTypes.Type(value = EnumToplevel.class, name = "enum"),
    @JsonSubTypes.Type(value = EnumAbstractToplevel.class, name = "enumabstract"),
    @JsonSubTypes.Type(value = LiteralToplevel.class, name = "literal"),
    @JsonSubTypes.Type(value = PackageToplevel.class, name = "package"),
    @JsonSubTypes.Type(value = TypeToplevel.class, name = "type")})
  sealed interface ToplevelEntry {
    /** The item at {@code index}; null for a kind the plugin does not offer. */
    CompletionItem toItem(int index);
  }

  record LocalToplevel(@JsonProperty("t") String type, @JacksonXmlText String name) implements ToplevelEntry {

    @Override
    public CompletionItem toItem(int index) {
      return item("Local", name, null, type, null, index);
    }

    /** Whether this is the local haxe 3 lists for the name being typed: that name, of an unbound type. */
    boolean isPartialName(String partialName) {
      return !partialName.isEmpty() && name.equals(partialName) && type.startsWith(UNKNOWN_TYPE_PREFIX);
    }
  }

  /** A member, a static or a global; a global's {@code p} names the type declaring it. */
  record MemberToplevel(@JsonProperty("t") String type, @JsonProperty("d") String doc, @JacksonXmlText String name)
    implements ToplevelEntry {

    @Override
    public CompletionItem toItem(int index) {
      return item("ClassField", name, null, type, doc, index);
    }
  }

  record EnumToplevel(@JsonProperty("t") String type, @JsonProperty("d") String doc, @JacksonXmlText String name)
    implements ToplevelEntry {

    @Override
    public CompletionItem toItem(int index) {
      return item("EnumField", name, null, type, doc, index);
    }
  }

  record EnumAbstractToplevel(@JsonProperty("t") String type, @JsonProperty("d") String doc, @JacksonXmlText String name)
    implements ToplevelEntry {

    @Override
    public CompletionItem toItem(int index) {
      return item("EnumAbstractField", name, null, type, doc, index);
    }
  }

  record LiteralToplevel(@JacksonXmlText String name) implements ToplevelEntry {

    @Override
    public CompletionItem toItem(int index) {
      return item("Literal", name, null, null, null, index);
    }
  }

  record PackageToplevel(@JacksonXmlText String name) implements ToplevelEntry {

    @Override
    public CompletionItem toItem(int index) {
      return item("Package", name, name, null, null, index);
    }
  }

  /** A type, with its path ({@code p}) as the detail. */
  record TypeToplevel(@JsonProperty("p") String path, @JsonProperty("d") String doc, @JacksonXmlText String name)
    implements ToplevelEntry {

    @Override
    public CompletionItem toItem(int index) {
      return item("Type", name, path, null, doc, index);
    }
  }

  /** A kind the plugin does not offer. */
  record UnlistedToplevel() implements ToplevelEntry {

    @Override
    public CompletionItem toItem(int index) {
      return null;
    }
  }

  /** A {@code @type} answer's {@code <type p="position">Type</type>}: the position asked about and its printed type. */
  record TypeEntry(@JsonProperty("p") String position, @JacksonXmlText String type) {
  }

  /** The item; a blank doc counts as none. */
  private static CompletionItem item(String kind, String name, String detail, String printedType, String doc, int index) {
    String docComment = doc == null || doc.isBlank() ? null : doc;
    return new CompletionItem(kind, name, detail, null, Haxe3Types.parse(printedType), docComment, index, null);
  }

  private static String qualified(String qualifier, String name) {
    return qualifier.isEmpty() ? name : qualifier + "." + name;
  }
}
