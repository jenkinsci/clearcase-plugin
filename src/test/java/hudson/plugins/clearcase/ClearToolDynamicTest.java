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
package hudson.plugins.clearcase;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItemInArray;
import hudson.FilePath;
import hudson.util.VariableResolver;

import java.io.InputStream;
import java.io.OutputStream;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClearToolDynamicTest extends AbstractWorkspaceTest {

	private Mockery context;

	private ClearTool clearToolExec;
	private ClearToolLauncher launcher;

	private VariableResolver resolver;

	@Before
	public void setUp() throws Exception {
		createWorkspace();
		context = new Mockery();

		launcher = context.mock(ClearToolLauncher.class);
		resolver = context.mock(VariableResolver.class);
		clearToolExec = new ClearToolDynamic(resolver, launcher, "/cc/drives");
	}

	@After
	public void tearDown() throws Exception {
		deleteWorkspace();
	}

	@Test
	public void testSetcs() throws Exception {
		context.checking(new Expectations() {
			{
				one(launcher).getWorkspace();
				will(returnValue(workspace));
				one(launcher).run(
						with(allOf(hasItemInArray("setcs"),
								hasItemInArray("-tag"),
								hasItemInArray("viewName"))),
						with(aNull(InputStream.class)),
						with(aNull(OutputStream.class)),
						with(aNull(FilePath.class)));
				will(returnValue(Boolean.TRUE));
			}
		});

		clearToolExec.setcs("viewName", "configspec");
		context.assertIsSatisfied();
	}

	@Test
	public void testStartview() throws Exception {
		context.checking(new Expectations() {
			{
				one(launcher).run(
						with(allOf(hasItemInArray("startview"),
								hasItemInArray("viewName"))),
						with(aNull(InputStream.class)),
						with(aNull(OutputStream.class)),
						with(aNull(FilePath.class)));
			}
		});

		clearToolExec.startView("viewName");
		context.assertIsSatisfied();
	}
}
