package net.uvavru.maven.plugins.jettyconf;

import java.io.File;

import net.uvavru.maven.plugins.jettyconf.internals.AbstractJettyConfMojo;
import net.uvavru.maven.plugins.jettyconf.types.ArtifactCandidates;
import net.uvavru.maven.plugins.jettyconf.types.JettyFiles;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal generate-properties
 * @requiresDependencyResolution test
 * @execute phase="compile"
 * @description Creates properties 'jetty.conf-plugin.classpath' and 'jetty.conf-plugin.webapp'
 */
public class JettyConfPropertiesMojo extends AbstractJettyConfMojo {

	public void execute() throws MojoExecutionException {

		ArtifactCandidates artifactCandidates = jettyArtifactCandidates();
		
		JettyFiles classpathFiles = filterAndTranslateClasspathArtifacts(artifactCandidates);
		
		JettyFiles webappFiles = filterAndTranslateWebAppArtifacts(artifactCandidates);
		
		
		String webapp = "";
		for (File file : webappFiles) {
			webapp += "\n<Item>" + file.toURI() + "</Item>";
		}
		project.getProperties()
				.setProperty("jetty.conf-plugin.webapp", webapp);

		String classpath = "";
		for (File file : classpathFiles) {
			classpath += "\n" + file.toURI() + ";";
		}
		project.getProperties()
				.setProperty("jetty.conf-plugin.classpath", classpath);

		getLog().info(
				"Generated properties 'jetty.conf-plugin.classpath' and 'jetty.conf-plugin.webapp'");

	}

}
