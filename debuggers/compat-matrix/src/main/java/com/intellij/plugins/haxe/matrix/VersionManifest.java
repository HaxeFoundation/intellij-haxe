package com.intellij.plugins.haxe.matrix;

import java.util.List;

/**
 * The toolchain versions the matrix certifies, with their per-OS release
 * assets. Compile-checked on purpose: adding a version to certify is adding
 * one line here. A null asset means the project publishes no binary for that
 * OS (HashLink stopped shipping linux binaries after 1.6 — provide those by
 * building from source into debuggerResources/hashlink/<name>/, they are
 * DISCOVERED next to the downloaded ones).
 *
 * Support floor (haxe >= 4.1, HashLink >= 1.13) is simply "not listed here".
 */
final class VersionManifest {

  /**
   * One downloadable toolchain, pinned PER PLATFORM: the url and its SHA-256
   * are selected together for the current OS, and the download is verified
   * against the pin BEFORE anything is extracted. Hash sources: node from
   * the official (signed) SHASUMS256.txt; haxe 5-preview from the GitHub
   * release-asset digest; the older haxe/HashLink assets predate GitHub's
   * digests, so their pins were computed from a reviewed download. A null
   * sha256 is reserved for artifacts that MOVE by design (the HashLink
   * nightly) — everything versioned is pinned.
   */
  record Tool(String name, String url, String sha256) {
    boolean downloadable() {
      return url != null;
    }
  }

  private static final String HAXE = "https://github.com/HaxeFoundation/haxe/releases/download/";
  private static final String HL = "https://github.com/HaxeFoundation/hashlink/releases/download/";
  // GitHub Actions artifacts need an auth token; nightly.link is the
  // standard tokenless mirror for a branch's latest successful artifacts
  private static final String HL_NIGHTLY = "https://nightly.link/HaxeFoundation/hashlink/workflows/build/master/";
  private static final String NODE = "https://nodejs.org/dist/";

  private static Tool haxe(String name, String tag, String winSha256, String linuxSha256) {
    String asset = Platform.WINDOWS ? "haxe-" + tag + "-win64.zip" : "haxe-" + tag + "-linux64.tar.gz";
    return new Tool(name, HAXE + tag + "/" + asset, Platform.WINDOWS ? winSha256 : linuxSha256);
  }

  private static Tool hashlink(String name, String tag, String windowsAsset, String winSha256) {
    return new Tool(name, Platform.WINDOWS ? HL + tag + "/" + windowsAsset : null,
                    Platform.WINDOWS ? winSha256 : null);
  }

  private static Tool hashlinkNightly() {
    // the nightly is re-downloaded when its version directory is deleted;
    // unlike releases it MOVES, so delete debuggerResources/hashlink/
    // hashlink-nightly to pick up a newer master build - and a moving
    // artifact cannot carry a hash pin (the one deliberate null)
    String asset = Platform.WINDOWS ? "windows-cmake-64.zip" : "linux-cmake-64.zip";
    return new Tool("hashlink-nightly", HL_NIGHTLY + asset, null);
  }

  private static Tool node(String name, String version, String winSha256, String linuxSha256) {
    String asset = Platform.WINDOWS
                   ? "node-" + version + "-win-x64.zip"
                   : "node-" + version + "-linux-x64.tar.gz";
    return new Tool(name, NODE + version + "/" + asset,
                    Platform.WINDOWS ? winSha256 : linuxSha256);
  }

  // Newest first: this is the run order, so a run stopped early still
  // certifies the versions most users are on. The report re-sorts columns
  // ascending itself.
  static List<Tool> haxeVersions() {
    return List.of(
      haxe("haxe_5_preview_1", "5.0.0-preview.1",
           "c223025518c6a527c66bd6c9ca51b4eff848ffcac97fc6c1833d1338cef1622e",
           "57710c7219c2d23bbd490cc5ed49e43686a946ab3a4910a7983a9d15fb078732"),
      haxe("haxe_4_3_7", "4.3.7",
           "29f7acb0fb9fc66a2b9f6bd9453af3474ccb14ebd9fd0142f351d7311c4010c9",
           "a156b3d039daa572f1f9329870ee753e3c39b7514fe8c818069323579659acca"),
      haxe("haxe_4_3_0", "4.3.0",
           "35d4d0e1f00a8b6904acd0e0a32a3e3abd4f33c5143b7ddc569f58a2504d2ece",
           "b1bcd3b75e2324a100ecefe8f231d611b2e6947108898c6c6026830b2ab9b847"),
      haxe("haxe_4_2_5", "4.2.5",
           "9e7913999eb3693d540926219b45107b3dc249feb44204c0378fcdc6a74a9132",
           "8670bf2f2950380c62450990f8a1b3a0fff9b27653c8f31f7cb7fbcae24c1b70"),
      haxe("haxe_4_1_5", "4.1.5",
           "ce4134cdf49814f8f8694648408d006116bd171b957a37be74c79cf403db9633",
           "e3a263476ccf575602126ba19f13da7e133f68a0c9493642e5fdaaa44437f4de"));
  }

  static List<Tool> hashlinkVersions() {
    return List.of(
      hashlink("hashlink-1.13.0", "1.13", "hashlink-1.13.0-win.zip",
               "b87557f30fbfd5ff382063317333cdce980b226c959586b9d71723c83d231934"),
      hashlink("hashlink-1.14.0", "1.14", "hashlink-1.14.0-win.zip",
               "528551550d518e3c2dd111fb4ecc886089da63452da6d7e813c37fdb8f9c8abd"),
      hashlink("hashlink-1.15.0", "1.15", "hashlink-1.15.0-win.zip",
               "69f1e2af38e8e912ee7fcd6869ba4a2eb352ad1b577701aee5503d7fd775657e"),
      hashlinkNightly());
  }

  /**
   * Node runtimes for the web-debugger lanes (they run the vscode DAP
   * adapters on node): the active LTS and the current release, pinned with
   * the official SHASUMS256.txt values (bumping a version means updating
   * the hashes from https://nodejs.org/dist/&lt;version&gt;/SHASUMS256.txt).
   * The browsers themselves are NOT provisioned — firefox/chromium must be
   * installed on the machine (or configured in the run configuration).
   */
  static List<Tool> nodeVersions() {
    return List.of(
      node("node-24.18.0-lts", "v24.18.0",
           "0ae68406b42d7725661da979b1403ec9926da205c6770827f33aac9d8f26e821",
           "783130984963db7ba9cbd01089eaf2c2efb055c7c1693c943174b967b3050cb8"),
      node("node-26.5.0-current", "v26.5.0",
           "d3b2277dbcccfdf24ef6302928f64f484cff1d77a6d3caa3a28f4d20ce9158f6",
           "22b5f47ad6ae78837e4c2b846019965ce1a06ba143de176102294a1bf44fc677"));
  }

  /**
   * haxe versions whose HL fixture behaviour was proven identical on every
   * runtime: unless --full, they run against the reference runtimes only
   * (latest release + nightly, so a runtime regression still gets caught).
   */
  static final List<String> DEGRADED_HAXE_ON_HL = List.of("haxe_4_1_5", "haxe_4_2_5");
  static final List<String> REFERENCE_RUNTIMES = List.of("hashlink-1.15.0", "hashlink-nightly");
}
