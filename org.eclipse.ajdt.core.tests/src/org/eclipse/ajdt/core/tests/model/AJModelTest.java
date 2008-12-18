/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matt Chapman  - initial version
 *******************************************************************************/

package org.eclipse.ajdt.core.tests.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aspectj.ajde.core.AjCompiler;
import org.aspectj.ajdt.internal.core.builder.AsmHierarchyBuilder;
import org.aspectj.asm.AsmManager;
import org.aspectj.asm.IProgramElement;
import org.eclipse.ajdt.core.AspectJPlugin;
import org.eclipse.ajdt.core.javaelements.AJCompilationUnitManager;
import org.eclipse.ajdt.core.model.AJModel;
import org.eclipse.ajdt.core.tests.AJDTCoreTestCase;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

/**
 * Tests for mapping between IProgramElement and IJavaElements
 * 
 */
public class AJModelTest extends AJDTCoreTestCase {

	public void testProgramElementToJavaElementDemo() throws Exception {
		IProject project = createPredefinedProject("MarkersTest"); //$NON-NLS-1$
		String filename = "src/tjp/Demo.java"; //$NON-NLS-1$
		String[][] results = {
				{ "Demo", "Demo" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "main(String[])", "main" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "go()", "go" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "field-set(int tjp.Demo.x)", "field-set(int tjp.Demo.x)" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "foo(int,Object)", "foo" }, //$NON-NLS-1$ //$NON-NLS-2$
				{
						"exception-handler(void tjp.Demo.<catch>(tjp.DemoException))", "exception-handler(void tjp.Demo.<catch>(tjp.DemoException))" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "bar(Integer)", "bar" } //$NON-NLS-1$ //$NON-NLS-2$
		};
		mappingTestForFile(project, filename, results);
	}

	public void testProgramElementToJavaElementGetInfo() throws Exception {
		IProject project = createPredefinedProject("MarkersTest"); //$NON-NLS-1$
		String filename = "src/tjp/GetInfo.aj"; //$NON-NLS-1$
		String[][] results = {
				{
						"declare warning: \"field set\"", "declare warning: \"field set\"" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "declare parents: implements Serializable", "declare parents" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "declare soft: tjp.DemoException", "declare soft" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "Demo.itd(int)", "Demo.itd" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "Demo.f", "Demo.f" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "before(): <anonymous pointcut>", "before" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "before(): demoExecs..", "before" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "before(): <anonymous pointcut>..", "before" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "after(): fieldSet..", "after" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "around(): demoExecs()..", "around" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "after(): <anonymous pointcut>", "after" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "printParameters(JoinPoint)", "printParameters" } //$NON-NLS-1$ //$NON-NLS-2$
		};
		mappingTestForFile(project, filename, results);
	}

	private void mappingTestForFile(IProject project, String filename,
			String[][] results) {
		IFile file = (IFile) project.findMember(filename);
		if (file == null)
			fail("Required file not found: " + filename); //$NON-NLS-1$

		String path = file.getRawLocation().toOSString();

		AjCompiler compiler = AspectJPlugin.getDefault().getCompilerFactory().getCompilerForProject(project);
		AsmManager ajModel = compiler.getModel();

		Map annotationsMap = ajModel.getInlineAnnotations(path,
				true, true);

		assertNotNull(
				"Didn't get annotations map for file: " + path, annotationsMap); //$NON-NLS-1$

		ICompilationUnit unit = AJCompilationUnitManager.INSTANCE
				.getAJCompilationUnit(file);
		if (unit == null) {
			unit = JavaCore.createCompilationUnitFrom(file);
		}

		assertNotNull("Didn't get a compilation unit from file: " + path, unit); //$NON-NLS-1$

		List toFind = new ArrayList();
		List toMatch = new ArrayList();
		for (int i = 0; i < results.length; i++) {
			toFind.add(results[i][0].intern());
			toMatch.add(results[i][1].intern());
		}

		Set keys = annotationsMap.keySet();
		for (Iterator it = keys.iterator(); it.hasNext();) {
			Object key = it.next();
			List annotations = (List) annotationsMap.get(key);
			for (Iterator it2 = annotations.iterator(); it2.hasNext();) {
				IProgramElement node = (IProgramElement) it2.next();
				String peName = node.toLabelString(false).intern();
				;
				IJavaElement je = AJModel.getInstance()
						.getCorrespondingJavaElement(node);
				if (je == null) {
					System.out.println("je is null"); //$NON-NLS-1$
					continue;
				}
				String jaName = je.getElementName().intern();
				int index = toFind.indexOf(peName);
				if (index == -1) {
					fail("Unexpected additional IProgramElement name found: " + peName); //$NON-NLS-1$
				} else {
					String expected = (String) toMatch.get(index);
					if (expected.equals(jaName)) {
						toFind.remove(index);
						toMatch.remove(index);
					} else {
						fail("Incorrect corresponding Java element. Found: " + jaName + " Expected: " + expected); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
		}

		// check that we found everything we were looking for
		if (toFind.size() > 0) {
			String missing = ""; //$NON-NLS-1$
			for (int j = 0; j < toFind.size(); j++) {
				missing += System.getProperty("line.separator"); //$NON-NLS-1$
				missing += (String) toFind.get(j);
			}
			fail("Did not find all expected IProgramElement names. Missing: " + missing); //$NON-NLS-1$
		}
	}
}