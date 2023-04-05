/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.lang.parser.utils;

import com.intellij.lang.*;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handle PSI building when parsing languages.  Beware!  Using this will override *every* language;
 * HaxePsiBuilder will be used as the only PSIBuilder when this is installed.
 * <p>
 * Install this class by adding the following stanza to your src/META-INF/plugin.xml file.  (It is
 * currently in place, but commented out.)
 * <p>
 * <applicationService overrides="true" serviceInterface="com.intellij.lang.PsiBuilderFactory"
 * serviceImplementation="com.intellij.plugins.haxe.lang.parser.utils.HaxePsiBuilderFactoryImpl"/>
 */
public class HaxePsiBuilderFactoryImpl extends PsiBuilderFactory {

  @NotNull
  @Override
  public PsiBuilder createBuilder(@NotNull final Project project,
                                  @NotNull final ASTNode chameleon,
                                  @Nullable final Lexer lexer,
                                  @NotNull final Language lang,
                                  @NotNull final CharSequence seq) {
    final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
    return new HaxePsiBuilder(project, parserDefinition, lexer != null ? lexer : createLexer(project, lang), chameleon, seq);
  }

  @NotNull
  @Override
  public PsiBuilder createBuilder(@NotNull final Project project, @NotNull final ASTNode chameleon) {
    return createBuilder(project, chameleon, null, chameleon.getElementType().getLanguage(), chameleon.getChars());
  }

  @NotNull
  @Override
  public PsiBuilder createBuilder(@NotNull final Project project, @NotNull final LighterLazyParseableNode chameleon) {
    final Language language = chameleon.getTokenType().getLanguage();
    ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(language);

    return new HaxePsiBuilder(project, parserDefinition, createLexer(project, language), chameleon, chameleon.getText());
  }

  @NotNull
  @Override
  public PsiBuilder createBuilder(@NotNull final Project project,
                                  @NotNull final LighterLazyParseableNode chameleon,
                                  @Nullable final Lexer lexer,
                                  @NotNull final Language lang,
                                  @NotNull final CharSequence seq) {
    final Language language = chameleon.getTokenType().getLanguage();
    final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(language);
    return new HaxePsiBuilder(project, parserDefinition, lexer != null ? lexer : createLexer(project, lang), chameleon, seq);
  }

  @NotNull
  @Override
  public PsiBuilder createBuilder(@NotNull final ParserDefinition parserDefinition,
                                  @NotNull final Lexer lexer,
                                  @NotNull final CharSequence seq) {
    return new HaxePsiBuilder(parserDefinition, lexer, seq);
  }

  private static Lexer createLexer(final Project project, final Language lang) {
    final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
    assert parserDefinition != null : "ParserDefinition absent for language: " + lang.getID();
    return parserDefinition.createLexer(project);
  }
}
