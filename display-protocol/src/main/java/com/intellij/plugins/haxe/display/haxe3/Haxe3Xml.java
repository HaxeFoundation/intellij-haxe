package com.intellij.plugins.haxe.display.haxe3;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.intellij.plugins.haxe.display.transport.MalformedPayloadException;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.MutableCoercionConfig;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * Reads the XML of haxe 3's display answers into {@link Haxe3Wire.Answer}.
 * That XML is not always well-formed, so the payload is made so first: a
 * {@code <pos>} path is never escaped, so a bare {@code &} can stand in its
 * text, and a call's signature answers with one root {@code <type>} per
 * overload, so the whole answer goes under one {@code <answer>} root. The line
 * breaks and tabs of a start tag become character references, because a
 * conforming parser turns raw ones in an attribute value into spaces and would
 * flatten the multi-line docs of {@code d} attributes.
 */
final class Haxe3Xml {

  private static final JsonSetter.Value EMPTY_FOR_NULL = JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY);

  // repeated elements stand side by side, with no wrapper element around them
  private static final XmlMapper MAPPER = XmlMapper.builder()
    .defaultUseWrapper(false)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .withCoercionConfig(LogicalType.POJO, Haxe3Xml::blankAsAbsent)
    .withConfigOverride(List.class, override -> override.setNullHandling(EMPTY_FOR_NULL))
    .withConfigOverride(String.class, override -> override.setNullHandling(EMPTY_FOR_NULL))
    .build();

  // an '&' that starts none of XML's entity references: a raw one from an unescaped path
  private static final Pattern BARE_AMPERSAND = Pattern.compile("&(?!(?:lt|gt|amp|quot|apos|#\\d+|#x[0-9A-Fa-f]+);)");
  // a start or empty-element tag; haxe 3 escapes '<' and '>' in attribute values, so neither ends one early
  private static final Pattern START_TAG = Pattern.compile("<[A-Za-z][^<>]*>");

  private Haxe3Xml() {
  }

  static Haxe3Wire.Answer read(String payload) throws MalformedPayloadException {
    try {
      return MAPPER.readValue(wellFormed(payload), Haxe3Wire.Answer.class);
    } catch (JacksonException e) {
      throw new MalformedPayloadException("Malformed haxe 3 display answer: " + e.getOriginalMessage(), e);
    }
  }

  /** The payload as one well-formed document under an {@code <answer>} root. */
  private static String wellFormed(String payload) {
    String escaped = BARE_AMPERSAND.matcher(payload).replaceAll("&amp;");
    String breaksKept = START_TAG.matcher(escaped).replaceAll(Haxe3Xml::withBreaksAsReferences);
    return "<answer>" + breaksKept + "</answer>";
  }

  private static String withBreaksAsReferences(MatchResult tag) {
    String referenced = tag.group()
      .replace("\r", "&#13;")
      .replace("\n", "&#10;")
      .replace("\t", "&#9;");
    return Matcher.quoteReplacement(referenced);
  }

  /**
   * An element holding only whitespace, such as an empty {@code <list>}, reads
   * as absent: as a record it would be built without the empty-list defaults.
   */
  private static void blankAsAbsent(MutableCoercionConfig config) {
    config.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull)
      .setAcceptBlankAsEmpty(true);
  }
}
