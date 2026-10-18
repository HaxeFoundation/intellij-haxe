package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.display.protocol.JsonPos;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import com.intellij.plugins.haxe.display.protocol.server.TypeBlueprint;
import com.intellij.plugins.haxe.util.HaxeExternalPaths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The members of a build context as one {@code typer/compiledTypes} answer
 * describes them, indexed by file and text range, so a declaration finds its
 * compiler type by where it is. The compiler reports byte offsets into the
 * files as it read them; {@link HaxeByteOffsets} turns those into editor text
 * offsets with each file's bytes. A file the reader cannot supply is left out.
 */
final class HaxeCompiledMembers {

  /** The bytes of a file by its normalized path; null when the file cannot be read. */
  @FunctionalInterface
  interface FileBytes {
    byte @Nullable [] read(@NotNull String path);
  }

  /** A member's declaration range in editor text offsets and its compiler type. */
  record Member(int startOffset, int endOffset, @NotNull String name, @NotNull JsonTypeRef type) {
  }

  private final Map<String, List<Member>> byFile;
  private final int memberCount;

  private HaxeCompiledMembers(@NotNull Map<String, List<Member>> byFile, int memberCount) {
    this.byFile = byFile;
    this.memberCount = memberCount;
  }

  @NotNull
  static HaxeCompiledMembers of(@NotNull List<TypeBlueprint> types,
                                @NotNull String workDirectory,
                                boolean caseSensitivePaths,
                                @NotNull FileBytes reader) {
    Map<String, List<TypeBlueprint.Member>> wireByFile = new HashMap<>();
    for (TypeBlueprint type : types) {
      for (TypeBlueprint.Member member : positionedMembers(type)) {
        String path = normalizePath(member.pos().file(), workDirectory, caseSensitivePaths);
        wireByFile.computeIfAbsent(path, ignored -> new ArrayList<>()).add(member);
      }
    }

    Map<String, List<Member>> byFile = new HashMap<>();
    int count = 0;
    for (Map.Entry<String, List<TypeBlueprint.Member>> entry : wireByFile.entrySet()) {
      byte[] bytes = reader.read(entry.getKey());
      if (bytes == null) continue;
      List<Member> members = convert(entry.getValue(), HaxeByteOffsets.of(bytes));
      byFile.put(entry.getKey(), members);
      count += members.size();
    }
    return new HaxeCompiledMembers(byFile, count);
  }

  /** The fields and statics of a type that carry both a position and a type. */
  @NotNull
  private static List<TypeBlueprint.Member> positionedMembers(@NotNull TypeBlueprint type) {
    List<TypeBlueprint.Member> members = new ArrayList<>();
    for (List<TypeBlueprint.Member> group : List.of(type.fields(), type.statics())) {
      for (TypeBlueprint.Member member : group) {
        if (member.pos() != null && member.type() != null) members.add(member);
      }
    }
    return members;
  }

  @NotNull
  private static List<Member> convert(@NotNull List<TypeBlueprint.Member> wire, @NotNull HaxeByteOffsets offsets) {
    List<Member> members = new ArrayList<>();
    for (TypeBlueprint.Member member : wire) {
      JsonPos pos = member.pos();
      members.add(new Member(offsets.textOffset(pos.min()), offsets.textOffset(pos.max()), member.name(), member.type()));
    }
    return List.copyOf(members);
  }

  /** The member whose declaration contains the text offset in the file, or null. */
  @Nullable
  Member memberAt(@NotNull String normalizedPath, int offset) {
    List<Member> members = byFile.get(normalizedPath);
    if (members == null) return null;
    for (Member member : members) {
      if (member.startOffset() <= offset && offset < member.endOffset()) return member;
    }
    return null;
  }

  int fileCount() {
    return byFile.size();
  }

  int memberCount() {
    return memberCount;
  }

  /**
   * A file path as a lookup key: forward slashes, absolute (a relative one is
   * taken from the work directory the compile ran in), lower-cased on a
   * case-insensitive file system. A position naming no usable path keeps its
   * text.
   */
  @NotNull
  static String normalizePath(@NotNull String path, @NotNull String workDirectory, boolean caseSensitive) {
    Path parsed = HaxeExternalPaths.pathOf(path);
    String resolved = parsed == null ? path : Path.of(workDirectory).resolve(parsed).normalize().toString();
    String text = resolved.replace('\\', '/');
    return caseSensitive ? text : text.toLowerCase(Locale.ROOT);
  }
}
