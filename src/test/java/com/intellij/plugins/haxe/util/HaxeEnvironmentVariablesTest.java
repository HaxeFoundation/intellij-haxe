package com.intellij.plugins.haxe.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Util: environment variables")
public class HaxeEnvironmentVariablesTest {

    /**
     * (variable value, entries the toolchain reads from it).
     */
    static final List<Arguments> PATH_LIST_VALUES = List.of(
            arguments(null, List.of()),
            arguments("", List.of()),
            arguments("/usr/share/haxe/std", List.of("/usr/share/haxe/std")),
            arguments("/usr/share/haxe/std:/opt/haxe/extraLibs", List.of("/usr/share/haxe/std", "/opt/haxe/extraLibs")),

            // a NEKOPATH as Neko reads it: Homebrew's library folder before a user's own
            arguments("/opt/homebrew/lib/neko:/usr/local/lib/neko", List.of("/opt/homebrew/lib/neko", "/usr/local/lib/neko")),

            // the ':' split tears the drive letter off; it is glued back on
            arguments("C:\\HaxeToolkit\\haxe\\std", List.of("C:\\HaxeToolkit\\haxe\\std")),
            arguments("C:\\HaxeToolkit\\haxe\\std;D:/libs", List.of("C:\\HaxeToolkit\\haxe\\std", "D:/libs")),

            // the compiler also accepts ':' between Windows entries
            arguments("C:/haxe/std:D:/libs", List.of("C:/haxe/std", "D:/libs")),

            // a trailing separator (setx leaves one behind) adds no entry
            arguments("C:/haxe/std;", List.of("C:/haxe/std")),
            arguments(";;", List.of()));

    // "[{index}] {0}": the null and empty rows would otherwise render a BLANK
    // display name, which the platform rejects
    @ParameterizedTest(name = "[{index}] {0}")
    @FieldSource("PATH_LIST_VALUES")
    @DisplayName("parses the toolchains path list")
    public void parsesTheToolchainsPathList(String value, List<String> entries) {
        assertEquals(entries, HaxeEnvironmentVariables.parsePathList(value));
    }
}
