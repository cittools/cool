package net.praqma.jenkins.utils.test;

import hudson.model.AbstractBuild;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.praqma.clearcase.Environment;
import net.praqma.util.debug.Logger;

import org.apache.commons.io.FileUtils;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.JenkinsRule;

public class ClearCaseJenkinsTestCase extends JenkinsRule {
	
	private static Logger logger = Logger.getLogger();
	
	public Environment coolTest = new ConcreteCoolTestCase();
	
	public Environment getCoolTestCase() {
		return coolTest;
	}

	public List<String> getLog( AbstractBuild<?, ?> build ) throws IOException {
		List<String> log = new ArrayList<String>();
		BufferedReader br = new BufferedReader( new FileReader( build.getLogFile() ) );
		String line = "";
		while( ( line = br.readLine() ) != null ) {
			log.add( line );
		}

		br.close();
		
		return log;
	}
	
	@Override
	public void after() {
		try {
			//logger.info( "Skipping teardown" );
			super.after();
		} catch( Exception e ) {
			System.out.println( "PATH: " + super.jenkins.getRootDir() );
			//FileUtils.deleteDirectory( super.jenkins.getRootDir() );
            try {
                FileUtils.forceDelete(super.jenkins.getRootDir());
            } catch (IOException ioex) {
                System.out.println("Failed to forcibly remove temp directory!");
            }
		}
	}
}
