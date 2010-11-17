package test;

import java.util.ArrayList;

import net.praqma.clearcase.ucm.entities.*;
import net.praqma.clearcase.ucm.entities.Baseline.BaselineDiff;
import net.praqma.clearcase.ucm.entities.Component.BaselineList;
import net.praqma.clearcase.ucm.entities.UCMEntity.Plevel;
import net.praqma.clearcase.ucm.utils.TagQuery;
import net.praqma.utils.Debug;

public class Main
{
	private static Debug logger = Debug.GetLogger( false );
	
	/**
	 * @param args
	 */
	public static void main( String[] args )
	{
		Stream st1 = UCMEntity.GetStream( "stream:STREAM_TEST1@\\PDS_PVOB" );
		Component co1 = UCMEntity.GetComponent( "component:COMPONENT_TEST1@\\PDS_PVOB" );
		BaselineList bls = co1.GetBaselines( st1, Plevel.INITIAL );
		
		System.out.println( "SIZE=" + bls.size() );
		
		Baseline bl1 = bls.get( 0 );
		
		Tag tag = bl1.CreateTag( "hudson", "snade", "now", "building" );
		tag.SetEntry( "buildnum", "1" );
		tag = tag.Persist();
		System.out.println( tag.Stringify() );
		
		tag.SetEntry( "buildstatus", "nogetnyt" );
		tag = tag.Persist();
		System.out.println( tag.Stringify() );
		
		bl1.SaveState();

	}
	
	public static void test5()
	{
		Stream st1 = UCMEntity.GetStream( "stream:STREAM_TEST1@\\PDS_PVOB" );
		Component co1 = UCMEntity.GetComponent( "component:COMPONENT_TEST1@\\PDS_PVOB" );
		BaselineList bls = co1.GetBaselines( st1, Plevel.INITIAL );
		
		
		/* Preprocess */
		for( Baseline bl : bls )
		{
			Tag t = bl.CreateTag( "hudson", "snade", "2010", "inprogres1s" );
		}
		
		
		TagQuery tq = new TagQuery();
		tq.AddCondition( "buildstatus", "^(?!inprogress$)" );
		
		BaselineList bls2 = bls.Filter( tq, "hudson", "snade" );
		
		System.out.println( "BLS2=" );
		for( Baseline bl : bls2 )
		{
			System.out.println( bl.toString() );
		}
		
		BaselineList bls3 = bls2.NewerThanRecommended();
		
		System.out.println( "BLS3=" );
		for( Baseline bl : bls3 )
		{
			System.out.println( bl.toString() );
			BaselineDiff diffs = bl.GetDiffs();
			System.out.println( diffs.get( 0 ).changeset.versions.get( 0 ).Stringify() );
		}
	}
	
	public static void test4()
	{
		Stream st1 = UCMEntity.GetStream( "stream:STREAM_TEST1@\\PDS_PVOB" );
		Component co1 = UCMEntity.GetComponent( "component:COMPONENT_TEST1@\\PDS_PVOB" );
		BaselineList bls = co1.GetBaselines( st1 );
		
		
		for( Baseline bl : bls )
		{
			//Tag t = bl.CreateTag( "hudson", "007", "2010", "inprogress" );
			
		}
		
		Baseline b = bls.get( 1 );
		Tag t = b.GetTag( "hudson", "007" );
		t.SetEntry( "buildstatus", "inprogress" );
		//logger.print( t.Stringify() );
		t.Persist();
		
		TagQuery tq = new TagQuery();
		tq.AddCondition( "buildstatus", ".*(?<!inprogress)" );
		
		BaselineList bls2 = bls.Filter( tq, "hudson", "007" );
		
		
		for( Baseline bl : bls2 )
		{
			System.out.println( bl.toString() );
		}
		
		t.SaveState();
	}
	
	public static void test2()
	{
		Stream st1 = UCMEntity.GetStream( "stream:STREAM_TEST1@\\PDS_PVOB" );
		ArrayList<Baseline> bls = st1.GetRecommendedBaselines();
		
		if( bls.size() != 1 )
		{
			logger.warning( "Recommended baselines are not the correct size" );
			return;
		}
		
		Baseline bl1 = bls.get( 0 );
		Tag t1 = bl1.CreateTag( "hudson", "123", "2010 11 10", "aborted" );
		
		System.out.println( t1.Stringify() );
		
		logger.debug( bl1.GetXML() );

	}
	
	public static void test1()
	{
		//Stream st1 = (Stream)UCMEntity.GetEntity( "stream:STREAM_TEST1@\\PDS_PVOB" );
		Stream st1 = UCMEntity.GetStream( "stream:STREAM_TEST1@\\PDS_PVOB" );
		ArrayList<Baseline> bls = st1.GetRecommendedBaselines();
		
		if( bls.size() != 1 )
		{
			logger.warning( "Recommended baselines are not the correct size" );
			//Baseline bl1 = (Baseline)UCMEntity.GetEntity( "baseline:BASELINE_TEST1@\\PDS_PVOB" );
			Baseline bl1 = UCMEntity.GetBaseline( "baseline:BASELINE_TEST1@\\PDS_PVOB" );
			st1.RecommendBaseline( bl1 );
		}
		
		bls = st1.GetRecommendedBaselines( true );
		
		Baseline bl1 = bls.remove( 0 );
		
		System.out.println( bl1.Stringify() );
		bl1.Promote();
		System.out.println( bl1.Stringify() );
		bl1.Promote();
		System.out.println( bl1.Stringify() );
		bl1.Promote();
		System.out.println( bl1.Stringify() );
		bl1.Promote();
		System.out.println( bl1.Stringify() );
		
		Tag t = bl1.GetTag( "hudson", "001" );
		System.out.println( t.Stringify() );
		
		t.SetEntry( "status", "pending" );
		
		t.Persist();
		
		Tag t2 = bl1.GetTag( "hudson", "001" );
		System.out.println( t2.Stringify() );
		
		t2.SetEntry( "status", "building" );
		
		t2.Persist();
		
		Tag t3 = bl1.GetTag( "hudson", "001" );
		System.out.println( t3.Stringify() );
		
		t3.SetEntry( "status", "failed" );
		
		t3.Persist();
		
		Tag t4 = bl1.GetTag( "hudson", "001" );
		System.out.println( t4.Stringify() );

		
		logger.debug( bl1.GetXML() );
	}
	
}