package net.uvavru.maven.plugins.jettyconf;

import net.uvavru.maven.plugins.jettyconf.internals.AbstractJettyConfMojo;
import net.uvavru.maven.plugins.jettyconf.types.ArtifactCandidates;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal generate-properties
 * @requiresDependencyResolution test
 * @description Creates properties <ul><li>{@code jetty.conf-plugin.classpath}</li> and <li>{@code jetty.conf-plugin.webapp}</li></ul>
 */
public class JettyConfPropertiesMojo extends AbstractJettyConfMojo {

	public void execute() throws MojoExecutionException {

		ArtifactCandidates artifactCandidates = jettyArtifactCandidates();
		
		initializeJettyConfProperties(artifactCandidates);

	}

}
