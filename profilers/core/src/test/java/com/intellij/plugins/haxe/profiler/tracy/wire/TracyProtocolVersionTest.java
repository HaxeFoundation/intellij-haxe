package com.intellij.plugins.haxe.profiler.tracy.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * The per-version tables are generated from the client headers (see
 * {@link TracyQueueTable}); these pins guard the facts the reader relies
 * on, each traced to the header diff between the releases.
 */
@DisplayName("tracy receiver: protocol versions")
public class TracyProtocolVersionTest {

  /** (version, item count in its queue enum, welcome size). */
  static final List<Arguments> LAYOUTS = List.of(
    arguments(TracyProtocolVersion.V69, 113, 1178),
    arguments(TracyProtocolVersion.V74, 115, 1178),
    arguments(TracyProtocolVersion.V76, 117, 1170),
    arguments(TracyProtocolVersion.V82, 132, 1170));

  @ParameterizedTest(name = "{0}")
  @FieldSource("LAYOUTS")
  public void testTablesAndWelcomeMatchTheClientHeaders(TracyProtocolVersion version, int itemCount, int welcomeSize) {
    assertEquals(itemCount, version.table().size());
    assertEquals(welcomeSize, version.format().welcomeSize());
  }

  /** (version, item, wire ordinal, wire size incl. the type byte). */
  static final List<Arguments> KNOWN_ITEMS = List.of(
    // 0.12 inserted MemDiscard at 33, shifting everything after
    arguments(TracyProtocolVersion.V69, TracyQueueType.GpuZoneBegin, 33, 24),
    arguments(TracyProtocolVersion.V74, TracyQueueType.MemDiscard, 33, 21),
    arguments(TracyProtocolVersion.V74, TracyQueueType.GpuZoneBegin, 35, 24),
    // 0.12 grew the wakeup item by cpu + adjust fields
    arguments(TracyProtocolVersion.V69, TracyQueueType.ThreadWakeup, 47, 13),
    arguments(TracyProtocolVersion.V74, TracyQueueType.ThreadWakeup, 49, 16),
    // 0.13 inserted the GPU annotation items at 52 and 103
    arguments(TracyProtocolVersion.V76, TracyQueueType.GpuAnnotationName, 52, 10),
    arguments(TracyProtocolVersion.V74, TracyQueueType.SingleStringData, 98, 1),
    arguments(TracyProtocolVersion.V76, TracyQueueType.SingleStringData, 99, 1),
    // 0.14 packed zone begins/ends and grew the message items by a metadata byte
    arguments(TracyProtocolVersion.V82, TracyQueueType.ZoneBegin16, 21, 11),
    arguments(TracyProtocolVersion.V82, TracyQueueType.ZoneEnd16, 27, 3),
    arguments(TracyProtocolVersion.V82, TracyQueueType.ZoneEnd32, 26, 5),
    arguments(TracyProtocolVersion.V82, TracyQueueType.Message, 2, 10),
    arguments(TracyProtocolVersion.V82, TracyQueueType.MessageColor, 3, 13),
    arguments(TracyProtocolVersion.V82, TracyQueueType.SingleStringData8, 114, 1),
    // the alloc-srcloc begin hxcpp emits keeps its shape everywhere
    arguments(TracyProtocolVersion.V69, TracyQueueType.ZoneBeginAllocSrcLoc, 7, 9),
    arguments(TracyProtocolVersion.V82, TracyQueueType.ZoneBeginAllocSrcLoc, 7, 9));

  @ParameterizedTest(name = "{0} {1}")
  @FieldSource("KNOWN_ITEMS")
  public void testKnownItemsSitAtTheirWireOrdinals(TracyProtocolVersion version, TracyQueueType type, int ordinal, int size) {
    TracyQueueTable table = version.table();

    assertEquals(ordinal, table.ordinalOf(type));
    assertSame(type, table.of(ordinal));
    assertEquals(size, table.wireSize(type));
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(TracyProtocolVersion.class)
  public void testOlderVersionsLackTheItemsAddedLater(TracyProtocolVersion version) {
    boolean packed = version == TracyProtocolVersion.V82;
    assertEquals(packed, version.table().defines(TracyQueueType.ZoneEnd16));
    assertEquals(packed, version.format() instanceof TracyPackedWireFormat);
    assertEquals(version != TracyProtocolVersion.V69, version.table().defines(TracyQueueType.MemDiscard));
  }

  @Test
  @DisplayName("the wire number resolves to its version and unknown numbers to nothing")
  public void testTheWireNumberResolvesToItsVersionAndUnknownNumbersToNothing() {
    assertSame(TracyProtocolVersion.V76, TracyProtocolVersion.of(76));
    assertNull(TracyProtocolVersion.of(75));
  }

  @Test
  @DisplayName("the probe order tries current hxcpp first and the oldest client last")
  public void testTheProbeOrderTriesCurrentHxcppFirstAndTheOldestClientLast() {
    assertEquals(List.of(TracyProtocolVersion.V76, TracyProtocolVersion.V74, TracyProtocolVersion.V82, TracyProtocolVersion.V69),
                 TracyProtocolVersion.PROBE_ORDER);
  }

  @Test
  @DisplayName("a table never defines an item outside the canonical vocabulary")
  public void testATableNeverDefinesAnItemOutsideTheCanonicalVocabulary() {
    for (TracyProtocolVersion version : TracyProtocolVersion.values()) {
      TracyQueueTable table = version.table();
      for (int ordinal = 0; ordinal < table.size(); ordinal++) {
        TracyQueueType type = table.of(ordinal);
        assertNotNull(type);
        assertTrue(table.wireSize(type) >= 1, version + " " + type);
      }
      assertNull(table.of(table.size()));
      assertNull(table.of(-1));
    }
  }
}
