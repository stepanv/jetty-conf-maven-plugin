package net.uvavru.maven.plugins.jettyconf.internals;

import java.io.IOException;
import java.io.OutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

public class XmlWriter {

	protected void writeDocumentToStream(Document doc, OutputStream outputStream) throws IOException {
		DOMImplementationLS domImplementationLS = (DOMImplementationLS) doc
				.getImplementation().getFeature("LS", "3.0");
		LSOutput lsOutput = domImplementationLS.createLSOutput();
		lsOutput.setByteStream((OutputStream) outputStream);
		LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
		lsSerializer.write(doc, lsOutput);
		outputStream.flush();
		outputStream.close();
	}
}
