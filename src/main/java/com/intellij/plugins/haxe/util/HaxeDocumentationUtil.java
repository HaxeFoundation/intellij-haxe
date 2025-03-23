package com.intellij.plugins.haxe.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

/**
 * Note: This util is just here to help extracting documentation for as long as we treat documentation as one single tag
 * if at some point start lazy parsing it these  util methods probably wont  work or be nessesary
 */
public class HaxeDocumentationUtil {


  @NotNull
  public static String unwrapCommentDelimiters(@NotNull String text) {
    if (text.startsWith("/**")) text = text.substring("/**".length());
    if (text.startsWith("/*")) text = text.substring("/*".length());
    if (text.startsWith("//")) text = text.substring("//".length());
    if (text.endsWith("**/")) text = text.substring(0, text.length() - "**/".length());
    if (text.endsWith("*/")) text = text.substring(0, text.length() - "*/".length());
    return text;
  }



  public static String removeExcessLines(String docs) {
    String[] split = docs.split("\n");
    if (split.length == 1)  return docs;
    // multi-line docs will contain the empty lins after /**  and before */
    List<String> fragments = new ArrayList<>(List.of(split));

    // remove empty lines at the beginning
      for (int i = 0; i < split.length; i++) {
          String fragment = split[i];
          if (fragment.isBlank()) fragments.removeFirst();
          break;
      }

    // remove empty lines at the end
    for (int i = split.length - 1; i >= 0; i--) {
      String fragment = split[i];
      if (fragment.isBlank()) fragments.removeLast();
      break;
    }


    return String.join("\n", fragments);
  }

  public static boolean docIsJavadocStyle(String rawText) {
    String[] split1 = rawText.split("\n");
    return Arrays.stream(split1).allMatch(str -> str.matches("^\\s*\\*.*"));
  }

  public static String stripIndents(String docs, boolean javaDocStyle) {
    String[] split = docs.split("\n");
    if(javaDocStyle) {
      return Arrays.stream(split).map(s-> s.replaceFirst("\\s*\\*","")).collect(Collectors.joining("\n"));
    }else {
      // workaround for docs with empty lines and  "content lines" with indentations
      Optional<Integer> min = Arrays.stream(split).filter(not(String::isBlank)).map(s -> s.stripLeading().length() - s.length()).min(Integer::compare);
      if(min.isPresent()) {
        Integer i = min.get();
        return Arrays.stream(split)
                .map(s ->  s.isBlank() ? s.indent(i) : s)
                .collect(Collectors.joining("\n"))
                .stripIndent();
      }
      return docs.stripIndent();
    }
  }

  public static String tryFixIndents(String extractedDocs) {
    String[] split = extractedDocs.split("\n");
    int lineCount = split.length;
    for (int i = 0; i < lineCount; i++) {

      int lineBeforeIndex = i - 1;
      int lineAfterIndex = i + 1;

      if (lineBeforeIndex > 0) {
        if (lineAfterIndex < lineCount) {
          String line = split[i];
          String lineBefore = split[lineBeforeIndex];
          String lineAfter = split[lineAfterIndex];
          if (line.isEmpty()) {
            int beforeIndents = countIndents(lineBefore);
            int afterIndents = countIndents(lineAfter);
            if (beforeIndents == afterIndents && afterIndents != 0) {
              String indents = lineAfter.substring(0, beforeIndents);
              split[i] = indents +line+"<br>";
            }
          }
        }

      }
    }
    return String.join("\n", split);
  }

  private static int countIndents(String lineBefore) {
    return lineBefore.length() - lineBefore.stripLeading().length();
  }
}
