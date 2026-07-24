package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResult;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeExpressionCodeFragment;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.CompletionItem;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

/**
 * Runtime-truth completion for the debugger's evaluate/watch fragments: asks
 * the running DAP session's adapter to complete the text (DAP
 * {@code completions}) and merges the results with the PSI-based haxe
 * completion. This is the fallback for identifiers the PSI cannot know —
 * browser globals reached through incomplete externs, dynamically attached
 * fields — which the RUNTIME evaluates fine (evaluation never needed the
 * resolve) but the editor could neither complete nor recognize. Only adapters
 * declaring supportsCompletionsRequest participate (js-debug does; the
 * firefox adapter and the haxe-side servers do not).
 *
 * Runs EARLY in the chain (after the controlling contributor) so it can let
 * the PSI contributors produce their items first via
 * {@code runRemainingContributors} and then add ONLY the runtime names the
 * PSI did not already offer — an extern-mapped field must not appear twice.
 */
public class DapRuntimeCompletionContributor extends CompletionContributor {

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    if (!(parameters.getOriginalFile() instanceof HaxeExpressionCodeFragment)) {
      return; // debugger fragments only - never regular editors
    }
    Project project = parameters.getOriginalFile().getProject();
    XDebugSession session = XDebuggerManager.getInstance(project).getCurrentSession();
    if (session == null || !(session.getDebugProcess() instanceof DapDebugProcess process)
        || !process.supportsRuntimeCompletions()) {
      return;
    }

    // let the PSI contributors run (their items pass through to the popup)
    // and remember what they offered, so runtime items only fill the GAPS
    LinkedHashSet<CompletionResult> psiResults = result.runRemainingContributors(parameters, true);
    Set<String> alreadyOffered = new HashSet<>();
    for (CompletionResult psiResult : psiResults) {
      alreadyOffered.add(psiResult.getLookupElement().getLookupString());
    }

    String text = parameters.getOriginalFile().getText();
    int column = parameters.getOffset() + 1; // DAP columns are 1-based
    List<CompletionItem> items = process.requestRuntimeCompletions(text, column);
    for (CompletionItem item : items) {
      String label = item.getLabel() != null ? item.getLabel() : item.getText();
      if (label == null || label.isBlank()) {
        continue;
      }
      String insert = item.getText() != null ? item.getText() : label;
      if (!alreadyOffered.add(insert)) {
        continue; // the PSI (or an earlier runtime item) already offers it
      }
      result.addElement(LookupElementBuilder.create(insert)
                          .withPresentableText(label)
                          .withTypeText("runtime", true)
                          .withIcon(iconFor(item.getType())));
    }
  }

  private static Icon iconFor(String type) {
    return switch (type == null ? "" : type) {
      case "method", "function", "constructor" -> AllIcons.Nodes.Method;
      case "field", "property" -> AllIcons.Nodes.Field;
      case "variable" -> AllIcons.Nodes.Variable;
      case "class", "interface" -> AllIcons.Nodes.Class;
      default -> AllIcons.Nodes.Unknown;
    };
  }
}
