/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sian January  - initial version
 *******************************************************************************/
package org.eclipse.ajdt.ui.tests.visual;

import org.eclipse.ajdt.core.javaelements.AJCompilationUnitManager;
import org.eclipse.ajdt.ui.tests.testutils.Utils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.SWT;

/**
 * A second visual test case for organise imports in a .aj file
 */
public class OrganiseImportsTest2 extends VisualTestCase {

	/**
	 * Test organize imports when there's nothing to add or remove
	 * in an aspect that extends an abstract aspect and has more 
	 * than one variable.
	 * @throws Exception
	 */
	public void testOrganiseImports2() throws Exception {
		IProject project = Utils.createPredefinedProject("Spacewar Example");
		try {
			IFile gameSynchronizationFile = (IFile)project.findMember("src/spacewar/GameSynchronization.aj");
			assertTrue("The Spacewar Example project should contain a file called 'GameSynchronization.aj'", gameSynchronizationFile != null );
			Utils.openFileInDefaultEditor(gameSynchronizationFile, true);
			final ICompilationUnit cUnit = AJCompilationUnitManager.INSTANCE.getAJCompilationUnit(gameSynchronizationFile);
			assertTrue("GameSynchronization.aj should start with one imports", cUnit.getImports().length == 1);
			Utils.waitForJobsToComplete();			
			
//			 Post Ctrl+Shift+O to organise imports
			postKeyDown(SWT.CTRL);
			postKeyDown(SWT.SHIFT);
			postCharacterKey('o');
			postKeyUp(SWT.SHIFT);
			postKeyUp(SWT.CTRL);

//			 Wait for an error condition to make sure it's caught if it does occur
			new DisplayHelper() {
				protected boolean condition() {
					try {
						return  cUnit.getImports().length == 0;
					} catch (JavaModelException e) {
					}
					return false;
				}
			
			}.waitForCondition(display, 3000);
			assertTrue("GameSynchronization.aj should now have one import, has " + cUnit.getImports().length, cUnit.getImports().length == 1);
			
			// Post Ctrl+S to save the file
			postKeyDown(SWT.CTRL);
			postCharacterKey('s');
			postKeyUp(SWT.CTRL);

			// Wait for an error condition to make sure it's caught if it does occur
			new DisplayHelper() {
				protected boolean condition() {
					try {					
						IMarker[] problemMarkers = cUnit.getResource().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
						return problemMarkers.length == 1;
					} catch (CoreException e) {
					}
					return false;
				}
			
			}.waitForCondition(display, 3000);
			IMarker[] problemMarkers = cUnit.getResource().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
			assertTrue("GameSynchronization.aj should not have any problems", problemMarkers.length == 0);

		} finally {
			Utils.waitForJobsToComplete();
			Utils.deleteProject(project);
		}
	}

	
}
