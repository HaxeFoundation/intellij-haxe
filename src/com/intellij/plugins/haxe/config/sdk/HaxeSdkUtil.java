package com.intellij.plugins.haxe.config.sdk;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.JavadocOrderRootType;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HaxeSdkUtil {
  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil");
  private static final Pattern VERSION_MATCHER = Pattern.compile("(\\d+(\\.\\d+)+)");
  private static final String COMPILER_EXECUTABLE_NAME = "haxe";

  @Nullable
  public static HaxeSdkData testHaxeSdk(String path) {
    final String exePath = getCompilerPathByFolderPath(path);

    if (exePath == null) {
      return null;
    }

    final GeneralCommandLine command = new GeneralCommandLine();
    command.setExePath(exePath);
    command.addParameter("-help");
    command.setWorkDirectory(path);

    try {
      final ProcessOutput output = new CapturingProcessHandler(
        command.createProcess(),
        Charset.defaultCharset(),
        command.getCommandLineString()).runProcess();

      if (output.getExitCode() != 0) {
        LOG.error("haXe compiler exited with invalid exit code: " + output.getExitCode());
        return null;
      }

      final String outputString = output.getStdout();

      final Matcher matcher = VERSION_MATCHER.matcher(outputString);
      if (matcher.find()) {
        final HaxeSdkData haxeSdkData = new HaxeSdkData(path, matcher.group(1));
        haxeSdkData.setNekoBinPath(suggestNekoBinPath(path));
        return haxeSdkData;
      }

      return null;
    }
    catch (ExecutionException e) {
      LOG.info("Exception while executing the process:", e);
      return null;
    }
  }

  public static void setupSdkPaths(@Nullable VirtualFile sdkRoot, SdkModificator modificator) {
    if (sdkRoot == null) {
      return;
    }
    final VirtualFile stdRoot = sdkRoot.findChild("std");
    if (stdRoot != null) {
      modificator.addRoot(stdRoot, OrderRootType.SOURCES);
    }
    final VirtualFile libRoot = sdkRoot.findChild("lib");
    if (libRoot != null) {
      modificator.addRoot(libRoot, OrderRootType.SOURCES);
    }
    final VirtualFile docRoot = sdkRoot.findChild("doc");
    if (docRoot != null) {
      modificator.addRoot(docRoot, JavadocOrderRootType.getInstance());
    }
  }

  @Nullable
  private static String suggestNekoBinPath(@NotNull String path) {
    String result = System.getenv("NEKOPATH");
    if (result == null) {
      result = System.getenv("NEKO_INSTPATH");
    }
    if (result == null && !SystemInfo.isWindows) {
      final VirtualFile candidate = VirtualFileManager.getInstance().findFileByUrl("/usr/bin/neko");
      if (candidate != null && candidate.exists()) {
        return candidate.getPath();
      }
    }
    if (result == null) {
      final String parentPath = new File(path).getParent();
      result = new File(parentPath, "neko").getAbsolutePath();
    }
    if (result != null) {
      result = new File(result, getExecutableName("neko")).getAbsolutePath();
    }
    if (result != null && new File(result).exists()) {
      return result;
    }
    return null;
  }

  @Nullable
  public static String getCompilerPathByFolderPath(String folderPath) {
    final String folderUrl = VfsUtil.pathToUrl(folderPath);
    if (!SystemInfo.isLinux) {
      final String candidate = folderUrl + "/bin/" + getExecutableName(COMPILER_EXECUTABLE_NAME);
      if (fileExists(candidate)) {
        return VfsUtil.urlToPath(candidate);
      }
    }

    final String resultUrl = folderUrl + "/" + getExecutableName(COMPILER_EXECUTABLE_NAME);
    if (fileExists(resultUrl)) {
      return VfsUtil.urlToPath(resultUrl);
    }

    return null;
  }

  private static String getExecutableName(String name) {
    if (SystemInfo.isWindows) {
      return name + ".exe";
    }
    return name;
  }

  private static boolean fileExists(@Nullable String filePath) {
    return filePath != null && checkFileExists(VirtualFileManager.getInstance().findFileByUrl(filePath));
  }

  private static boolean checkFileExists(@Nullable VirtualFile file) {
    return file != null && file.exists();
  }

  @Nullable
  public static String suggestHomePath() {
    final String result = System.getenv("HAXEPATH");
    if (result == null && !SystemInfo.isWindows) {
      final String candidate = "/usr/lib/haxe";
      if (VirtualFileManager.getInstance().findFileByUrl(candidate) != null) {
        return candidate;
      }
    }
    return result;
  }
}
