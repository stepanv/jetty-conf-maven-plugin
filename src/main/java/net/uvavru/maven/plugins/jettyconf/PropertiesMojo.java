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

		for (Artifact artifact : results.getResolvedDependencies()) {
			getLog().debug(
					"resolved: " + artifact + ", path: " + artifact.getFile());
			if ("war".equals(artifact.getType())) {
				webAppResources.add(artifact);
			} else {
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
			getLog().info("webapp URI: " + file.toURI());
		}

		for (File file : classpathTranslatedFiles) {
			getLog().debug("classpath URI: " + file.toURI());
		}

		String webapp = "";
		for (File file : webAppTranslatedFiles) {
			if (!file.isDirectory()) {
				getLog().warn(
						"Not adding artifact file: '" + file
								+ "' into webapps because it's not a directory");
			} else {
				webapp += "\n<Item>" + file.toURI() + "</Item>";
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
