package com.intellij.plugins.haxe.profiler.flash;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Streaming AMF3 value decoder covering the types the flash runtime's
 * telemetry channel emits. The three reference tables (strings, complex
 * objects, object traits) are stream-global, so one decoder instance must
 * read the whole stream in order — values late in a session reference
 * strings and traits announced kilobytes earlier.
 *
 * Decoded values map to: null, {@link #UNDEFINED}, Boolean, Long (integer
 * marker), Double, String, byte[] (ByteArray), List (arrays and vectors),
 * Map (dictionary) and {@link Amf3Object} (typed objects).
 */
public final class Amf3Decoder {

  /** AMF3 {@code undefined}, kept distinct from {@code null}. */
  public static final Object UNDEFINED = new Object();

  private final DataInputStream in;
  private final List<String> strings = new ArrayList<>();
  private final List<Object> objects = new ArrayList<>();
  private final List<Traits> traits = new ArrayList<>();

  /** A typed AMF3 object: the traits' class name plus its members in declaration order. */
  public record Amf3Object(@NotNull String className, @NotNull Map<String, Object> members) {
    public @Nullable Object member(@NotNull String name) {
      return members.get(name);
    }
  }

  private record Traits(String className, List<String> members, boolean dynamic) {}

  public Amf3Decoder(@NotNull InputStream in) {
    this.in = new DataInputStream(in);
  }

  /** Reads one complete value; {@link EOFException} on a stream that ends at or inside it. */
  public @Nullable Object readValue() throws IOException {
    int marker = readByte();
    return switch (marker) {
      case 0x00 -> UNDEFINED;
      case 0x01 -> null;
      case 0x02 -> Boolean.FALSE;
      case 0x03 -> Boolean.TRUE;
      case 0x04 -> readSignedU29();
      case 0x05 -> in.readDouble();
      case 0x06 -> readString();
      case 0x07, 0x0b -> readXml();
      case 0x08 -> readDate();
      case 0x09 -> readArray();
      case 0x0a -> readObject();
      case 0x0c -> readByteArray();
      case 0x0d, 0x0e -> readVector(false, () -> (long)in.readInt());
      case 0x0f -> readVector(false, in::readDouble);
      case 0x10 -> readVector(true, this::readValue);
      case 0x11 -> readDictionary();
      default -> throw new ProfilerFormatException("unhandled AMF3 marker 0x" + Integer.toHexString(marker));
    };
  }

  private Object readObject() throws IOException {
    int flags = readU29();
    if ((flags & 1) == 0) return objects.get(flags >> 1);
    Traits objectTraits = readTraits(flags);
    Map<String, Object> members = new LinkedHashMap<>();
    Amf3Object object = new Amf3Object(objectTraits.className(), members);
    objects.add(object);
    for (String member : objectTraits.members()) {
      members.put(member, readValue());
    }
    if (objectTraits.dynamic()) {
      while (true) {
        String key = readString();
        if (key.isEmpty()) break;
        members.put(key, readValue());
      }
    }
    return object;
  }

  private Traits readTraits(int flags) throws IOException {
    if ((flags & 2) == 0) return traits.get(flags >> 2);
    if ((flags & 4) != 0) {
      // an externalizable body has no self-describing layout to skip over
      throw new ProfilerFormatException("externalizable AMF3 object " + readString());
    }
    boolean dynamic = (flags & 8) != 0;
    int memberCount = flags >> 4;
    String className = readString();
    List<String> members = new ArrayList<>(memberCount);
    for (int i = 0; i < memberCount; i++) {
      members.add(readString());
    }
    Traits read = new Traits(className, members, dynamic);
    traits.add(read);
    return read;
  }

  private Object readArray() throws IOException {
    int flags = readU29();
    if ((flags & 1) == 0) return objects.get(flags >> 1);
    int dense = flags >> 1;
    List<Object> array = new ArrayList<>(dense);
    objects.add(array);
    while (true) {
      String key = readString();
      if (key.isEmpty()) break;
      readValue(); // associative entries - the telemetry stream never uses them
    }
    for (int i = 0; i < dense; i++) {
      array.add(readValue());
    }
    return array;
  }

  private byte[] readByteArray() throws IOException {
    int flags = readU29();
    if ((flags & 1) == 0) return (byte[])objects.get(flags >> 1);
    byte[] bytes = readFully(flags >> 1);
    objects.add(bytes);
    return bytes;
  }

  /** One vector element, read in the vector's element encoding. */
  private interface ElementReader {
    Object read() throws IOException;
  }

  /** A Vector.&lt;int|uint|Number|Object&gt;; an object vector also names its element type, which is skipped. */
  private Object readVector(boolean namesElementType, ElementReader element) throws IOException {
    int flags = readU29();
    if ((flags & 1) == 0) return objects.get(flags >> 1);
    int count = flags >> 1;
    readByte(); // fixed-length flag
    if (namesElementType) readString();
    List<Object> vector = new ArrayList<>(count);
    objects.add(vector);
    for (int i = 0; i < count; i++) {
      vector.add(element.read());
    }
    return vector;
  }

  private Object readDictionary() throws IOException {
    int flags = readU29();
    if ((flags & 1) == 0) return objects.get(flags >> 1);
    int count = flags >> 1;
    readByte(); // weak-keys flag
    Map<Object, Object> map = new LinkedHashMap<>();
    objects.add(map);
    for (int i = 0; i < count; i++) {
      map.put(readValue(), readValue());
    }
    return map;
  }

  private Object readDate() throws IOException {
    int flags = readU29();
    if ((flags & 1) == 0) return objects.get(flags >> 1);
    Double epochMillis = in.readDouble();
    objects.add(epochMillis);
    return epochMillis;
  }

  private Object readXml() throws IOException {
    int flags = readU29();
    if ((flags & 1) == 0) return objects.get(flags >> 1);
    String text = new String(readFully(flags >> 1), StandardCharsets.UTF_8);
    objects.add(text);
    return text;
  }

  private String readString() throws IOException {
    int flags = readU29();
    if ((flags & 1) == 0) return strings.get(flags >> 1);
    String text = new String(readFully(flags >> 1), StandardCharsets.UTF_8);
    if (!text.isEmpty()) strings.add(text); // the empty string never enters the table
    return text;
  }

  private long readSignedU29() throws IOException {
    int value = readU29();
    return value > 0x0FFFFFFF ? value - 0x20000000 : value;
  }

  private int readU29() throws IOException {
    int value = 0;
    for (int i = 0; i < 3; i++) {
      int b = readByte();
      if ((b & 0x80) == 0) return value << 7 | b;
      value = value << 7 | b & 0x7F;
    }
    return value << 8 | readByte();
  }

  private byte[] readFully(int length) throws IOException {
    if (length < 0) throw new ProfilerFormatException("negative AMF3 length " + length);
    byte[] bytes = new byte[length];
    in.readFully(bytes);
    return bytes;
  }

  private int readByte() throws IOException {
    return in.readUnsignedByte();
  }
}
