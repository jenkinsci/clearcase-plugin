package hudson.plugins.clearcase;

import java.io.File;

import java.io.IOException;

import org.xml.sax.SAXException;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;

/***********************************************************************************************************************
 * ClearCase change log parser.
 * 
 * @author Erik Ramfelt
 */
public class ClearCaseChangeLogParser extends ChangeLogParser {
    @Override
    public ChangeLogSet<ClearCaseChangeLogEntry> parse(AbstractBuild build, File changelogFile) throws IOException,
            SAXException {
        return ClearCaseChangeLogSet.parse(build, changelogFile);
    }
}
