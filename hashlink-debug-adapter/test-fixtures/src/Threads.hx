/**
 * Multi-threaded fixture (M14): its OWN program (main class) because it blocks
 * forever — it can't join the shared Main flow that other tests run to exit.
 *
 * A worker thread waits until main is parked in block() (holding a live local),
 * then prints on a known line where the test sets a breakpoint. At that stop
 * ALL threads are frozen: the worker is at its println, main is spinning in
 * block(). Both stacks + locals are then inspectable.
 *
 * WARNING: FIXTURE_THREADS_WORKER_LINE in DapIntegrationTestBase is the marked
 * line below — keep them in sync.
 */
class Threads {
	static var mainReady = false;
	static var release = false;
	static var gate = new sys.thread.Lock(); // never released: used to park the worker

	static function main():Void {
		release = Std.parseInt("0") > 0; // runtime false — defeats constant-folding of the spin loop
		var mainLocal = Std.parseInt("111") + 0; // plain Int 111
		sys.thread.Thread.create(worker);
		block(mainLocal);
	}

	static function block(v:Int):Void {
		mainReady = true; // signal only once v is live and we're about to spin
		var spins = v;
		while (!release) {
			spins++;
		}
		Sys.println("block-done:" + spins); // never reached in the test
	}

	static function worker():Void {
		while (!mainReady) {
			Sys.sleep(0.001); // yield + force a re-read of mainReady
		}
		var workerLocal = Std.parseInt("222") + 0; // plain Int 222
		Sys.println("worker:" + workerLocal); // FIXTURE_THREADS_WORKER_LINE = 39
		gate.wait(); // FIXTURE_THREADS_BLOCK_LINE = 40 — never released; step over blocks forever
	}
}
