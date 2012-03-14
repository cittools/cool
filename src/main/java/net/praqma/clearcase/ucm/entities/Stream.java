package net.praqma.clearcase.ucm.entities;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.praqma.clearcase.PVob;
import net.praqma.clearcase.cleartool.Cleartool;
import net.praqma.clearcase.exceptions.CleartoolException;
import net.praqma.clearcase.exceptions.NoSingleTopComponentException;
import net.praqma.clearcase.exceptions.UCMEntityNotFoundException;
import net.praqma.clearcase.exceptions.UCMException;
import net.praqma.clearcase.exceptions.UnableToCreateEntityException;
import net.praqma.clearcase.exceptions.UnableToListBaselinesException;
import net.praqma.clearcase.exceptions.UnableToListProjectsException;
import net.praqma.clearcase.exceptions.UnableToLoadEntityException;
import net.praqma.clearcase.interfaces.Diffable;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.clearcase.ucm.utils.Baselines;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.util.debug.Logger;
import net.praqma.util.execute.AbnormalProcessTerminationException;
import net.praqma.util.execute.CmdResult;

/**
 * This is the OO implementation of the ClearCase entity Stream
 * 
 * @author wolfgang
 * 
 */
public class Stream extends UCMEntity implements Diffable, Serializable {

	private static final String rx_stream_load = "\\s*Error: stream not found\\s*";

	transient static private Logger logger = Logger.getLogger();

	/* Stream specific fields */
	private ArrayList<Baseline> recommendedBaselines = null;
	private Project project = null;
	private Stream defaultTarget = null;
	private boolean readOnly = true;
	private Baseline foundation;

	private Stream parent;

	public Stream() {
		super( "stream" );
	}

	/**
	 * This method is only available to the package, because only UCMEntity
	 * should be allowed to call it.
	 * 
	 * @return A new Stream Entity
	 */
	static Stream getEntity() {
		return new Stream();
	}

	/**
	 * Create a new stream, given a parent Stream, a fully qualified name for
	 * the new Stream and whether the Stream is read only or not
	 * 
	 * @param parent
	 *            The parent Stream
	 * @param nstream
	 *            The fully qualified name of the new Stream
	 * @param readonly
	 *            Whether the new Stream is read only or not
	 * @return A new Stream given the parameters
	 * @throws UnableToCreateEntityException
	 */
	public static Stream create( Stream parent, String nstream, boolean readonly, Baseline baseline ) throws UnableToCreateEntityException {
		//UCMEntity.getNamePart( nstream );

		logger.debug( "PSTREAM:" + parent.getShortname() + ". NSTREAM: " + nstream + ". BASELINE: " + baseline.getShortname() );

		//Stream stream = context.createStream( parent, nstream, readonly, baseline );
		//strategy.createStream( pstream.getFullyQualifiedName(), nstream, readonly, ( baseline != null ? baseline.getFullyQualifiedName() : null ) );

		logger.debug( "Creating stream " + nstream + " as child of " + parent );

		String cmd = "mkstream -in " + parent + " " + ( baseline != null ? "-baseline " + baseline + " " : "" ) + ( readonly ? "-readonly " : "" ) + nstream;
		try {
			Cleartool.run( cmd );
		} catch( Exception e ) {
			//throw new UCMException( "Could not create stream: " + e.getMessage() );
			throw new UnableToCreateEntityException( Stream.class, e );
		}

		Stream stream = Stream.get( nstream );

		stream.setCreated( true );

		if( parent != null ) {
			stream.setParent( parent );
		}

		return stream;
	}

	public static Stream createIntegration( String name, Project project, Baseline baseline ) throws UnableToCreateEntityException {
		//context.createIntegrationStream( name, project, baseline );

		String cmd = "mkstream -integration -in " + project.getFullyQualifiedName() + " -baseline " + baseline.getFullyQualifiedName() + " " + name + "@" + project.getPVob();

		try {
			Cleartool.run( cmd );
		} catch( AbnormalProcessTerminationException e ) {
			//throw new UCMException( "Could not create integration stream: " + e.getMessage(), UCMType.CREATION_FAILED );
			throw new UnableToCreateEntityException( Stream.class, e );
		}

		return Stream.get( name, project.getPVob(), true );
	}

	public UCMEntity load() throws UCMEntityNotFoundException, UnableToLoadEntityException {
		logger.debug( "loading stream" );
		//context.loadStream( this );

		List<String> data = null;

		String cmd = "describe -fmt %[name]p\\n%[project]Xp\\n%X[def_deliver_tgt]p\\n%[read_only]p\\n%[found_bls]Xp " + this;
		try {
			data = Cleartool.run( cmd ).stdoutList;
		} catch( AbnormalProcessTerminationException e ) {
			if( e.getMessage().matches( rx_stream_load ) ) {
				//throw new UCMException( "The component \"" + this + "\", does not exist.", UCMType.LOAD_FAILED );
				throw new UCMEntityNotFoundException( this, e );
			} else {
				//throw new UCMException( e.getMessage(), e.getMessage(), UCMType.LOAD_FAILED );
				throw new UnableToLoadEntityException( this, e );
			}
		}

		logger.debug( "I got: " + data );

		/* Set project */
		setProject( Project.get( data.get( 1 ) ) );

		/* Set default target, if exists */
		if( !data.get( 2 ).trim().equals( "" ) ) {
			try {
				setDefaultTarget( Stream.get( data.get( 2 ) ) );
			} catch( Exception e ) {
				logger.debug( "The Stream did not have a default target." );
			}
		}

		/* Set read only */
		if( data.get( 3 ).length() > 0 ) {
			setReadOnly( true );
		} else {
			setReadOnly( false );
		}

		/* Set foundation baseline */
		try {
			setFoundationBaseline( Baseline.get( data.get( 4 ) ) );
		} catch( Exception e ) {
			logger.warning( "Could not get the foundation baseline: " + e.getMessage() );
		}

		this.loaded = true;

		return this;
	}

	public List<Baseline> getBaselines( PromotionLevel plevel ) throws UnableToListBaselinesException, NoSingleTopComponentException, UnableToLoadEntityException {
		//return context.getBaselines( this, getSingleTopComponent(), plevel, pvob );
		return Baselines.get( this, getSingleTopComponent(), plevel, pvob );
	}

	public List<Baseline> getBaselines( Component component, PromotionLevel plevel ) throws UnableToListBaselinesException {
		//return context.getBaselines( this, component, plevel, pvob );
		return Baselines.get( this, component, plevel, pvob );
	}

	public List<Baseline> getBaselines( Component component, PromotionLevel plevel, Date date ) throws UnableToListBaselinesException {
		List<Baseline> baselines = Baselines.get( this, component, plevel, pvob );

		if( date == null ) {
			return baselines;
		}

		Iterator<Baseline> it = baselines.iterator();
		while( it.hasNext() ) {
			Baseline baseline = it.next();

			if( date.after( baseline.getDate() ) ) {
				logger.debug( "Removing [" + baseline.getShortname() + " " + baseline.getDate() + "/" + date + "]" );
				it.remove();
			}
		}

		return baselines;
	}

	public List<Stream> getChildStreams() {

		List<Stream> streams = new ArrayList<Stream>();
		try {
			CmdResult res = null;

			String cmd = "desc -fmt %[dstreams]CXp " + this;
			try {
				res = Cleartool.run( cmd );
			} catch( AbnormalProcessTerminationException e ) {
				throw new UCMEntityNotFoundException( this, e );
			}

			String[] strms = res.stdoutBuffer.toString().split( ", " );
			for( String stream : strms ) {
				streams.add( Stream.get( stream ) );
			}

		} catch( UCMEntityNotFoundException e ) {
			logger.debug( "The Stream has no child streams" );
		}

		return streams;
	}

	public void setProject( Project project ) {
		this.project = project;
	}

	public void setDefaultTarget( Stream stream ) {
		this.defaultTarget = stream;
	}

	/**
	 * For each project return their integration streams
	 * 
	 * @return
	 * @throws UnableToListProjectsException 
	 * @throws UCMException
	 */
	public List<Stream> getSiblingStreams() throws UnableToListProjectsException {
		logger.debug( "Getting sibling streams" );
		List<Project> projects = Project.getProjects( this.getPVob() );
		List<Stream> streams = new ArrayList<Stream>();

		logger.debug( projects );

		for( Project p : projects ) {
			try {
				if( p.getIntegrationStream().getDefaultTarget() != null && this.equals( p.getIntegrationStream().getDefaultTarget() ) ) {
					streams.add( p.getIntegrationStream() );
				}
			} catch( Exception e ) {
				/* Just move on! */
			}
		}

		logger.debug( streams );

		return streams;
	}

	public boolean equals( Stream other ) {
		if( this.fqname.equals( other.getFullyQualifiedName() ) ) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Determines whether a Stream exists, given a fully qualified name
	 * 
	 * @param fqname
	 *            Fully qualified name
	 * @return True if the Stream exists, false otherwise
	 * @throws UCMException
	 *             Is thrown if the fully qualified name is not a valid name
	 */
	public static boolean streamExists( String fqname ) {
		String cmd = "describe " + fqname;
		try {
			Cleartool.run( cmd );
			return true;
		} catch( Exception e ) {
			return false;
		}
	}

	public boolean exists() {
		return streamExists( this.fqname );
	}



	public List<Baseline> getRecommendedBaselines() throws UnableToListBaselinesException, NoSingleTopComponentException, UnableToLoadEntityException {
		return getRecommendedBaselines( false );
	}

	public void generate() throws CleartoolException {
		String cmd = "chstream -generate " + this;
		try {
			Cleartool.run( cmd );
		} catch( AbnormalProcessTerminationException e ) {
			throw new CleartoolException( e );
		}
	}

	public ArrayList<Baseline> getRecommendedBaselines( boolean force ) throws UnableToListBaselinesException, NoSingleTopComponentException, UnableToLoadEntityException {
		logger.debug( "Getting recommended baselines" );

		if( this.recommendedBaselines == null || force ) {
			//this.recommendedBaselines = context.getRecommendedBaselines( this );
			ArrayList<Baseline> bls = new ArrayList<Baseline>();

			//String result = strategy.getRecommendedBaselines( stream.getFullyQualifiedName() );
			String result = "";
			String cmd = "desc -fmt %[rec_bls]p " + this;
			try {
				result = Cleartool.run( cmd ).stdoutBuffer.toString();
			} catch( AbnormalProcessTerminationException e ) {
				//throw new UCMException( "Unable to get recommended baselines from " + stream + ": " + e.getMessage() );
				throw new UnableToListBaselinesException( this, getSingleTopComponent(), null, e );
			}
			
			String[] rs = result.split( " " );

			for( int i = 0; i < rs.length; i++ ) {
				/* There is something in the element. */
				if( rs[i].matches( "\\S+" ) ) {
					// bls.add( (Baseline)UCMEntity.GetEntity( rs[i], true ) );
					bls.add( Baseline.get( rs[i] + "@" + pvob, true ) );
				}
			}

			return bls;
		}

		return this.recommendedBaselines;
	}

	public void recommendBaseline( Baseline baseline ) throws CleartoolException {
		String cmd = "chstream -recommend " + baseline + " " + this;
		try {
			Cleartool.run( cmd );
		} catch( AbnormalProcessTerminationException e ) {
			//throw new UCMException( "Could not recommend Baseline: " + e.getMessage(), e.getMessage() );
			throw new CleartoolException( "Unable to recommend " + baseline, e );
		}
	}

	public List<Baseline> getLatestBaselines() throws CleartoolException {
		//return context.getLatestBaselines( this );
		
		//List<String> bs = strategy.getLatestBaselines( stream.getFullyQualifiedName() );
		String cmd = "desc -fmt %[latest_bls]Xp " + this;
		try {
			String[] t = Cleartool.run( cmd ).stdoutBuffer.toString().split( " " );
			List<Baseline> bls = new ArrayList<Baseline>();
			for( String s : t ) {
				if( s.matches( "\\S+" ) ) {
					bls.add( Baseline.get( s.trim() ) );
				}
			}

			return bls;
		} catch( AbnormalProcessTerminationException e ) {
			//throw new UCMException( "Unable to get latest baseline from " + stream + ": " + e.getMessage() );
			throw new CleartoolException( "Unable to get latest baselines from " + this, e );
		}
	}

	public Component getSingleTopComponent() throws NoSingleTopComponentException, UnableToLoadEntityException {
		List<Baseline> bls;
		try {
			bls = this.getRecommendedBaselines();
		} catch( UnableToListBaselinesException e ) {
			throw new NoSingleTopComponentException( this );
		}

		if( bls.size() != 1 ) {
			//throw new Cleartool( "The Stream " + this.getShortname() + " does not have a single composite component." );
			throw new NoSingleTopComponentException( this );
		}

		return bls.get( 0 ).getComponent();
	}

	public Project getProject() throws UCMEntityNotFoundException, UnableToLoadEntityException {
		if( !this.loaded ) load();

		return this.project;
	}

	/**
	 * This method returns the default Stream the given Stream will deliver to.
	 * 
	 * @return A Stream
	 * @throws UnableToLoadEntityException 
	 * @throws UCMEntityNotFoundException 
	 * @throws UCMException
	 */
	public Stream getDefaultTarget() throws UCMEntityNotFoundException, UnableToLoadEntityException {
		if( !this.loaded ) {
			load();
		}
		return this.defaultTarget;
	}


	public void setReadOnly( boolean readOnly ) {
		this.readOnly = readOnly;
	}

	public boolean isReadOnly() throws UCMEntityNotFoundException, UnableToLoadEntityException {
		if( !this.loaded ) load();
		return readOnly;
	}

	public void setFoundationBaseline( Baseline baseline ) {
		this.foundation = baseline;
	}

	public Baseline getFoundationBaseline() throws UCMEntityNotFoundException, UnableToLoadEntityException {
		if( !this.loaded ) load();

		return this.foundation;
	}

	public void setParent( Stream parent ) {
		this.parent = parent;
	}

	public Stream getParent() {
		return parent;
	}

	public String stringify() {
		StringBuffer sb = new StringBuffer();
		try {
			if( !this.loaded ) load();

			if( this.recommendedBaselines != null ) {
				sb.append( "Recommended baselines: " + this.recommendedBaselines.size() + linesep );
				for( Baseline b : this.recommendedBaselines ) {
					sb.append( "\t" + b.toString() + linesep );
				}
			} else {
				sb.append( "Recommended baselines: Undefined/not loaded" + linesep );
			}
		} catch( Exception e ) {

		} finally {
			//sb.append( super.stringify() );
			sb.insert( 0, super.stringify() );
		}

		return sb.toString();
	}

	public static Stream get( String name ) {
		return get( name, true );
	}

	public static Stream get( String name, boolean trusted ) {
		if( !name.startsWith( "stream:" ) ) {
			name = "stream:" + name;
		}
		Stream entity = (Stream) UCMEntity.getEntity( Stream.class, name, trusted );
		return entity;
	}

	public static Stream get( String name, PVob pvob, boolean trusted ) {
		if( !name.startsWith( "stream:" ) ) {
			name = "stream:" + name;
		}
		Stream entity = (Stream) UCMEntity.getEntity( Stream.class, name + "@" + pvob, trusted );
		return entity;
	}

}
