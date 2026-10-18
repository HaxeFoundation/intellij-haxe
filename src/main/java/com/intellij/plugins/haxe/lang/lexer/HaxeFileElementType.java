package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NotNull;

/**
 * The Haxe file node. Parses with a lexer whose {@code #if} evaluation
 * follows the file's own resolve context ({@link HaxeLexer#forFile}); the
 * platform's default would take the parser definition's project-wide lexer.
 */
public final class HaxeFileElementType extends IFileElementType {

  public HaxeFileElementType() {
    super("HAXEFILE", HaxeLanguage.INSTANCE);
  }

  @Override
  protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement psi) {
    Project project = psi.getProject();
    Lexer lexer = HaxeLexer.forFile(project, psi.getContainingFile());
    PsiBuilder builder = PsiBuilderFactory.getInstance()
      .createBuilder(project, chameleon, lexer, HaxeLanguage.INSTANCE, chameleon.getChars());
    PsiParser parser = LanguageParserDefinitions.INSTANCE.forLanguage(HaxeLanguage.INSTANCE).createParser(project);
    return parser.parse(this, builder).getFirstChildNode();
  }
}
