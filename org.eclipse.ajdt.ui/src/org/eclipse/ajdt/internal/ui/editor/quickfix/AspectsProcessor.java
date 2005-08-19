/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ajdt.internal.ui.editor.quickfix;

import java.util.Collection;

import org.eclipse.ajdt.internal.ui.editor.AspectJEditor;
import org.eclipse.ajdt.internal.utils.AJDTUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.ChangeCorrectionProposal;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

class AspectsProcessor {

	public static void switchToAJEditorProposal(final IInvocationContext context,
			IProblemLocation problem, Collection proposals)
			throws CoreException {
		String name = CorrectionMessages.getString("QuickFix.OpenInAspectJEditor");
		final IEditorPart editor = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		ChangeCorrectionProposal proposal = new ChangeCorrectionProposal(name,
				null, 0, JavaPluginImages
						.get(JavaPluginImages.IMG_CORRECTION_CHANGE)) {
			public void apply(IDocument document) {
				if (editor instanceof ITextEditor) {
					final IWorkbenchPage page = editor.getSite().getPage();
					final IEditorInput input = editor.getEditorInput();
					((ITextEditor) editor).doSave(null);
					((ITextEditor) editor).close(true);
					Display.getCurrent().asyncExec(new Runnable() {
						public void run() {
							try {
								IDE.openEditor(page,
										input, AspectJEditor.ASPECTJ_EDITOR_ID);
							} catch (PartInitException e) {
							}
						}
					});
				}
			}
		};
		proposals.add(proposal);
	}
	
	public static void convertToAJProjectProposal(IInvocationContext context,
			IProblemLocation problem, Collection proposals)
			throws CoreException {
				final IProject project = context.getCompilationUnit().getJavaProject().getProject();
		
		String name = CorrectionMessages.getString("QuickFix.ConvertProjectToAspectJ");
		final IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		ChangeCorrectionProposal proposal = new ChangeCorrectionProposal(name, null, 0, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)) {
			public void apply(IDocument document) {
				if (editor instanceof ITextEditor) {
					try {
						AJDTUtils.addAspectJNature(project);
					} catch (CoreException e) {
					}
				}
			}
		};
		proposals.add(proposal);
	}
}
