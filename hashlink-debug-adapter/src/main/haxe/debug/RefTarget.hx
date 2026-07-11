package debug;

/**
 * What a DAP `variablesReference` resolves to. References are handed out per stop
 * and invalidated on resume. Step 2 adds object/array targets.
 */
enum RefTarget {
	RefLocals(frameId:Int);
}
