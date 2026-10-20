package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.util.HaxeNameSuggesterUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The two lookups offered where a function is expected: an arrow function
 * and a function literal, both shaped by the expected signature. Choosing
 * one inserts a live template with one stop per parameter name; each stop
 * offers the suggested names for its parameter, and the caret ends up in
 * the body. A function literal that returns a value starts its body with
 * {@code return}.
 *
 * Completion treats the template's stops as ordinary code positions, so
 * while the template is active no lambda is offered inside it, and
 * {@link HaxeLambdaCompletionConfidence} keeps the auto-popup closed there.
 */
public final class HaxeLambdaLookups {

  /** Above every ordinary item, as the expected enum values are. */
  private static final double LAMBDA_PRIORITY = 1000;
  private static final String TEMPLATE_KEY = "haxe.lambda";
  private static final String TEMPLATE_GROUP = "haxe";

  private HaxeLambdaLookups() {
  }

  /** Adds the lambda lookups for the shape, unless a lambda template is being filled in at the caret. */
  public static void addTo(@NotNull CompletionResultSet result, @NotNull HaxeLambdaShape shape, @NotNull CompletionParameters parameters) {
    if (isLambdaTemplateActive(parameters.getPosition().getProject(), parameters.getEditor().getDocument())) return;
    List<List<String>> names = suggestedNames(shape, parameters.getPosition());
    result.addElement(PrioritizedLookupElement.withPriority(arrowFunction(shape, names), LAMBDA_PRIORITY));
    result.addElement(PrioritizedLookupElement.withPriority(functionLiteral(shape, names), LAMBDA_PRIORITY));
  }

  /** Whether an editor of the document is filling in a lambda template. */
  public static boolean isLambdaTemplateActive(@NotNull Project project, @NotNull Document document) {
    TemplateManager templates = TemplateManager.getInstance(project);
    for (Editor editor : EditorFactory.getInstance().getEditors(document, project)) {
      Template active = templates.getActiveTemplate(editor);
      if (active != null && TEMPLATE_KEY.equals(active.getKey())) return true;
    }
    return false;
  }

  /** The suggested names of every parameter, best first; a parameter avoids the names the earlier parameters lead with. */
  @NotNull
  private static List<List<String>> suggestedNames(@NotNull HaxeLambdaShape shape, @NotNull PsiElement context) {
    List<List<String>> names = new ArrayList<>();
    Set<String> taken = new HashSet<>();
    for (HaxeLambdaShape.Parameter parameter : shape.parameters()) {
      List<String> suggestions = HaxeNameSuggesterUtil.suggestForType(
        parameter.declaredName(), parameter.typeName(), parameter.isFunction(), context, taken);
      names.add(suggestions);
      taken.add(suggestions.getFirst());
    }
    return names;
  }

  /** {@code i -> }, or {@code (a, b) -> } for any other parameter count. */
  @NotNull
  private static LookupElement arrowFunction(@NotNull HaxeLambdaShape shape, @NotNull List<List<String>> names) {
    List<String> defaults = defaults(names);
    boolean bare = defaults.size() == 1;
    String presentable = (bare ? defaults.getFirst() : parenthesized(defaults)) + " ->";
    String template = (bare ? placeholder(0) : parenthesizedPlaceholders(defaults.size())) + " -> $END$";
    return lookup(presentable, template, names, false);
  }

  /** {@code function(a, b) { }} with the caret inside, after a {@code return} when the function has a result. */
  @NotNull
  private static LookupElement functionLiteral(@NotNull HaxeLambdaShape shape, @NotNull List<List<String>> names) {
    List<String> defaults = defaults(names);
    String presentable = "function" + parenthesized(defaults) + " {}";
    String body = shape.returnsVoid() ? "$END$" : "return $END$;";
    String template = "function" + parenthesizedPlaceholders(defaults.size()) + " {\n" + body + "\n}";
    return lookup(presentable, template, names, true);
  }

  @NotNull
  private static List<String> defaults(@NotNull List<List<String>> names) {
    return names.stream().map(List::getFirst).toList();
  }

  @NotNull
  private static LookupElement lookup(@NotNull String presentable, @NotNull String template, @NotNull List<List<String>> names, boolean reformat) {
    return LookupElementBuilder.create(presentable)
      .withIcon(AllIcons.Nodes.Lambda)
      .withTailText(" " + HaxeBundle.message("haxe.completion.lambda.tail"), true)
      .withInsertHandler(templateInsert(template, names, reformat));
  }

  /** Replaces the inserted lookup text with the template; every parameter stop offers its names. */
  @NotNull
  private static InsertHandler<LookupElement> templateInsert(@NotNull String templateText, @NotNull List<List<String>> names, boolean reformat) {
    return (context, item) -> {
      context.getDocument().deleteString(context.getStartOffset(), context.getTailOffset());
      context.commitDocument();
      TemplateManager templates = TemplateManager.getInstance(context.getProject());
      Template template = templates.createTemplate(TEMPLATE_KEY, TEMPLATE_GROUP, templateText);
      for (int i = 0; i < names.size(); i++) {
        ParameterNames stop = new ParameterNames(names.get(i));
        template.addVariable(variableName(i), stop, stop, true);
      }
      template.setToReformat(reformat);
      templates.startTemplate(context.getEditor(), template);
    };
  }

  /** A parameter stop's value: the first suggestion, with all of them as the stop's lookup. */
  private static final class ParameterNames extends Expression {
    private final List<String> suggestions;

    ParameterNames(@NotNull List<String> suggestions) {
      this.suggestions = suggestions;
    }

    @Override
    public @Nullable Result calculateResult(ExpressionContext context) {
      return new TextResult(suggestions.getFirst());
    }

    @Override
    public LookupElement @Nullable [] calculateLookupItems(ExpressionContext context) {
      return suggestions.stream()
        .map(LookupElementBuilder::create)
        .toArray(LookupElement[]::new);
    }
  }

  @NotNull
  private static String parenthesized(@NotNull List<String> names) {
    return names.stream().collect(Collectors.joining(", ", "(", ")"));
  }

  @NotNull
  private static String parenthesizedPlaceholders(int count) {
    return IntStream.range(0, count)
      .mapToObj(HaxeLambdaLookups::placeholder)
      .collect(Collectors.joining(", ", "(", ")"));
  }

  /** The template text that marks a parameter's stop, such as {@code $PARAM0$}. */
  @NotNull
  private static String placeholder(int index) {
    return "$" + variableName(index) + "$";
  }

  @NotNull
  private static String variableName(int index) {
    return "PARAM" + index;
  }
}
