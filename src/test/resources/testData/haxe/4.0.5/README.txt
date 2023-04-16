This is a STRIPPED DOWN version of the Toolkit's standard library. Only
the files necessary for testing (or foreseeable new tests) are included.

Do not add the full toolkit here as all of these files are copied for
every test that uses the standard library -- and copying all of the files
gets really slow (several seconds per test), even when the target-specific
directories are excluded.