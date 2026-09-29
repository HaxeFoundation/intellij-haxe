package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.util.HaxeModuleVariants;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Stream;

/**
 * Line-based parser for hxml files, extracting the compilation target, defines and
 * haxelib dependencies. Referenced hxml files (a bare {@code common.hxml} line) are
 * merged first through {@link #flatten}; a {@code --next} chain splits into isolated
 * per-compilation blocks through {@code HaxeBuildFileInspector.sectionContents}
 * (PSI-driven, so it cannot live here), each parsed on its own.
 */
public final class HxmlFileParser {

  /** Reads an included hxml's content by the reference spelled in the file, or null when unreadable. */
  @FunctionalInterface
  public interface IncludeReader {
    @Nullable
    String read(@NotNull String path);
  }

  // written above every expanded reference so a section keeps the name of the
  // file it came from (--next chains are typically one include per section)
  private static final String INCLUDE_MARKER = "# include ";

  /**
   * The section separators. A trailing token on their line is ONE argument of
   * the FOLLOWING content — an hxml reference or a dot path; the compiler
   * takes the whole rest as a single argument and rejects flags there
   * ({@code --next -js out.js} fails as "unknown option").
   */
  private static final String NEXT_SEPARATOR = "--next";
  private static final String EACH_SEPARATOR = "--each";
  private static final Set<String> SECTION_FLAGS = Set.of(NEXT_SEPARATOR, EACH_SEPARATOR);

  private static final Map<String, HaxeTarget> TARGET_FLAGS = buildTargetFlagMap();

  /** The define flag's two spellings; the flag's value is {@code name} or {@code name=value}. */
  public static final Set<String> DEFINE_FLAGS = Set.of("-D", "--define");

  /** Haxe 5's {@code --custom-target name[=path]}: the name becomes the platform name. */
  private static final String CUSTOM_TARGET_FLAG = "--custom-target";

  private static final Set<String> LIBRARY_FLAGS = Set.of("-lib", "--library", "-L");
  private static final Set<String> CLASSPATH_FLAGS = Set.of("-cp", "-p", "--class-path");
  private static final Set<String> DEBUG_FLAGS = Set.of("-debug", "--debug");
  private static final Set<String> MAIN_FLAGS = Set.of("-main", "--main", "-m");

  private HxmlFileParser() {
  }

  /** Whether the flag token starts a new {@code --next} compilation section. */
  public static boolean isNextSeparator(@NotNull String token) {
    return token.equals(NEXT_SEPARATOR);
  }

  /** Whether the flag token starts the shared {@code --each} block. */
  public static boolean isEachSeparator(@NotNull String token) {
    return token.equals(EACH_SEPARATOR);
  }

  /**
   * The build's EFFECTIVE hxml: every referenced-hxml line replaced by that
   * file's content, recursively. haxe resolves a reference against the
   * invocation's working directory, NOT the file that declares it (a nested
   * include in a subfolder still reads next to the root), so one reader
   * rooted at the build file's directory serves
   * every nesting level. A revisited reference is dropped: haxe would loop on
   * such input. Unreadable references stay as lines; {@link #parse} skips them.
   * Each expansion is preceded by a comment naming the included file (see
   * {@link #leadingIncludedFile}), and a reference trailing a separator
   * ({@code --next other.hxml} inside an included file) is split off so it
   * still expands.
   */
  @NotNull
  public static String flatten(@NotNull String content, @NotNull IncludeReader reader) {
    StringBuilder flat = new StringBuilder();
    flattenInto(content, reader, new HashSet<>(), flat);
    return flat.toString();
  }

  private static void flattenInto(@NotNull String content,
                                  @NotNull IncludeReader reader,
                                  @NotNull Set<String> visited,
                                  @NotNull StringBuilder out) {
    for (String rawLine : content.lines().toList()) {
      appendFlattened(rawLine, reader, visited, out);
    }
  }

  private static void appendFlattened(@NotNull String rawLine,
                                      @NotNull IncludeReader reader,
                                      @NotNull Set<String> visited,
                                      @NotNull StringBuilder out) {
    String line = rawLine.trim();
    // the flag token and the rest of the line; a separator's trailing token
    // belongs to the FOLLOWING section
    String[] tokens = line.split("\\s+", 2);
    if (SECTION_FLAGS.contains(tokens[0]) && tokens.length > 1) {
      out.append(tokens[0]).append('\n');
      appendFlattened(tokens[1], reader, visited, out);
      return;
    }
    if (isIncludeReference(line)) {
      if (!visited.add(line)) return;
      String included = reader.read(line);
      if (included != null) {
        out.append(INCLUDE_MARKER).append(line).append('\n');
        flattenInto(included, reader, visited, out);
        return;
      }
    }
    out.append(rawLine).append('\n');
  }

  /**
   * The hxml file this section's LEADING content was expanded from by
   * {@link #flatten}, or null when the section starts with inline lines.
   * The FIRST marker wins: it is the chain's own entry, and any marker
   * stacked after it is a nested include — that entry's implementation detail.
   */
  @Nullable
  public static String leadingIncludedFile(@NotNull String sectionContent) {
    for (String rawLine : sectionContent.lines().toList()) {
      String line = rawLine.trim();
      if (line.startsWith(INCLUDE_MARKER)) {
        String reference = line.substring(INCLUDE_MARKER.length()).trim();
        if (isIncludeReference(reference)) return reference;
        continue;
      }
      if (isSignificantLine(line)) break;
    }
    return null;
  }

  /** A line the compiler acts on - not blank, not a comment (which covers the {@code # include} markers). */
  private static boolean isSignificantLine(@NotNull String rawLine) {
    String line = rawLine.trim();
    return !line.isEmpty() && !line.startsWith("#");
  }

  /**
   * A stable identity per section content (see
   * {@code HaxeBuildFileInspector.sectionContents}): the file the section's
   * leading content came from (the root file's name for inline sections),
   * with {@code #n} appended from the second occurrence on
   * ({@code compile-cs.hxml}, {@code compile-cs.hxml#2}). Selections stored by
   * identity survive chain edits; a removed section simply stops matching.
   */
  @NotNull
  public static List<String> sectionIds(@NotNull String rootFileName, @NotNull List<String> sections) {
    List<String> ids = new ArrayList<>();
    Map<String, Integer> occurrences = new HashMap<>();
    for (String section : sections) {
      String leading = leadingIncludedFile(section);
      String name = leading != null ? leading : rootFileName;
      int occurrence = occurrences.merge(name, 1, Integer::sum);
      ids.add(occurrence == 1 ? name : name + "#" + occurrence);
    }
    return ids;
  }

  /**
   * One human descriptor per section for chooser UIs: the compilation
   * target's display name (the {@code -main} class's simple name for a
   * target-less section), extended where sections would otherwise read the
   * same — first with the target flag's output argument, then with the main
   * class — so each descriptor states what sets its section apart. Empty for
   * a section declaring neither target nor main. Index-aligned with
   * {@code sections}.
   */
  @NotNull
  public static List<String> sectionDescriptors(@NotNull List<String> sections) {
    List<HaxeBuildFileInfo> infos = sections.stream().map(HxmlFileParser::parse).toList();
    List<String> mains = sections.stream().map(HxmlFileParser::mainClass).toList();

    List<String> descriptors = new ArrayList<>();
    for (int i = 0; i < sections.size(); i++) {
      descriptors.add(baseDescriptor(infos.get(i).target(), mains.get(i)));
    }
    extendCollidingDescriptors(descriptors, i -> infos.get(i).targetOutput());
    extendCollidingDescriptors(descriptors, i -> mains.get(i) == null ? null : StringUtil.getShortName(mains.get(i)));
    return descriptors;
  }

  /** The target's display name, else the main class's simple name, else empty. */
  @NotNull
  private static String baseDescriptor(@Nullable HaxeTarget target, @Nullable String main) {
    if (target != null) return target.toString();
    if (main != null) return StringUtil.getShortName(main);
    return "";
  }

  /**
   * Appends the extra to every member of a group of EQUAL non-empty
   * descriptors — colliding sections grow the next differentiator while
   * already-distinct ones stay short. A group whose extras are all the same
   * is left alone (appending a shared value separates nothing); a remaining
   * tie is the section number's job.
   */
  private static void extendCollidingDescriptors(@NotNull List<String> descriptors,
                                                 @NotNull IntFunction<@Nullable String> extras) {
    Map<String, List<Integer>> groups = new LinkedHashMap<>();
    for (int i = 0; i < descriptors.size(); i++) {
      if (!descriptors.get(i).isEmpty()) {
        groups.computeIfAbsent(descriptors.get(i), key -> new ArrayList<>()).add(i);
      }
    }
    for (List<Integer> group : groups.values()) {
      if (group.size() < 2) continue;
      Set<String> distinctExtras = new HashSet<>();
      for (int i : group) distinctExtras.add(extras.apply(i));
      if (distinctExtras.size() < 2) continue;
      for (int i : group) {
        String extra = extras.apply(i);
        if (extra == null || extra.isBlank() || extra.equals(descriptors.get(i))) continue;
        descriptors.set(i, descriptors.get(i) + " · " + extra);
      }
    }
  }

  /** Parses effective (include-merged) hxml content — see {@link #flatten}. */
  @NotNull
  public static HaxeBuildFileInfo parse(@NotNull String content) {
    ParseAccumulator accumulator = new ParseAccumulator();
    for (HxmlLine line : significantLines(content).toList()) {
      String flag = line.flag();
      String value = line.value();

      if (DEFINE_FLAGS.contains(flag) && value != null) {
        accumulator.defines.add(parseDefine(value));
      }
      else if (LIBRARY_FLAGS.contains(flag) && value != null) {
        accumulator.libraries.add(parseLibrary(value));
      }
      else if (CLASSPATH_FLAGS.contains(flag) && value != null) {
        accumulator.classpaths.add(value);
      }
      else if (TARGET_FLAGS.containsKey(flag)) {
        if (accumulator.target == null) {
          accumulator.target = TARGET_FLAGS.get(flag);
          accumulator.targetOutput = value;
        }
      }
      else if (CUSTOM_TARGET_FLAG.equals(flag) && value != null) {
        if (accumulator.customTarget == null) {
          // "name" or "name=path" - the path is the generator, irrelevant here
          accumulator.customTarget = value.split("=", 2)[0].trim();
        }
      }
    }
    return accumulator.toInfo();
  }

  /** One significant hxml line as (flag, value); the value is null for a bare flag. */
  private record HxmlLine(@NotNull String flag, @Nullable String value) {
  }

  /** The compiler-visible lines tokenised — the one line grammar {@link #parse} and {@link #mainClass} share. */
  @NotNull
  private static Stream<HxmlLine> significantLines(@NotNull String content) {
    return content.lines()
      .filter(HxmlFileParser::isSignificantLine)
      .map(HxmlFileParser::tokeniseLine);
  }

  @NotNull
  private static HxmlLine tokeniseLine(@NotNull String rawLine) {
    // the flag token and the rest of the line, which stays ONE argument
    String[] tokens = rawLine.trim().split("\\s+", 2);
    return new HxmlLine(tokens[0], tokens.length > 1 ? tokens[1].trim() : null);
  }

  // "name" or "name=value"
  @NotNull
  private static HaxeDefine parseDefine(@NotNull String value) {
    String[] parts = value.split("=", 2);
    return new HaxeDefine(parts[0], parts.length > 1 ? parts[1] : null);
  }

  // "name", "name:1.2.0" or "name:git:https://..."
  @NotNull
  private static HaxeLibDependency parseLibrary(@NotNull String value) {
    String[] parts = value.split(":", 2);
    return new HaxeLibDependency(parts[0], parts.length > 1 ? parts[1] : null);
  }

  private static boolean isIncludeReference(@NotNull String token) {
    return !token.startsWith("-") && token.endsWith(".hxml");
  }

  /** The target a compiler flag selects ({@code -js}, {@code --jvm}, ...), or null for non-target flags. */
  @Nullable
  public static HaxeTarget targetForFlag(@NotNull String flag) {
    return TARGET_FLAGS.get(flag);
  }

  /** Whether effective (include-merged) content declares a debug compile — hxcpp renames its binary on it. */
  public static boolean hasDebugFlag(@NotNull String content) {
    return content.lines()
      .map(String::trim)
      .anyMatch(DEBUG_FLAGS::contains);
  }

  /** Whether the flag declares the build's main class ({@code -main}/{@code --main}/{@code -m}). */
  public static boolean isMainFlag(@NotNull String flag) {
    return MAIN_FLAGS.contains(flag);
  }

  /**
   * The build's {@code --main} class from effective (include-merged) content
   * (first declaration wins), or null when none is declared. Hxcpp names the
   * produced executable after this class's simple name, which is why it is
   * read separately from {@link #parse} - only artifact launching needs it.
   */
  @Nullable
  public static String mainClass(@NotNull String content) {
    return significantLines(content)
      .filter(line -> MAIN_FLAGS.contains(line.flag()) && line.value() != null)
      .map(HxmlLine::value)
      .findFirst()
      .orElse(null);
  }

  private static Map<String, HaxeTarget> buildTargetFlagMap() {
    Map<String, HaxeTarget> map = new HashMap<>();
    for (HaxeTarget target : HaxeTarget.values()) {
      // INTERP's flag is "-interp", so both spellings below already yield "--interp".
      map.put("-" + target.getFlag(), target);
      map.put("--" + target.getFlag(), target);
    }
    // aliases the enum does not carry
    map.put("-jvm", HaxeTarget.JAVA);
    map.put("--jvm", HaxeTarget.JAVA);
    map.put("-as3", HaxeTarget.FLASH);

    return Map.copyOf(map);
  }

  /** One parse run's mutable state (first target flag wins). */
  private static final class ParseAccumulator {
    final List<HaxeDefine> defines = new ArrayList<>();
    final List<HaxeLibDependency> libraries = new ArrayList<>();
    final List<String> classpaths = new ArrayList<>();

    @Nullable HaxeTarget target;
    @Nullable String targetOutput;
    @Nullable String customTarget;

    @NotNull
    HaxeBuildFileInfo toInfo() {
      // a custom target surfaces as the defines the compiler sets for it, so
      // define consumers (CC evaluation, variant activation) need no extra field
      if (customTarget != null && !customTarget.isEmpty()) {
        defines.add(new HaxeDefine(HaxeModuleVariants.CUSTOM_TARGET_DEFINE, null));
        defines.add(new HaxeDefine(HaxeModuleVariants.TARGET_NAME_DEFINE, customTarget));
      }
      return new HaxeBuildFileInfo(target, targetOutput,
                                   List.copyOf(defines),
                                   List.copyOf(libraries),
                                   List.copyOf(classpaths));
    }
  }
}
