package hudson.plugins.clearcase.util;

import org.apache.commons.digester3.Digester;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

public class DigesterUtil {

    public static Digester createDigester(boolean secure) throws SAXException
    {
        Digester digester = new Digester();
        if (secure) {
            digester.setXIncludeAware(false);
            try {
                digester.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                digester.setFeature("http://xml.org/sax/features/external-general-entities", false);
                digester.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                digester.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch ( ParserConfigurationException ex) {
                throw new SAXException("Failed to securely configure xml digester parser", ex);
            }
        }
        return digester;
    }
}
