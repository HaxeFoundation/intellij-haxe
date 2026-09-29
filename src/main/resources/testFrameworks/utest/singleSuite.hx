// A template for the entry point the IDE generates for a gutter-started
// run: the selected utest case classes of the tests build, compiled with
// that build's classpaths, defines and libraries. A single-test run adds
// -D UTEST_PATTERN on top. The IDE substitutes the NEW_SUITES token (one
// `new Suite()` per selected class) before the compile; this file is never
// compiled as it is.
class IjSingleRun {
	static function main() {
		utest.UTest.run([${NEW_SUITES}]);
	}
}
