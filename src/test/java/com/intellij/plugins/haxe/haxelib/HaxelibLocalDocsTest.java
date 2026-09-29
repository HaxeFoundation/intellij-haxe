package com.intellij.plugins.haxe.haxelib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/** Local repository layout resolution: comma version directories, the .dev pointer, and doc-file discovery. */
@DisplayName("Haxelib: local docs resolution")
public class HaxelibLocalDocsTest {

  @TempDir
  Path temp;

  @Test
  @DisplayName("release versions map dots to comma directories")
  public void testReleaseVersionsMapDotsToCommaDirectories() throws Exception {
    Path repo = repo();
    Path versionDir = Files.createDirectories(repo.resolve("lime/8,3,2"));

    assertEquals(versionDir, HaxelibLocalDocs.versionDirectory(repo, "lime", "8.3.2"));
    assertNull(HaxelibLocalDocs.versionDirectory(repo, "lime", "9.9.9"), "absent versions resolve to null");
  }

  @Test
  @DisplayName("dev pointer file drives both dev entry points")
  public void testDevPointerFileDrivesBothDevEntryPoints() throws Exception {
    Path repo = repo();
    Path devTarget = Files.createDirectories(temp.resolve("dev-target"));
    Files.createDirectories(repo.resolve("mylib"));
    Files.writeString(repo.resolve("mylib/.dev"), devTarget + System.lineSeparator());

    assertEquals(devTarget, HaxelibLocalDocs.versionDirectory(repo, "mylib", "dev"));
    assertEquals(devTarget.toString(), HaxelibLocalDocs.devPath(repo, "mylib"));
    assertNull(HaxelibLocalDocs.devPath(repo, "otherlib"), "no dev pointer resolves to null");
  }

  @Test
  @DisplayName("git checkout reads branch and commit from head and loose ref")
  public void testGitCheckoutReadsBranchAndCommitFromHeadAndLooseRef() throws Exception {
    Path repo = repo();
    Path gitDir = Files.createDirectories(repo.resolve("mylib/git/.git"));
    Files.writeString(gitDir.resolve("HEAD"), "ref: refs/heads/main\n");
    Files.createDirectories(gitDir.resolve("refs/heads"));
    Files.writeString(gitDir.resolve("refs/heads/main"), "0123456789abcdef0123456789abcdef01234567\n");

    HaxelibLocalDocs.GitCheckout checkout = HaxelibLocalDocs.gitCheckout(repo, "mylib");
    assertNotNull(checkout);
    assertEquals("main", checkout.branch());
    assertEquals("0123456789abcdef0123456789abcdef01234567", checkout.commit());
  }

  @Test
  @DisplayName("git checkout falls back to packed refs")
  public void testGitCheckoutFallsBackToPackedRefs() throws Exception {
    Path repo = repo();
    Path gitDir = Files.createDirectories(repo.resolve("packedlib/git/.git"));
    Files.writeString(gitDir.resolve("HEAD"), "ref: refs/heads/develop\n");
    Files.writeString(gitDir.resolve("packed-refs"), """
      # pack-refs with: peeled fully-peeled sorted
      fedcba9876543210fedcba9876543210fedcba98 refs/heads/develop
      """);

    HaxelibLocalDocs.GitCheckout packed = HaxelibLocalDocs.gitCheckout(repo, "packedlib");
    assertNotNull(packed);
    assertEquals("develop", packed.branch());
    assertEquals("fedcba9876543210fedcba9876543210fedcba98", packed.commit());
  }

  @Test
  @DisplayName("git checkout keeps slashes in the branch name")
  public void testGitCheckoutKeepsSlashesInTheBranchName() throws Exception {
    Path repo = repo();
    Path gitDir = Files.createDirectories(repo.resolve("slashlib/git/.git"));
    Files.writeString(gitDir.resolve("HEAD"), "ref: refs/heads/castlewars/prod\n");
    Files.createDirectories(gitDir.resolve("refs/heads/castlewars"));
    Files.writeString(gitDir.resolve("refs/heads/castlewars/prod"), "0123456789abcdef0123456789abcdef01234567\n");

    HaxelibLocalDocs.GitCheckout checkout = HaxelibLocalDocs.gitCheckout(repo, "slashlib");
    assertNotNull(checkout);
    assertEquals("castlewars/prod", checkout.branch());
    assertEquals("0123456789abcdef0123456789abcdef01234567", checkout.commit());
  }

  @Test
  @DisplayName("detached head yields a commit without a branch")
  public void testDetachedHeadYieldsACommitWithoutABranch() throws Exception {
    Path repo = repo();
    Path gitDir = Files.createDirectories(repo.resolve("detachedlib/git/.git"));
    Files.writeString(gitDir.resolve("HEAD"), "abcdef0123456789abcdef0123456789abcdef01\n");

    HaxelibLocalDocs.GitCheckout detached = HaxelibLocalDocs.gitCheckout(repo, "detachedlib");
    assertNotNull(detached);
    assertNull(detached.branch(), "a detached checkout has no branch");
    assertEquals("abcdef0123456789abcdef0123456789abcdef01", detached.commit());
  }

  @Test
  @DisplayName("doc files match well known names in display order regardless of case")
  public void testDocFilesMatchWellKnownNamesInDisplayOrderRegardlessOfCase() throws Exception {
    Path versionDir = Files.createDirectories(temp.resolve("version"));
    Files.writeString(versionDir.resolve("CHANGELOG.md"), "changes");
    Files.writeString(versionDir.resolve("ReadMe.MD"), "readme");
    Files.writeString(versionDir.resolve("LICENSE"), "license");
    Files.writeString(versionDir.resolve("Notes.md"), "not a doc tab");

    List<Path> docs = HaxelibLocalDocs.docFiles(versionDir);
    List<String> names = docs.stream().map(p -> p.getFileName().toString()).toList();
    assertEquals(List.of("ReadMe.MD", "CHANGELOG.md", "LICENSE"), names,
                 "readme first, then changelog and license; unrelated markdown excluded");
  }

  private Path repo() throws IOException {
    return Files.createDirectories(temp.resolve("repo"));
  }
}
