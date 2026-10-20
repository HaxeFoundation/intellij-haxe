package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.display.client.HaxeVersionProbe;
import com.intellij.plugins.haxe.display.protocol.InitializeResult.SemVer;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeServerGeneration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link HaxeServerGeneration} an SDK's haxe speaks: from the SDK's
 * version, else from the executable's own {@code -version} answer, asked
 * once per executable for the IDE session. A haxe found on the PATH, with no
 * SDK registered, has no known version until asked.
 */
public final class HaxeServerGenerations {

  private static final Map<String, HaxeServerGeneration> ANSWERED = new ConcurrentHashMap<>();

  private HaxeServerGenerations() {
  }

  /** The SDK's generation; a haxe of unknown version is asked. Background threads only: asking runs haxe. */
  @NotNull
  public static HaxeServerGeneration of(@NotNull Project project, @Nullable String sdkName) {
    HaxeServerGeneration fromSdk = fromSdk(project, sdkName);
    if (fromSdk != null) return fromSdk;
    String executablePath = HaxeToolPathResolver.resolveHaxeExecutable(project, sdkName);
    return ANSWERED.computeIfAbsent(executablePath, HaxeServerGenerations::ask);
  }

  /** The SDK's generation as far as it is known without running haxe; haxe 4 while nobody asked. Safe under the read lock. */
  @NotNull
  public static HaxeServerGeneration known(@NotNull Project project, @Nullable String sdkName) {
    HaxeServerGeneration fromSdk = fromSdk(project, sdkName);
    if (fromSdk != null) return fromSdk;
    String executablePath = HaxeToolPathResolver.resolveHaxeExecutable(project, sdkName);
    return ANSWERED.getOrDefault(executablePath, HaxeServerGeneration.HAXE_4);
  }

  @Nullable
  private static HaxeServerGeneration fromSdk(@NotNull Project project, @Nullable String sdkName) {
    HaxeLanguageLevel level = HaxeLanguageLevelUtil.fromSdkName(project, sdkName);
    return level == null ? null : HaxeServerGeneration.of(level);
  }

  /** The generation the executable's {@code -version} answer names; haxe 3 prints it on stderr, newer ones on stdout. */
  @NotNull
  private static HaxeServerGeneration ask(@NotNull String executablePath) {
    try {
      ProcessOutput output = ExecUtil.execAndGetOutput(new GeneralCommandLine(executablePath, "-version"));
      SemVer version = HaxeVersionProbe.parse(output.getStdout() + "\n" + output.getStderr());
      return version != null && version.major() < 4 ? HaxeServerGeneration.HAXE_3 : HaxeServerGeneration.HAXE_4;
    } catch (ExecutionException e) {
      return HaxeServerGeneration.HAXE_4;
    }
  }
}
