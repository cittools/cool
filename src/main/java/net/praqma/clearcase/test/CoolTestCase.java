package net.praqma.clearcase.test;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;

import net.praqma.clearcase.Cool;
import net.praqma.clearcase.PVob;
import net.praqma.clearcase.annotations.TestConfiguration;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.exceptions.CleartoolException;
import net.praqma.clearcase.exceptions.UCMEntityNotFoundException;
import net.praqma.clearcase.exceptions.UnableToCreateEntityException;
import net.praqma.clearcase.exceptions.UnableToListProjectsException;
import net.praqma.clearcase.exceptions.UnableToLoadEntityException;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.view.DynamicView;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.Logger.LogLevel;
import net.praqma.util.debug.appenders.Appender;
import net.praqma.util.debug.appenders.ConsoleAppender;

import junit.framework.TestCase;

public abstract class CoolTestCase extends TestCase {
	
	protected static Logger logger = Logger.getLogger();
	protected static ConsoleAppender appender = new ConsoleAppender();
	
	protected static boolean rolling = true;
	
	static {
		System.out.println( "STATIC" );
		appender.setTemplate( "[%level]%space %message%newline" );
		appender.setMinimumLevel(LogLevel.DEBUG);
		Logger.addAppender(appender);
	}
	
	protected PVob pvob;
	protected boolean removePvob = false;
	protected boolean fail = false;
	
	protected String dynamicView = "TestDynamicView";
	private DynamicView baseView;
	
	protected void createComponents( int rooted, int rootless ) {
		
	}

	@Override
	protected void setUp() {
		logger.debug( "Setup" );
		
		TestConfiguration config = getClass().getAnnotation( TestConfiguration.class );
		String project = config.project();
		String pvob = Cool.filesep + config.pvob();
		
		removePvob = false;
		PVob pv = PVob.get( pvob );
		if( pv == null ) {
			logger.info("Creating " + pvob);
			try {
				logger.verbose( "Creating pvob " + pvob );
				this.pvob = (PVob) PVob.create( pvob, true, null, "testing" );
				logger.verbose( "Creating dynamic view" );
				baseView = DynamicView.create( null, dynamicView, null );
				logger.verbose( "Starting view" );
				new DynamicView( null, dynamicView ).startView();
				removePvob = true;
			} catch( ClearCaseException e ) {
				e.print( System.err );
				fail = true;
				
			}
		} else {
			logger.fatal("The PVob " + pvob + " already exists" );
			fail = true;
		}
	}
	
	@Override
    protected void runTest() throws Throwable {
    	if( !fail ) {
    		super.runTest();
    	} else {
    		logger.fatal( "ClearCase not set up, unable to run test" );
    	}
    }
    

    @Override
	public void runBare() throws Throwable {
    	System.out.println( "runBare!" );
        Thread t = Thread.currentThread();
        String o = getClass().getName()+'.'+t.getName();
        t.setName("Executing "+getName());
        try {
            super.runBare();
        } finally {
            t.setName(o);
        }
    }
    
    @Override
    protected void tearDown() {
    	logger.info( "Tear down ClearCase" );
    	
    	if( removePvob ) {
    		try {
	    		/* Removing baseview */
	    		logger.verbose( "Removing base view" );
	    		baseView.remove();
	    		
	    		logger.info("Getting projects");
	    		try {
					Project.getProjects(pvob);
				} catch (ClearCaseException e1) {
					e1.print(appender.getOut());
					return;
				}
	    		
	    		logger.info("Removing PVob " + pvob);
	    	
				pvob.remove();
			} catch( ClearCaseException e ) {
				logger.fatal( "Unable to tear down ClearCase" );
				e.print( System.err );
			}
    	}
    }
}
