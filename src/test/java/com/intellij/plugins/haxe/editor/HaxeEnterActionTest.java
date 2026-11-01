/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2020 Eric Bishton
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
package com.intellij.plugins.haxe.editor;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.AbstractEnterActionTestCase;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author winmain
 */
@DisplayName("Editor: enter action")
public class HaxeEnterActionTest extends AbstractEnterActionTestCase {

    /** (declaration keyword, the member line the caret ends). */
    static final List<Arguments> DECLARATION_BODIES = List.of(
            // enter in abstract
            arguments("abstract", "var a;"),
            // enter in class
            arguments("class", "var a;"),
            // enter in enum
            arguments("enum", "FOO;"),
            // enter in extern class
            arguments("extern class", "var a;"),
            // enter in interface
            arguments("interface", "function qwe():Void;"));

    @ParameterizedTest(name = "enter in {0}")
    @FieldSource("DECLARATION_BODIES")
    @DisplayName("enter after a member keeps the body indent")
    public void testEnterAfterAMemberKeepsTheBodyIndent(String keyword, String member) throws Throwable {
        String before = """
                %s Test {
                    %s<caret>
                }""".formatted(keyword, member);
        String after = """
                %s Test {
                    %s
                   \s
                }""".formatted(keyword, member);

        doTextTest("hx", before, after);
    }

    @Test
    @DisplayName("enter after documentation start")
    public void testEnterAfterDocumentationStart() throws Throwable {
        doTextTest("hx",
                """
                        class Test {
                            /**<caret>
                            function foo():Void {}
                        }
                        """,
                """
                        class Test {
                            /**
                                <caret>
                            **/
                            function foo():Void {}
                        }
                        """);
    }

    @Test
    @DisplayName("enter after documentation start in javadoc style")
    public void testEnterAfterDocumentationStartInJavadocStyle() throws Throwable {
        useDocCommentStyle(HaxeCodeStyleSettings.DOC_COMMENT_STYLE_JAVADOC);

        doTextTest("hx",
                """
                        class Test {
                            /**<caret>
                            function foo(a:Int):Int { return a; }
                        }
                        """,
                """
                        class Test {
                            /**
                             * <caret>
                             * @param a
                             * @return
                             */
                            function foo(a:Int):Int { return a; }
                        }
                        """);
    }

    @Test
    @DisplayName("enter after documentation start stubs the method tags")
    public void testEnterAfterDocumentationStartStubsTheMethodTags() throws Throwable {
        doTextTest("hx",
                """
                        class Test {
                            /**<caret>
                            function foo(a:Int, ?b:String):Int { return a; }
                        }
                        """,
                """
                        class Test {
                            /**
                                <caret>
                                @param a
                                @param b
                                @return
                            **/
                            function foo(a:Int, ?b:String):Int { return a; }
                        }
                        """);
    }

    @Test
    @DisplayName("enter after documentation start when closed")
    public void testEnterAfterDocumentationStartWhenClosed() throws Throwable {
        doTextTest("hx",
                """
                        class Test {
                            /**<caret>
                            **/
                            function foo():Void {}
                        }
                        """,
                """
                        class Test {
                            /**
                                <caret>
                            **/
                            function foo():Void {}
                        }
                        """);
    }

    @Test
    @DisplayName("enter after documentation start on line with content")
    public void testEnterAfterDocumentationStartOnLineWithContent() throws Throwable {
        doTextTest("hx",
                """
                        class Test {/**<caret>
                            function foo():Void {}
                        }
                        """,
                // TODO formatting
                """
                       class Test {/**
                           <caret>
                       **/
                           function foo():Void {}
                       }
                       """);
    }

    /** (case, style for new comments, before, after): a closed comment's new line follows the comment's own style. */
    static final List<Arguments> LINES_INSIDE_COMMENTS = List.of(
            arguments("a javadoc body line continues its star", HaxeCodeStyleSettings.DOC_COMMENT_STYLE_HAXE,
                      """
                              /**
                               * first<caret>
                               */
                              """,
                      """
                              /**
                               * first
                               * <caret>
                               */
                              """),
            arguments("a javadoc opener line starts a starred line", HaxeCodeStyleSettings.DOC_COMMENT_STYLE_HAXE,
                      """
                              /**<caret>
                               * text
                               */
                              """,
                      """
                              /**
                               * <caret>
                               * text
                               */
                              """),
            arguments("the text indent after a star stays", HaxeCodeStyleSettings.DOC_COMMENT_STYLE_HAXE,
                      """
                              /**
                               * @param a   the first
                               *   continued<caret>
                               */
                              """,
                      """
                              /**
                               * @param a   the first
                               *   continued
                               *   <caret>
                               */
                              """),
            arguments("text after the caret moves to the new starred line", HaxeCodeStyleSettings.DOC_COMMENT_STYLE_HAXE,
                      """
                              /**
                               * first<caret> second
                               */
                              """,
                      """
                              /**
                               * first
                               * <caret>second
                               */
                              """),
            arguments("enter before the javadoc closer adds an empty starred line", HaxeCodeStyleSettings.DOC_COMMENT_STYLE_HAXE,
                      """
                              /**
                               * text
                               <caret>*/
                              """,
                      """
                              /**
                               * text
                               *
                               <caret>*/
                              """),
            arguments("a haxedoc body line gets no star", HaxeCodeStyleSettings.DOC_COMMENT_STYLE_JAVADOC,
                      """
                              /**
                                  text<caret>
                              **/
                              """,
                      """
                              /**
                                  text
                                  <caret>
                              **/
                              """),
            arguments("a markdown bullet in a haxedoc comment gets no star", HaxeCodeStyleSettings.DOC_COMMENT_STYLE_HAXE,
                      """
                              /**
                                  Intro
                                  * item<caret>
                              **/
                              """,
                      """
                              /**
                                  Intro
                                  * item
                                  <caret>
                              **/
                              """),
            arguments("a comment without lines of its own takes the javadoc style", HaxeCodeStyleSettings.DOC_COMMENT_STYLE_JAVADOC,
                      """
                              /**<caret>
                              **/
                              """,
                      """
                              /**
                               * <caret>
                              **/
                              """));

    @ParameterizedTest(name = "{0}")
    @FieldSource("LINES_INSIDE_COMMENTS")
    @DisplayName("enter inside a comment continues its style")
    public void testEnterInsideACommentContinuesItsStyle(String name, int newCommentStyle, String before, String after) throws Throwable {
        useDocCommentStyle(newCommentStyle);

        doTextTest("hx", before + "class Test {}\n", after + "class Test {}\n");
    }

    private void useDocCommentStyle(int style) {
        CodeStyleSettingsManager manager = CodeStyleSettingsManager.getInstance(getProject());
        CodeStyleSettings settings = manager.cloneSettings(CodeStyle.getSettings(getProject()));
        settings.getCustomSettings(HaxeCodeStyleSettings.class).DOC_COMMENT_STYLE = style;
        manager.setTemporarySettings(settings);
    }
}
