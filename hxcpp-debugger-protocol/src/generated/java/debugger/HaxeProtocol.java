package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class HaxeProtocol extends haxe.lang.HxObject
{
	static 
	{
		debugger.HaxeProtocol.gClientIdentification = "Haxe debug client v1.1 coming at you!\n\n";
		debugger.HaxeProtocol.gServerIdentification = "Haxe debug server v1.1 ready and willing, sir!\n\n";
	}
	public    HaxeProtocol(haxe.lang.EmptyObject empty)
	{
		{
		}
		
	}
	
	
	public    HaxeProtocol()
	{
		debugger.HaxeProtocol.__hx_ctor_debugger_HaxeProtocol(this);
	}
	
	
	public static   void __hx_ctor_debugger_HaxeProtocol(debugger.HaxeProtocol __temp_me23)
	{
		{
		}
		
	}
	
	
	public static   void writeClientIdentification(haxe.io.Output output)
	{
		output.writeString(debugger.HaxeProtocol.gClientIdentification);
	}
	
	
	public static   void writeServerIdentification(haxe.io.Output output)
	{
		output.writeString(debugger.HaxeProtocol.gServerIdentification);
	}
	
	
	public static   void readClientIdentification(haxe.io.Input input)
	{
		haxe.io.Bytes id = input.read(debugger.HaxeProtocol.gClientIdentification.length());
		if ( ! (haxe.lang.Runtime.valEq(id.toString(), debugger.HaxeProtocol.gClientIdentification)) ) 
		{
			throw haxe.lang.HaxeException.wrap(( "Unexpected client identification string: " + haxe.root.Std.string(id) ));
		}
		
	}
	
	
	public static   void readServerIdentification(haxe.io.Input input)
	{
		haxe.io.Bytes id = input.read(debugger.HaxeProtocol.gServerIdentification.length());
		if ( ! (haxe.lang.Runtime.valEq(id.toString(), debugger.HaxeProtocol.gServerIdentification)) ) 
		{
			throw haxe.lang.HaxeException.wrap(( "Unexpected server identification string: " + haxe.root.Std.string(id) ));
		}
		
	}
	
	
	public static   void writeCommand(haxe.io.Output output, debugger.Command command)
	{
		debugger.HaxeProtocol.writeDynamic(output, command);
	}
	
	
	public static   void writeMessage(haxe.io.Output output, debugger.Message message)
	{
		debugger.HaxeProtocol.writeDynamic(output, message);
	}
	
	
	public static   debugger.Command readCommand(haxe.io.Input input)
	{
		java.lang.Object raw = debugger.HaxeProtocol.readDynamic(input);
		try 
		{
			return ((debugger.Command) (raw) );
		}
		catch (java.lang.Throwable __temp_catchallException90)
		{
			java.lang.Object __temp_catchall91 = __temp_catchallException90;
			if (( __temp_catchall91 instanceof haxe.lang.HaxeException )) 
			{
				__temp_catchall91 = ((haxe.lang.HaxeException) (__temp_catchallException90) ).obj;
			}
			
			{
				java.lang.Object e = __temp_catchall91;
				throw haxe.lang.HaxeException.wrap(( ( ( "Expected Command, but got " + haxe.root.Std.string(raw) ) + ": " ) + haxe.root.Std.string(e) ));
			}
			
		}
		
		
	}
	
	
	public static   debugger.Message readMessage(haxe.io.Input input)
	{
		java.lang.Object raw = debugger.HaxeProtocol.readDynamic(input);
		try 
		{
			return ((debugger.Message) (raw) );
		}
		catch (java.lang.Throwable __temp_catchallException92)
		{
			java.lang.Object __temp_catchall93 = __temp_catchallException92;
			if (( __temp_catchall93 instanceof haxe.lang.HaxeException )) 
			{
				__temp_catchall93 = ((haxe.lang.HaxeException) (__temp_catchallException92) ).obj;
			}
			
			{
				java.lang.Object e = __temp_catchall93;
				throw haxe.lang.HaxeException.wrap(( ( ( "Expected Message, but got " + haxe.root.Std.string(raw) ) + ": " ) + haxe.root.Std.string(e) ));
			}
			
		}
		
		
	}
	
	
	public static   void writeDynamic(haxe.io.Output output, java.lang.Object value)
	{
		java.lang.String string = haxe.Serializer.run(value);
		int msg_len = string.length();
		haxe.io.Bytes msg_len_raw = haxe.io.Bytes.alloc(8);
		{
			int _g = 0;
			while (( _g < 8 ))
			{
				int i = _g++;
				msg_len_raw.b[( 7 - i )] = ((byte) (( ( msg_len % 10 ) + 48 )) );
				msg_len = ( msg_len / 10 );
			}
			
		}
		
		output.write(msg_len_raw);
		output.writeString(string);
	}
	
	
	public static   java.lang.Object readDynamic(haxe.io.Input input)
	{
		haxe.io.Bytes msg_len_raw = input.read(8);
		int msg_len = 0;
		{
			int _g = 0;
			while (( _g < 8 ))
			{
				int i = _g++;
				msg_len *= 10;
				msg_len += ( (( msg_len_raw.b[i] & 255 )) - 48 );
			}
			
		}
		
		if (( msg_len > 2097152 )) 
		{
			throw haxe.lang.HaxeException.wrap(( ( "Read bad message length: " + msg_len ) + "." ));
		}
		
		return haxe.Unserializer.run(input.read(msg_len).toString());
	}
	
	
	public static  java.lang.String gClientIdentification;
	
	public static  java.lang.String gServerIdentification;
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new debugger.HaxeProtocol(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new debugger.HaxeProtocol();
	}
	
	
}


