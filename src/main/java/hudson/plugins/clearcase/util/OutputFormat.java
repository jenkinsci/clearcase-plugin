/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.clearcase.util;

/**
 *
 * @author Henrik L. Hansen
 */
public interface OutputFormat {
    
    // Format 
    public static final String START_DELIMITER ="\\\"";             
    public static final String END_DELIMITER ="\\\" "; // Note the space!
    public static final String REGEX_GROUP = "\"(.*)\"\\s*";
    public static final String LINEEND = "\\n";
    public static final String PLACEHOLDER = "\\\" \\\" ";
   
    
    //Comment
    public static final String COMMENT="%c";
    public static final String COMMENT_NONEWLINE="%Nc";
    
    //Date
    public static final String DATE = "%d";
    public static final String DATE_NUMERIC = "%Nd";
    
    //Event
    public static final String EVENT ="%e";
    
    // Name
    public static final String NAME = "%n";
    public static final String NAME_ELEMENTNAME = "%En";
    public static final String NAME_VERSIONID="%Vn";

    //Event
    public static final String OPERATION ="%o";
    
       
    //User
    public static final String USER_ID = "%u";
    public static final String USER_FULLNAME = "%Fu";
    public static final String USER_GROUPNAME = "%Gu";
    public static final String USER_LOGIN_AND_GROUP = "%Lu";
    
    
    // UCM Activities
    public static final String UCM_ACTIVITY_HEADLINE=  "%[headline]p";
    public static final String UCM_ACTIVITY_STREAM=  "%[stream]p";
    public static final String UCM_ACTIVITY_VIEW=  "%[view]p";
    public static final String UCM_ACTIVITY_CONTRIBUTING=  "%[contrib_acts]p";    
    
    // UCM Versions
    public static final String UCM_VERSION_ACTIVITY="%[activity]p";
}
