package com.intellij.plugins.haxe.config.sdk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("SDK: std class paths")
public class HaxeStdClassPathsTest {

    /**
     * (HAXE_STD_PATH value, compiler directory, unix, std folder candidates in search order).
     */
    static final List<Arguments> STD_FOLDER_CANDIDATES = List.of(
            // a set variable replaces the defaults, every entry in order
            arguments("C:\\HaxeToolkit\\haxe\\std", "C:/HaxeToolkit/haxe", false, List.of("C:\\HaxeToolkit\\haxe\\std")),
            arguments("C:/does-not-exist;C:/HaxeToolkit/haxe/std", "C:/HaxeToolkit/haxe", false, List.of("C:/does-not-exist", "C:/HaxeToolkit/haxe/std")),
            arguments("/opt/haxe/std:/opt/more", "/usr/bin", true, List.of("/opt/haxe/std", "/opt/more")),

            // a folder listed twice becomes one candidate
            arguments("C:/HaxeToolkit/haxe/std;C:/HaxeToolkit/haxe/std", "C:/HaxeToolkit/haxe", false, List.of("C:/HaxeToolkit/haxe/std")),

            // set but empty: the compiler finds no standard library either
            arguments("", "/usr/bin", true, List.of()),
            arguments(null, "C:/HaxeToolkit/haxe", false, List.of("C:/HaxeToolkit/haxe/std")),

            // distribution package
            arguments(null, "/usr/bin", true, List.of("/usr/lib/haxe/std", "/usr/share/haxe/std", "/usr/bin/std")),

            // Homebrew: the launch path's directory, not the symlink target's
            arguments(null, "/opt/homebrew/bin", true, List.of("/opt/homebrew/lib/haxe/std", "/opt/homebrew/share/haxe/std", "/opt/homebrew/bin/std")),

            // unpacked release archive
            arguments(null, "/opt/haxe", true, List.of("/opt/lib/haxe/std", "/opt/share/haxe/std", "/opt/haxe/std")));

    @ParameterizedTest(name = "{0} / {1}")
    @FieldSource("STD_FOLDER_CANDIDATES")
    @DisplayName("lists the compilers std folders")
    public void listsTheCompilersStdFolders(String haxeStdPathValue, String compilerDirectory, boolean unix, List<String> candidates) {
        assertIterableEquals(candidates, HaxeStdClassPaths.candidates(haxeStdPathValue, Path.of(compilerDirectory), unix));
    }
}
