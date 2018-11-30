// This version of this test file is extracted (without comments) from the Haxe Toolkit v. 3.4.2.
/*
 * Copyright (C)2005-2017 Haxe Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */


//import haxe.ds.StringMap;
//import haxe.ds.IntMap;
//import haxe.ds.HashMap;
//import haxe.ds.ObjectMap;
//import haxe.ds.WeakMap;
//import haxe.ds.EnumValueMap;
//import haxe.Constraints.IMap;

@:multiType(@:followWithAbstracts K)
abstract Map<K,V>(IMap<K,V> ) {
	public function new();
	public inline function set(key:K, value:V) this.set(key, value);
	@:arrayAccess public inline function get(key:K) return this.get(key);
	public inline function exists(key:K) return this.exists(key);
	public inline function remove(key:K) return this.remove(key);
	public inline function keys():Iterator<K> {
		return this.keys();
	}
	public inline function iterator():Iterator<V> {
		return this.iterator();
	}
	public inline function toString():String {
		return this.toString();
	}

	@:arrayAccess @:noCompletion public inline function arrayWrite(k:K, v:V):V {
		this.set(k, v);
		return v;
	}

// Commented out until we need them for tests (and we include summaries of the actual import files).
//	@:to static inline function toStringMap<K:String,V>(t:IMap<K,V>):StringMap<V> {
//		return new StringMap<V>();
//	}
//
//	@:to static inline function toIntMap<K:Int,V>(t:IMap<K,V>):IntMap<V> {
//		return new IntMap<V>();
//	}
//
//	@:to static inline function toEnumValueMapMap<K:EnumValue,V>(t:IMap<K,V>):EnumValueMap<K,V> {
//		return new EnumValueMap<K, V>();
//	}
//
//	@:to static inline function toObjectMap<K:{ },V>(t:IMap<K,V>):ObjectMap<K,V> {
//		return new ObjectMap<K, V>();
//	}
//
//	@:from static inline function fromStringMap<V>(map:StringMap<V>):Map< String, V > {
//		return cast map;
//	}
//
//	@:from static inline function fromIntMap<V>(map:IntMap<V>):Map< Int, V > {
//		return cast map;
//	}
//
//	@:from static inline function fromObjectMap<K:{ }, V>(map:ObjectMap<K,V>):Map<K,V> {
//		return cast map;
//	}
}

@:dox(hide)
@:deprecated
//typedef IMap<K, V> = haxe.Constraints.IMap<K, V>;
typedef IMap<K, V> = haxe_Constraints_IMap<K, V>;

// From haxe toolkit 3.4.2, std/haxe.Constraints.hx.  Same license as above.
interface haxe_Constraints_IMap<K,V> {
        public function get(k:K):Null<V>;
        public function set(k:K, v:V):Void;
        public function exists(k:K):Bool;
        public function remove(k:K):Bool;
        public function keys():Iterator<K>;
        public function iterator():Iterator<V>;
        public function toString():String;
}