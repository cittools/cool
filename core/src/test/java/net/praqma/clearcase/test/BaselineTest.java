package net.praqma.clearcase.test;

import static org.junit.Assert.*;

import java.io.File;

import net.praqma.clearcase.Environment;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Baseline.LabelBehaviour;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.clearcase.util.ExceptionUtils;

import org.junit.ClassRule;
import org.junit.Test;

@ClearCaseTest( name = "" )
public class BaselineTest {

	@ClassRule
	public static ClearCaseRule ccenv = new ClearCaseRule( "clearcaserule", "cool" + Environment.getUniqueTimestamp() );

	public final String vn = "cc-" + Environment.getUniqueTimestamp();
	
	@Test
	public void testLoadAndPromotionLevel() throws Exception {
		Baseline bl = ccenv.context.baselines.get( "model-3" ).load();
		
		assertNotNull( bl );
		assertEquals( PromotionLevel.INITIAL, bl.getPromotionLevel( false ) );
	}
	
	@Test
	public void testCreateBaseline() throws Exception {
		String viewtag = ccenv.getVobName() + "_one_int";
		System.out.println( "VIEW: " + ccenv.context.views.get( viewtag ) );
		//File path = new File( context.views.get( viewtag ).getPath() );
		File path = new File( ccenv.context.mvfs + "/" + ccenv.getVobName() + "_one_int/" + ccenv.getVobName() );
		
		System.out.println( "PATH: " + path );
		
		try {
			ccenv.addNewContent( ccenv.context.components.get( "Model" ), path, "test.txt" );
		} catch( ClearCaseException e ) {
			ExceptionUtils.print( e, System.out, true );
		}
		
		Baseline.create( "new-baseline", ccenv.context.components.get( "_System" ), path, LabelBehaviour.FULL, false );
	}
	
	@Test
	public void testPromote() throws Exception {
		Baseline bl = ccenv.context.baselines.get( "model-3" ).load();
		
		assertNotNull( bl );
		assertEquals( PromotionLevel.INITIAL, bl.getPromotionLevel( false ) );
		bl.promote();
		assertEquals( PromotionLevel.BUILT, bl.getPromotionLevel( false ) );
	}
	
	@Test
	public void testDemote() throws Exception {
		Baseline bl = ccenv.context.baselines.get( "model-2" ).load();
		
		assertNotNull( bl );
		assertEquals( PromotionLevel.INITIAL, bl.getPromotionLevel( false ) );
		bl.demote();
		assertEquals( PromotionLevel.REJECTED, bl.getPromotionLevel( false ) );
	}
	
	@Test
	public void testSetPromotionLevel() throws Exception {
		Baseline bl = ccenv.context.baselines.get( "model-1" ).load();
		
		assertNotNull( bl );
		assertEquals( PromotionLevel.INITIAL, bl.getPromotionLevel( false ) );
		bl.setPromotionLevel( PromotionLevel.RELEASED );
		assertEquals( PromotionLevel.RELEASED, bl.getPromotionLevel( false ) );
	}
	
	@Test
	public void testGetStream() throws Exception {
		Baseline bl = ccenv.context.baselines.get( "model-1" ).load();
		
		assertEquals( ccenv.context.integrationStreams.get( "one_int" ), bl.getStream() );
	}
	
	@Test
	public void testGetComponent() throws Exception {
		Baseline bl = ccenv.context.baselines.get( "client-1" ).load();
		
		assertEquals( ccenv.context.components.get( "Clientapp" ), bl.getComponent() );
	}
	
	@Test
	public void testGet() throws Exception {
		Baseline bl = Baseline.get( "_System_1.0@" + ccenv.getPVob() );
		
		assertNotNull( bl );
	}
	
	@Test
	public void testGetPvob() throws Exception {
		Baseline bl = Baseline.get( "_System_1.0", ccenv.getPVob() );
		
		assertNotNull( bl );
	}
	
	
}
