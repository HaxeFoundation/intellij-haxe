package com.intellij.plugins.haxe.display.protocol;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.List;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The JSON mapper the display protocol's records bind with. Every compiler
 * version adds properties of its own, so unknown ones are ignored, and what
 * the wire leaves out or sends as null reads as empty: a list as an empty
 * list, a string as {@code ""}, a number as 0. A component whose absence must
 * stay visible keeps null with {@code @JsonSetter(nulls = Nulls.SET)}. An
 * enum number no constant has reads as the enum's
 * {@code @JsonEnumDefaultValue}, or as null without one, and a value whose
 * type its sibling {@code kind} names reads as null when the wire leaves it
 * out.
 */
public final class ProtocolJson {

  private static final JsonSetter.Value EMPTY_FOR_NULL = JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY);

  public static final ObjectMapper MAPPER = JsonMapper.builder()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
    .disable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY)
    .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
    .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
    .withConfigOverride(List.class, override -> override.setNullHandling(EMPTY_FOR_NULL))
    .withConfigOverride(String.class, override -> override.setNullHandling(EMPTY_FOR_NULL))
    .build();

  private ProtocolJson() {
  }
}
