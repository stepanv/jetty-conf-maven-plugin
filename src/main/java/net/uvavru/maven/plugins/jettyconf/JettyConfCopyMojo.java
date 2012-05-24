package net.uvavru.maven.plugins.jettyconf;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import net.uvavru.maven.plugins.jettyconf.internals.AbstractJettyConfMojo;
import net.uvavru.maven.plugins.jettyconf.internals.JettyConfWriter;
import net.uvavru.maven.plugins.jettyconf.types.ArtifactCandidates;
import net.uvavru.maven.plugins.jettyconf.types.JettyFiles;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.codehaus.plexus.util.FileUtils.FilterWrapper;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;

/**
 * @goal copy-from-template
 * @requiresDependencyResolution test
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
		
		InputStream inStream = readTemplateFile(artifactCandidates);
		
		filterAndWriteFileIfChanged(inStream);

	}
	
	private InputStream readTemplateFile(ArtifactCandidates artifactCandidates) throws MojoExecutionException {
		if (parseAsXml) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	
			JettyFiles classpathFiles = filterAndTranslateClasspathArtifacts(artifactCandidates);
			JettyFiles webappFiles = filterAndTranslateWebAppArtifacts(artifactCandidates);
	
			// Write new configuration file to an output stream
			JettyConfWriter writer = new JettyConfWriter(classpathFiles,
					webappFiles);
	
			try {
				writer.writeToStream(outputStream, contextXmlTemplate);
				
				return new ByteArrayInputStream(
						outputStream.toByteArray());
			} catch (Exception e) {
				throw new MojoExecutionException(
						"Cannot generate Jetty configuration: " + e.getMessage(), e);
			}
		} else {
			try {
				if (filtering) {
					// if filtering is not enabled, there is no need to generate these properties
					initializeJettyConfProperties(artifactCandidates);
				}
				
				return new FileInputStream(contextXmlTemplate);
			} catch (Exception e) {
				throw new MojoExecutionException(
						"Cannot read Jetty configuration template: " + e.getMessage(), e);
			}
		}
	}
	
	private void filterAndWriteFileIfChanged(InputStream inStream) throws MojoExecutionException, MojoFailureException {
		// conditionally filter the output
		if (filtering) {
			
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			
			filterInputToOutputStream(inStream, outputStream);
			writeFileIfChanged(outputStream.toByteArray());
		} else {
			try {
				writeFileIfChanged(IOUtil.toByteArray(inStream));
			} catch (IOException e) {
				throw new MojoExecutionException(
						"Cannot write Jetty configuration template: " + e.getMessage(), e);
			}
		}
	}

	private void writeFileIfChanged(byte[] newConfigFileContent) throws MojoExecutionException {
		File configurationFile = new File(confTargetDir
				+ System.getProperty("file.separator")
				+ contextXmlTemplate.getName());

		try {
			if (!configurationFile.exists()) {
				File parentDir = configurationFile.getParentFile();
				if (parentDir != null) {
					parentDir.mkdirs();
				}

				configurationFile.createNewFile();

			}
			FileInputStream inputStreamOld = new FileInputStream(
					configurationFile);
			ByteArrayInputStream inputStreamNew = new ByteArrayInputStream(
					newConfigFileContent);

			if (!IOUtil.contentEquals(inputStreamOld, inputStreamNew)) {
				OutputStream confFileOutputStream = new FileOutputStream(
						configurationFile);
				IOUtil.copy(newConfigFileContent, confFileOutputStream);
				confFileOutputStream.flush();
				confFileOutputStream.close();

				getLog().info(
						"New context xml file written at: " + configurationFile);
			} else {
				getLog().info(
						"Preserving old context xml file at: "
								+ configurationFile);
			}
			inputStreamNew.close();
			inputStreamOld.close();
		} catch (IOException e) {
			throw new MojoExecutionException(
					"Cannot write Jetty configuration: " + e.getMessage(), e);
		}
		
	}

	private void filterInputToOutputStream(InputStream inStream, OutputStream outStream)
			throws MojoExecutionException, MojoFailureException {

		MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(
				null, null, project, encoding, null, Collections.EMPTY_LIST,
				session);

		try {
			createDefaultFilteringReader(mavenResourcesExecution, inStream,
					outStream);
		} catch (IOException e) {
			throw new MojoExecutionException(
					"IO error occured when filtering context xml file: "
							+ e.getMessage(), e);
		} catch (MavenFilteringException e) {
			throw new MojoFailureException(
					"Filtering error: " + e.getMessage(), e);
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void initializeDelimiters(MavenResourcesExecution mavenResourcesExecution) {
		// if these are NOT set, just use the defaults, which are '${*}' and '@'.
        if ( delimiters != null && !delimiters.isEmpty() )
        {
            LinkedHashSet<String> delims = new LinkedHashSet<String>();
            if ( useDefaultDelimiters )
            {
                delims.addAll( mavenResourcesExecution.getDelimiters() );
            }
            
            for ( Iterator dIt = delimiters.iterator(); dIt.hasNext(); )
            {
                String delim = (String) dIt.next();
                if ( delim == null )
                {
                    // FIXME: ${filter:*} could also trigger this condition. Need a better long-term solution.
                    delims.add( "${*}" );
                }
                else
                {
                    delims.add( delim );
                }
            }
            
            mavenResourcesExecution.setDelimiters( delims );
        }
	}

	@SuppressWarnings("unchecked")
	private void createDefaultFilteringReader(
			MavenResourcesExecution mavenResourcesExecution,
			InputStream inStream, OutputStream outStream) throws IOException,
			MavenFilteringException {

		mavenResourcesExecution.setEscapeWindowsPaths( escapeWindowsPaths );
		initializeDelimiters(mavenResourcesExecution);
		
		List<FilterWrapper> filterWrappers = new ArrayList<FilterWrapper>();
		filterWrappers.addAll(mavenFileFilter
				.getDefaultFilterWrappers(mavenResourcesExecution));
		mavenResourcesExecution.setFilterWrappers(filterWrappers);

		if (mavenResourcesExecution.getEncoding() == null
				|| mavenResourcesExecution.getEncoding().length() < 1) {
			getLog().warn(
					"Using platform encoding ("
							+ ReaderFactory.FILE_ENCODING
							+ " actually) to copy filtered resources, i.e. build is platform dependent!");
		} else {
			getLog().info(
					"Using '" + mavenResourcesExecution.getEncoding()
							+ "' encoding to copy filtered resources.");
		}

		filter(mavenResourcesExecution, inStream, outStream);

	}

	@SuppressWarnings("unchecked")
	private void filter(MavenResourcesExecution mavenResourcesExecution,
			InputStream inStream, OutputStream outStream) throws IOException {

		String encoding = mavenResourcesExecution.getEncoding();

		Reader fileReader = null;
		Writer fileWriter = null;
		if (encoding == null || encoding.length() < 1) {
			fileReader = new BufferedReader(new InputStreamReader(inStream));
			fileWriter = new OutputStreamWriter(outStream);
		} else {
			fileReader = new BufferedReader(new InputStreamReader(inStream,
					encoding));

			fileWriter = new OutputStreamWriter(outStream, encoding);
		}

		Reader reader = fileReader;
		for (FilterWrapper wrapper : (List<FilterWrapper>) mavenResourcesExecution
				.getFilterWrappers()) {
			reader = wrapper.getReader(reader);
		}

		IOUtil.copy(reader, fileWriter);

	}

	/**
     * 
     * @component role="org.apache.maven.shared.filtering.MavenFileFilter" role-hint="default"
     * @required
     */
	protected MavenFileFilter mavenFileFilter;

	/**
	 * The character encoding scheme to be applied when filtering resources.
	 * 
	 * @parameter expression="${encoding}"
	 *            default-value="${project.build.sourceEncoding}"
	 */
	protected String encoding;

	/**
	 * @parameter default-value="${session}"
	 * @readonly
	 * @required
	 */
	protected MavenSession session;
	
	/**
     * <p>
     * Set of delimiters for expressions to filter within the resources. These delimiters are specified in the
     * form 'beginToken*endToken'. If no '*' is given, the delimiter is assumed to be the same for start and end.
     * </p><p>
     * So, the default filtering delimiters might be specified as:
     * </p>
     * <pre>
     * &lt;delimiters&gt;
     *   &lt;delimiter&gt;${*}&lt/delimiter&gt;
     *   &lt;delimiter&gt;@&lt/delimiter&gt;
     * &lt;/delimiters&gt;
     * </pre>
     * <p>
     * Since the '@' delimiter is the same on both ends, we don't need to specify '@*@' (though we can).
     * </p>
     * @parameter
     * @since 2.4
     */
    @SuppressWarnings("rawtypes")
	protected List delimiters;
    
    
    /**
     * @parameter default-value="true"
     */
    protected boolean useDefaultDelimiters;
    
    /**
     * Whether to escape backslashes and colons in windows-style paths.
     * @parameter expression="${escapeWindowsPaths}" default-value="true"
     */
    protected boolean escapeWindowsPaths;
    
    /**
     * Whether to filter the context xml file template (ie. maven filtering token can be replaced with maven properties).<br>
     * The same rules apply as with resources filtering in 'maven-resources-plugin'.
     * @parameter expression="${filtering}" default-value="false"
     */
    protected boolean filtering;
    
    /**
     * Whether to parse the xml template and inject dynamic values (ie. web app resources and classpath entries) into the dom.<p>
     * If set to false, maven properties:
     * <ul><li>{@code jetty.conf-plugin.classpath}</li> and <li>{@code jetty.conf-plugin.webapp}</li></ul>
     * should be included in the template otherwise these values won't be set.<br>
     * In this case {@link #filtering} should be set to {@code true}.<p>
     * If you care about performance you should now that parsing as xml might take about twice longer.
     * 
     * @parameter expression="${parseAsXml}" default-value="true"
     */
    protected boolean parseAsXml;
}
