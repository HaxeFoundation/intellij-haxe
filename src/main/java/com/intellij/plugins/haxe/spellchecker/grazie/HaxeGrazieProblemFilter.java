package com.intellij.plugins.haxe.spellchecker.grazie;

import com.intellij.grazie.text.ProblemFilter;
import com.intellij.grazie.text.TextProblem;
import org.jetbrains.annotations.NotNull;

public class HaxeGrazieProblemFilter  extends ProblemFilter {
    @Override
    public boolean shouldIgnore(@NotNull TextProblem problem) {
        return false;
    }
}
