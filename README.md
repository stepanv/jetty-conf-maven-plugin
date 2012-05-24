Jetty context XML configuration plugin generator helper.
=======================		

Main features
-------------
1. There is no need for any unnecessary copying of web resources or classpath entries.
2. Universal solution which can be integrated in 'jetty-maven-plugin', WTP Jetty Adapters


General Info
------------
		
This plugin helps you to generate your Context XML file with a dynamic content such as:
	* classpath entries
	* web app resources
	* maven properties

### Requirements
Plugin needs a template Jetty context XML file where the dynamic content is injected.

### Injection possibilities
Injection of dynamic content is supported as:
	* altering the DOM with new values
	* with enabled filtering dynamic values can be replaced as properties (see bellow)

### Filtering
Filtering of the template file is supported. 
Basically it means you can include content from your maven properties in your context XML file.

If desired plugin sets two maven properties with the dynamic content:
	* jetty.conf-plugin.classpath
	* jetty.conf-plugin.webapp
These properties might be used for filtering as well.


How it works
------------

Plugin resolves project dependency artifacts. 
These artifacts are transformed into webapp resources and classpath entries according to the maven configuration.


Enhancement with Maven Dependency plugin
----------------------------------------

This plugin was designed to reuse files unpacked by maven-dependency-plugin (from its goal unpack-dependencies).
If you application contain web resource, this might be conditionally unpacked if not resolved to a directory.
 

Compatibility
-------------

### Multi-module project
Works with multi-module projects. 
	* Run the build from the parent project.
	* jetty-conf-maven-plugin execution must run in the project module (e.g. war)
	Then dependencies are resolved in the multimodule project)

### Eclipse m2e (Maven integration in Eclipse, a.k.a m2eclipse)
Works in Eclipse with enabled m2e.
	* Workspace artifact resolution must be enabled

There is not m2e connector yet. As a workaround use:
1. Enable plugin execution from the pluginManagement m2e configuration
2. Use maven-resources-plugin to copy 'nothing' into the 'targetDir' and as a result the context file will be refreshed in the Eclipse workspace.


Other info
----------

I've created this plugin because there is no good enough solution for running jetty from Elipse or a command line.