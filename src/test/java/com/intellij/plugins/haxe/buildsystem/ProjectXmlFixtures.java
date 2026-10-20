package com.intellij.plugins.haxe.buildsystem;

/** Project-xml sources shared by the build-file recognizer tests: the scanner's type detection and the DOM descriptions. */
public final class ProjectXmlFixtures {

  private ProjectXmlFixtures() {
  }

  public static final String CLASSPATH_ONLY_PROJECT_SOURCE = """
    <project>
      <app main="Main"/>
      <classpath path="src"/>
    </project>
    """;

  public static final String NME_SAMPLE_PROJECT_SOURCE = """
    <project>
      <app main="Main" file="Sample"/>
      <window width="800" height="600"/>
    </project>
    """;

  /** The lime elements sit inside a conditional section, not directly under the root. */
  public static final String SECTIONED_LIME_PROJECT_SOURCE = """
    <project>
      <section if="desktop">
        <app main="Main"/>
        <source path="src"/>
        <haxelib name="lime"/>
      </section>
    </project>
    """;

  /** A library's include file: lime reads its {@code <extension>} root like a project. */
  public static final String LIBRARY_INCLUDE_SOURCE = """
    <extension>
      <source path="src"/>
      <haxelib name="lime"/>
    </extension>
    """;

  public static final String MAVEN_POM_SOURCE = """
    <project>
      <modelVersion>4.0.0</modelVersion>
      <artifactId>shapes</artifactId>
    </project>
    """;
}
