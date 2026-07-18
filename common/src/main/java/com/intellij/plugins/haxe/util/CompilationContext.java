package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import com.intellij.plugins.haxe.module.HaxeModuleSettingsBase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CompilationContext {
    HaxeSdkAdditionalDataBase getHaxeSdkData();

    @NotNull
    HaxeModuleSettingsBase getModuleSettings();

    String getModuleName();

    String getCompilationClass();
    String getOutputFileName();
    String getOutputDirectory();
    Boolean getIsTestBuild();

    void errorHandler(String message);
    void warningHandler(String message);
    void infoHandler(String message);

    void log(String message);

    String getSdkHomePath();

    public String getHaxelibPath();

    public String getNekoBinPath();

    boolean isDebug();

    String getSdkName();

    List<String> getSourceRoots();

    String getModuleDefaultCompileOutputPath();

    void setErrorRoot(String root);

    String getErrorRoot();

    void handleOutput(String[] lines);

    HaxeTarget getHaxeTarget();

    String getModuleDirPath();

    boolean isCancelled();

    /**
     * Called once if the compiler process is still running a while after a cancellation
     * request and a graceful stop attempt.  Implementations decide what to do with the
     * unresponsive process: offer the user a way to kill it (IDE), or kill it outright (JPS).
     */
    void handleUnresponsiveProcess(@NotNull Process process);
}
