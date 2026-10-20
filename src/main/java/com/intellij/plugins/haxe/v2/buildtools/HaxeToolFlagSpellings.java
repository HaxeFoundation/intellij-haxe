package com.intellij.plugins.haxe.v2.buildtools;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A build tool's forwarding forms for the plain hxml flags a launch adds
 * ({@code -D}, {@code -cp}, {@code --macro}); each flag's VALUE is appended to
 * its form as one argument, so the form's ending (nothing, a space or
 * {@code =}) decides how the tool sees the flag and its value joined.
 */
public record HaxeToolFlagSpellings(@NotNull String defineForm, @NotNull String classpathForm, @NotNull String macroForm) {

  /** Respells plain hxml arguments into the tool's forwarding forms; anything else passes through. */
  @NotNull
  public List<String> respell(@NotNull List<String> plainArguments) {
    List<String> spelled = new ArrayList<>();
    for (int i = 0; i < plainArguments.size(); i++) {
      String argument = plainArguments.get(i);
      switch (argument) {
        case "-D" -> spelled.add(defineForm + plainArguments.get(++i));
        case "-cp" -> spelled.add(classpathForm + plainArguments.get(++i));
        case "--macro" -> spelled.add(macroForm + plainArguments.get(++i));
        default -> spelled.add(argument);
      }
    }
    return spelled;
  }
}
