package net.uvavru.maven.plugins.jettyconf;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.resolvers.ResolveDependenciesMojo;
import org.apache.maven.plugin.dependency.utils.DependencyStatusSets;

/**
 * Goal which generates jetty xml from resolved dependencies in properties.
 * 
 * @extendsPlugin dependency
 * @extendsGoal resolve
 * @goal generate
 * @requiresProject false
 */
public class PropertiesMojo extends ResolveDependenciesMojo {

	/**
	 * Comma Separated list of regexp patterns indicating how webapp resources
	 * paths should be translated.
	 * 
	 * @since 1.0
	 * @parameter expression="${webAppPatterns}" default-value=""
	 * @optional
	 */
	protected String webAppPatterns = "";

	/**
	 * Comma Separated list of regexp patterns indicating how webapp resources
	 * paths should be translated into.
	 * 
	 * @since 1.0
	 * @parameter expression="${webAppReplacements}" default-value=""
	 * @optional
	 */
	protected String webAppReplacements = "";

	/**
	 * Comma Separated list of regexp patterns indicating how classpath
	 * resources paths should be translated.
	 * 
	 * @since 1.0
	 * @parameter expression="${classpathPatterns}" default-value=""
	 * @optional
	 */
	protected String classpathPatterns = "";

	/**
	 * Regexp pattern that will match layout 'groupId:artifactId:type:classifier' string.
	 * This is how to indicate which artifacts should be considered for the classpath.
	 * 
	 * @since 1.0
	 * @parameter expression="${classpathMatchArtifactPattern}" default-value="[^:]*:[^:]*:jar:[^:]*"
	 * @optional
	 */
	protected String classpathMatchArtifactPattern = "[^:]*:[^:]*:jar:[^:]*";

	/**
	 * Regexp pattern that will match layout 'groupId:artifactId:type:classifier' string.
	 * This is how to indicate which artifacts should be considered for the web app resources.
	 * 
	 * @since 1.0
	 * @parameter expression="${webAppMatchArtifactPattern}" default-value="[^:]*:[^:]*:war:[^:]*"
	 * @optional
	 */
	protected String webAppMatchArtifactPattern = "[^:]*:[^:]*:war:[^:]*";

	/**
	 * Comma Separated list of artifact types current project should mock as
	 * 
	 * @since 1.0
	 * @parameter expression="${classpathPatterns}" default-value=""
	 * @optional
	 */
	protected String mockingTypes = "";

	/**
	 * Comma Separated list of regexp patterns indicating how classpath
	 * resources paths should be translated into.
	 * 
	 * @since 1.0
	 * @parameter expression="${classpathReplacements}" default-value=""
	 * @optional
	 */
	protected String classpathReplacements = "";
	
	/**
	 * Whether to add only directories into web app resources.
	 * Note that jetty accepts only directories!
	 * 
	 * @since 1.0
	 * @parameter expression="${webAppResourcesAsDirsOnly}" default-value="true"
	 * @optional
	 */
	protected boolean webAppResourcesAsDirsOnly = true;
	
	/**
	 * Whether to treat web app paths as windows path.
	 * In this case single backslash characters are doubled.
	 * 
	 * @since 1.0
	 * @parameter expression="${webappDirNonexistentTreatAsWindowsPath}" default-value="false"
	 * @optional
	 */
	protected boolean webappDirNonexistentTreatAsWindowsPath = false;
	
	
	
	/**
	 * If directory for a web app resource doesn't exist this pattern
	 * is used to transform the file path to a different path.<br>
	 * Implies {@code webAppResourcesAsDirsOnly} is set to {@code true}
	 * 
	 * @since 1.0
	 * @parameter expression="${webAppDirNonexistentAlternatePattern}" default-value="null"
	 * @optional
	 */
	protected String webAppDirNonexistentAlternatePattern = null;
	
	/**
	 * If directory for a web app resource doesn't exist this pattern
	 * is used to transform the file path to a different path.
	 * Implies {@code webAppResourcesAsDirsOnly} is set to {@code true}
	 * 
	 * @since 1.0
	 * @parameter expression="${webAppDirNonexistentAlternateReplacement}" default-value="null"
	 * @optional
	 */
	protected String webAppDirNonexistentAlternateReplacement = null;
	

	private DependencyStatusSets results;

	public void execute() throws MojoExecutionException {
		// get sets of dependencies
		results = this.getDependencySets(true);

		Set<Artifact> webAppResources = new HashSet<Artifact>();
		Set<Artifact> classpathResources = new HashSet<Artifact>();

		// adding current artifact
		for (String type : mockingTypes.split(",")) {
			Artifact active = this.project.getArtifact();
			Artifact current = new DefaultArtifact(active.getGroupId(),
					active.getArtifactId(), active.getVersionRange(),
					active.getScope(), type, active.getClassifier(),
					active.getArtifactHandler());
			current.setFile(new File(project.getBuild().getOutputDirectory()));
			results.getResolvedDependencies().add(current);
		}

		Pattern webAppArtifactPattern = Pattern.compile(webAppMatchArtifactPattern);
		Pattern classpathArtifactPattern = Pattern.compile(classpathMatchArtifactPattern);

		for (Artifact artifact : results.getResolvedDependencies()) {
			getLog().debug(
					"resolved: " + artifact + ", path: " + artifact.getFile());
			String artifactDescriptor = String.format("%s:%s:%s:%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(), artifact.getClassifier());
			getLog().debug("Matching against descriptor: " + artifactDescriptor);
			
			if (webAppArtifactPattern.matcher(artifactDescriptor).matches()) {
				getLog().debug("adding to webapp resources");
				webAppResources.add(artifact);
			}
			if (classpathArtifactPattern.matcher(artifactDescriptor).matches()) {
				getLog().debug("adding to classpath resources");
				classpathResources.add(artifact);
			}
		}

		Set<File> webAppTranslatedFiles = translatePaths(webAppResources,
				webAppPatterns, webAppReplacements);
		Set<File> classpathTranslatedFiles = translatePaths(classpathResources,
				classpathPatterns, classpathReplacements);

		canonicalPaths(webAppResources, webAppTranslatedFiles);
		canonicalPaths(classpathResources, classpathTranslatedFiles);

		for (File file : webAppTranslatedFiles) {
			getLog().debug("webapp URI: " + file.toURI());
		}

		for (File file : classpathTranslatedFiles) {
			getLog().debug("classpath URI: " + file.toURI());
		}

		String webapp = "";
		Pattern webAppAlternativePattern = null;
		if (webAppDirNonexistentAlternatePattern != null) {
			if (webAppDirNonexistentAlternateReplacement == null) {
				getLog().warn(String.format("Not using %s as web app alternate pattern because 'replacement' is null", webAppDirNonexistentAlternatePattern));
			} else {
			webAppAlternativePattern = Pattern.compile(webAppDirNonexistentAlternatePattern);
			}
		}
		if (webappDirNonexistentTreatAsWindowsPath) {
			webAppDirNonexistentAlternateReplacement = webAppDirNonexistentAlternateReplacement.replace("\\", "\\\\");
		}
		for (File file : webAppTranslatedFiles) {
			try {
				if (webAppAlternativePattern != null && !file.isDirectory() && webAppResourcesAsDirsOnly) {
					if (webAppAlternativePattern.matcher(file.getCanonicalPath()).matches()) {
						String alternateFilePath = webAppAlternativePattern.matcher(file.getCanonicalPath()).replaceAll(webAppDirNonexistentAlternateReplacement);
						File alternateFile = new File(alternateFilePath);
						if (alternateFile.exists() && alternateFile.isDirectory()) {
							getLog().warn(String.format("transforming '%s' into '%s'", file.getCanonicalPath(), alternateFile.getCanonicalPath()));
							file = alternateFile;
						} else {
							getLog().warn("File " + alternateFilePath + " doesn't exist .. skipping alternate path");
						}
					} else {
						getLog().warn("File " + file.getCanonicalPath() + " doesn't match web alternate pattern.");
					}
				} else {
					getLog().warn("Condition for: " + file + " not met..");
				}
			} catch (IOException e) {
				throw new MojoExecutionException("Cannot generate alternate path for: " + file.getName(), e);
			}
			if (file.isDirectory() || !webAppResourcesAsDirsOnly) {
				webapp += "\n<Item>" + file.toURI() + "</Item>";
			} else {
				getLog().warn(
						"Not adding artifact file: '" + file
								+ "' into webapps because it's not a directory");
			}
		}
		this.getProject().getProperties()
				.setProperty("jetty.conf-plugin.webapp", webapp);

		String classpath = "";
		for (File file : classpathTranslatedFiles) {
			classpath += "\n" + file.toURI() + ";";
		}
		this.getProject().getProperties()
				.setProperty("jetty.conf-plugin.classpath", classpath);

		getLog().info(
				"Generated properties 'jetty.conf-plugin.classpath' and 'jetty.conf-plugin.webapp'");

	}

	private void canonicalPaths(Set<Artifact> artifacts, Set<File> files)
			throws MojoExecutionException {
		for (Artifact artifact : artifacts) {
			try {
				files.add(artifact.getFile().getCanonicalFile());
			} catch (IOException e) {
				throw new MojoExecutionException(
						"Cannot generate path for an artifact: " + artifact, e);
			}
		}
	}

	/**
	 * Translate paths and reduces artifacts set if matched.
	 * 
	 * @param artifacts
	 * @param patterns
	 * @param replacements
	 * @return
	 * @throws MojoExecutionException
	 */
	private Set<File> translatePaths(Set<Artifact> artifacts, String patterns,
			String replacements) throws MojoExecutionException {
		Set<File> files = new HashSet<File>();

		if (patterns == null | replacements == null) {
			return files;
		}

		Artifact artifact = null;

		try {
			String[] patternStrings = patterns.split(",");
			String[] replacementsStrings = replacements.split(",");
			for (int i = 0; i < patternStrings.length
					&& i < replacementsStrings.length; ++i) {
				Pattern pattern = Pattern.compile(patternStrings[i]);

				Iterator<Artifact> it = artifacts.iterator();
				while (it.hasNext()) {
					artifact = it.next();

					String path = artifact.getFile().getCanonicalPath();

					File file = translatePath(path, pattern,
							replacementsStrings[i]);
					if (file != null) {
						files.add(file);
						it.remove();
					}
				}
			}

		} catch (IOException e) {
			throw new MojoExecutionException(
					"Cannot generate path for an artifact: " + artifact, e);
		}
		return files;
	}

	private File translatePath(String path, Pattern pattern, String replacement) {
		if (pattern.matcher(path).matches()) {
			return new File(pattern.matcher(path).replaceAll(replacement));
		}
		return null;
	}
}
