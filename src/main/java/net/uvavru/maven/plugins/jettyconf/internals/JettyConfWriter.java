package net.uvavru.maven.plugins.jettyconf.internals;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * Jetty Context xml file configuration writer;
 * <br>
 * 
 * This writer preserves xml indentation.<br>
 * 
 * @author stepan
 *
 */
public class JettyConfWriter extends XmlWriter {
	
	private List<File> classpathFiles;
	private List<File> webAppFiles;

	public JettyConfWriter(List<File> classpathFiles2, List<File> webappFiles2) {
		this.classpathFiles = classpathFiles2;
		this.webAppFiles = webappFiles2;
	}
	
	private XPath xpath = XPathFactory.newInstance().newXPath();

	/**
	 * Selects a node in the document according to the exprXPath
	 * 
	 * @param exprXPath
	 * @param doc
	 * @return the Node
	 * @throws XPathExpressionException
	 * @throws MojoFailureException
	 */
	private Node selectSingleNodeSafe(String exprXPath, Document doc) throws XPathExpressionException, MojoFailureException {
		Node node = (Node) xpath.evaluate(exprXPath, doc, XPathConstants.NODE);
		if (node == null) {
			throw new MojoFailureException("Cannot find xpath expression '" + exprXPath + "' in the xml template file.");
		}
		return node;
	}

	private void addClasspathEntries(Document doc) throws XPathExpressionException, MojoFailureException {
		Node extraCPSetNode = selectSingleNodeSafe("//Set[@name=\"extraClasspath\"]", doc);
		
		String ctxClasspath = "";
		for (File ctxEntry : classpathFiles) {
			ctxClasspath += "\n" + ctxEntry.toURI() + ";";
		}
		Text ctxClasspathNode = doc.createTextNode(ctxClasspath);
		extraCPSetNode.appendChild(ctxClasspathNode);
	}
	
	private void addWebAppEntries(Document doc) throws XPathExpressionException, MojoFailureException {
		Node extraResourcesNode = selectSingleNodeSafe("//Array[@type=\"java.lang.String\"]", doc);
		for (File resource : webAppFiles) {
			Element resourceNode = doc.createElement("Item");
			resourceNode.appendChild(doc.createTextNode(resource.toURI().toString()));
			extraResourcesNode.appendChild(resourceNode);
			extraResourcesNode.appendChild(doc.createTextNode("\n"));
		}
		
	}
	
	/**
	 * Adds classpath entries and web app resources to the {@code Document} parameter. <br>
	 *  
	 * @param doc
	 * @return
	 * @throws MojoExecutionException
	 * @throws MojoFailureException 
	 */
	public Document modifyContextXmlDoc(Document doc) throws MojoExecutionException, MojoFailureException {

		try {
			addClasspathEntries(doc);
			addWebAppEntries(doc);
		} catch (XPathExpressionException e) {
			throw new MojoExecutionException("Internal error in when locating node in the context file: " + e.getMessage(), e);
		}

		return doc;
	}
	
	

	private static Document parseXmlFile(File xmlFile) throws SAXException, IOException, MojoExecutionException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setExpandEntityReferences(false);
		DocumentBuilder db;
		
		try {
			db = dbf.newDocumentBuilder();
			Document doc = db.parse(xmlFile);

			return doc;
		} catch (ParserConfigurationException e) {
			throw new MojoExecutionException("Internal error in parser configuration: " + e.getMessage(), e);
		}
	}

	/**
	 * Writes modified Jetty context xml file to the output stream.
	 * 
	 * @param outputStream
	 * @param contextXmlTemplate
	 * @throws SAXException
	 * @throws MojoExecutionException
	 * @throws IOException
	 * @throws MojoFailureException
	 */
	public void writeToStream(OutputStream outputStream, File contextXmlTemplate) throws SAXException, MojoExecutionException, IOException, MojoFailureException {
		Document doc = modifyContextXmlDoc(parseXmlFile(contextXmlTemplate));
		
		writeDocumentToStream(doc, outputStream);
	}
}
