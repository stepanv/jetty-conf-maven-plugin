package net.uvavru.maven.plugins.jettyconf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import net.uvavru.maven.plugins.jettyconf.internals.AbstractJettyConfMojo;
import net.uvavru.maven.plugins.jettyconf.internals.JettyConfWriter;
import net.uvavru.maven.plugins.jettyconf.types.ArtifactCandidates;
import net.uvavru.maven.plugins.jettyconf.types.JettyFiles;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;

/**
 * @goal copy-from-template
 * @requiresDependencyResolution test
 * @execute phase="compile"
 * @description Copies files from the template directory to the
 */
public class JettyConfCopyMojo extends AbstractJettyConfMojo {

	/**
	 * Original location of Jetty context.xml style file which will be used as a
	 * template.
	 * 
	 * @since 1.0
	 * @parameter expression="${contextXmlTemplate}"
	 * @required
	 */
	private File contextXmlTemplate;

	public File getContextXmlTemplate() {
		return contextXmlTemplate;
	}

	/**
	 * Original location of Jetty context.xml style file which will be used as a
	 * template.
	 * 
	 * @since 1.0
	 * @parameter expression="${confTargetDir}"
	 *            default-value="${project.build.directory}/jetty-context"
	 */
	private String confTargetDir;

	public void setContextXmlTemplate(File contextXmlTemplate) {
		this.contextXmlTemplate = contextXmlTemplate;
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		ArtifactCandidates artifactCandidates = jettyArtifactCandidates();

		JettyFiles classpathFiles = filterAndTranslateClasspathArtifacts(artifactCandidates);
		JettyFiles webappFiles = filterAndTranslateWebAppArtifacts(artifactCandidates);

		// Write new configuration file to an output stream
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			JettyConfWriter writer = new JettyConfWriter(classpathFiles, webappFiles);
			
			writer.writeToStream(outputStream, contextXmlTemplate);

			File configurationFile = new File(confTargetDir + System.getProperty("file.separator")
					+ contextXmlTemplate.getName());

			if (!configurationFile.exists()) {
				configurationFile.createNewFile();
			}
			FileInputStream inputStreamOld = new FileInputStream(
					configurationFile);
			byte[] newConfigArray = outputStream.toByteArray();
			ByteArrayInputStream inputStreamNew = new ByteArrayInputStream(
					newConfigArray);
			if (!IOUtil.contentEquals(inputStreamOld, inputStreamNew)) {
				OutputStream confFileOutputStream = new FileOutputStream(
						configurationFile);
				IOUtil.copy(newConfigArray, confFileOutputStream);
				confFileOutputStream.flush();
				confFileOutputStream.close();
				
				getLog().info("New context xml file written at: " + configurationFile);
			} else {
				getLog().info("Preserving old context xml file at: " + configurationFile);
			}
			outputStream.close();
			inputStreamNew.close();
			inputStreamOld.close();
		} catch (Exception e) {
			throw new MojoExecutionException("Cannot generate Jetty configuration: " + e.getMessage(), e);
		}

	}
	
	

}
