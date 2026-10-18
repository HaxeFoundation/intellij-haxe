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

import com.intellij.codeInsight.AbstractEnterActionTestCase;
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
                              \s
                            **/
                            function foo():Void {}
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
                // TODO formatting should probably indent this the same way as testEnterAfterDocumentationStart
                """
                        class Test {
                            /**
                           \s
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
                         \s
                       **/
                           function foo():Void {}
                       }
                       """);
    }
}
