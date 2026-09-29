package com.intellij.plugins.haxe.ide.refactoring;

import com.intellij.lang.refactoring.NamesValidator;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Decides what is a valid Haxe name. The platform asks before accepting a rename, the name suggesters before offering a name. */
public class HaxeNamesValidator implements NamesValidator {
  // an identifier as the compiler lexes it: an ASCII letter or underscore, then ASCII letters, digits and underscores
  private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
  /** The reserved words and the constant keywords; the soft keywords ({@code to}, {@code from}) remain usable as names. */
  private static final Set<String> KEYWORDS = keywords();

  public static boolean isIdentifier(@NotNull String name) {
    return IDENTIFIER.matcher(name).matches() && !isKeyword(name);
  }

  public static boolean isKeyword(@NotNull String name) {
    return KEYWORDS.contains(name);
  }

  @Override
  public boolean isKeyword(@NotNull String name, Project project) {
    return isKeyword(name);
  }

  @Override
  public boolean isIdentifier(@NotNull String name, Project project) {
    return isIdentifier(name);
  }

  @NotNull
  private static Set<String> keywords() {
    IElementType[] reserved = HaxeTokenTypeSets.KEYWORDS.getTypes();
    IElementType[] constants = HaxeTokenTypeSets.KEYWORD_CONSTANTS.getTypes();
    return Stream.concat(Arrays.stream(reserved), Arrays.stream(constants))
      .map(IElementType::toString)
      .collect(Collectors.toSet());
  }
}
