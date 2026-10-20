package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.ExternalAnnotatorBatchInspection;
import org.jetbrains.annotations.NotNull;

/**
 * The batch inspections paired with the compiler-diagnostics external
 * annotators ({@code ExternalAnnotator.getPairedBatchInspectionShortName}).
 * The pairing does two things. Inspect Code runs the annotator at all, since
 * it skips unpaired external annotators. And the findings appear under the
 * Haxe inspection group instead of the catch-all "General &gt; Annotator"
 * node. The compiler-settings toggles still decide first: a disabled feature
 * collects nothing in batch mode, just as in the editor.
 */
public final class HaxeCompilerDiagnosticsBatchInspections {

  public static final String ERRORS_SHORT_NAME = "HaxeCompilerDiagnostics";
  public static final String UNUSED_IMPORT_SHORT_NAME = "HaxeCompilerUnusedImport";
  public static final String REMOVABLE_CODE_SHORT_NAME = "HaxeCompilerRemovableCode";

  private HaxeCompilerDiagnosticsBatchInspections() {
  }

  public static class Errors extends LocalInspectionTool implements ExternalAnnotatorBatchInspection {
    @Override
    public @NotNull String getShortName() {
      return ERRORS_SHORT_NAME;
    }
  }

  public static class UnusedImport extends LocalInspectionTool implements ExternalAnnotatorBatchInspection {
    @Override
    public @NotNull String getShortName() {
      return UNUSED_IMPORT_SHORT_NAME;
    }
  }

  public static class RemovableCode extends LocalInspectionTool implements ExternalAnnotatorBatchInspection {
    @Override
    public @NotNull String getShortName() {
      return REMOVABLE_CODE_SHORT_NAME;
    }
  }
}
