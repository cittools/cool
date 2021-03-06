package net.praqma.clearcase.util;

import java.io.PrintStream;

import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.util.debug.Logger;

public abstract class ExceptionUtils {
	
	private static Logger logger = Logger.getLogger();

	public static void print( Throwable e, PrintStream out, boolean stack ) {
		
		if( e instanceof ClearCaseException ) {
			((ClearCaseException)e).print( out );
		}
		
		if( e.getCause() != null ) {
			out.println( e.getMessage() );
			print( e.getCause(), out, stack );
		} else {
			if( stack ) { 
				e.printStackTrace( out );
			} else {
				out.println( e.getMessage() );
			}
		}
	}
	
	public static void log( Throwable e, boolean stack ) {
		
		if( e instanceof ClearCaseException ) {
			((ClearCaseException)e).log();
		}
		
		if( e.getCause() != null ) {
			logger.warning( e.getMessage() );
			log( e.getCause(), stack );
		} else {
			if( stack ) { 
				logger.warning( e );
			} else {
				logger.warning( e.getMessage() );
			}
		}
	}
}
