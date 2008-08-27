/**********************************************************************
 Copyright (c) 2002, 2006 IBM Corporation and others.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Public License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/epl-v10.html
 Contributors:
 Adrian Colyer, Andy Clement - initial version
 Helen Hawkins - updated for new ajde interface (bug 148190)

 **********************************************************************/
package org.eclipse.ajdt.internal.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.aspectj.weaver.ast.HasAnnotation;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ajdt.core.AJLog;
import org.eclipse.ajdt.core.AspectJCorePreferences;
import org.eclipse.ajdt.internal.launching.LaunchConfigurationManagementUtils;
import org.eclipse.ajdt.internal.ui.ajde.AJDTErrorHandler;
import org.eclipse.ajdt.internal.ui.text.UIMessages;
import org.eclipse.ajdt.internal.ui.wizards.AspectPathBlock;
import org.eclipse.ajdt.internal.ui.wizards.InPathBlock;
import org.eclipse.ajdt.internal.ui.wizards.TabFolderLayout;
import org.eclipse.ajdt.internal.utils.AJDTUtils;
import org.eclipse.ajdt.ui.AspectJUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ClasspathAttribute;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.internal.launching.JREContainer;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.preferences.PreferencesMessages;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathBasePage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * The properties page for the AspectJ build path options that can be set.
 * The aspect path and in path are stored with the Java classpath
 * THe outjar and the inpath out folder are stored in project specific settings
 */
public class AspectJProjectPropertiesPage extends PropertyPage implements
		IStatusChangeListener {
    
    /**
     * Listens for changes that require a refresh or a commit of
     * the properties page.
     */
    private class PageChangeListener implements Listener, IResourceChangeListener {
        public void handleEvent(Event event) {
            if (event.type == SWT.Hide) {
                if (hasChanges()) {
                    commit();
                }
            }
        }

        public void resourceChanged(IResourceChangeEvent event) {
            // should traverse the resource delta to make sure it
            // is the .classpath
            if (event.getDelta().findMember(thisProject.getFile(
                    new Path(".classpath")).getFullPath()) != null) { //$NON-NLS-1$
                refreshPathBlock();
            }
        }

        /**
         * runs resetPathBlocks() in the UI thread
         */
        private void refreshPathBlock() {
            if (hasChangesInClasspathFile()) {
                // must run from the UI thread
                Display.getDefault().asyncExec(new Runnable() {
                	public void run() {
                        resetPathBlocks();
                	}
                });
                // would be better to use a runnable that has a progress bar here, but
                // this is giving me invocation errors. so not using it.
//                IRunnableWithProgress runnable = new IRunnableWithProgress() {
//                    public void run(IProgressMonitor monitor) {
//                        resetPathBlocks();
//                    }
//                };
//                try {
//                    PlatformUI.getWorkbench().getProgressService().runInUI(
//                    		new ProgressMonitorDialog(null), runnable, null);
//                } catch (InvocationTargetException e) {
//                } catch (InterruptedException e) {
//                	// cancelled
//                }
            }
        }
    }

	private static final String INDEX = "pageIndex"; //$NON-NLS-1$

	private int fPageIndex;

	private static final String PAGE_SETTINGS = "AspectJBuildPropertyPage"; //$NON-NLS-1$

	public static final String PROP_ID = "org.eclipse.ajdt.internal.ui.AspectJProjectPropertiesPage"; //$NON-NLS-1$

	// compiler options for ajc 
	private StringFieldEditor outputJarEditor;

	// Relevant project for which the properties are being set
	private IProject thisProject;

	private BuildPathBasePage fCurrPage;

	private InPathBlock fInPathBlock;

	private AspectPathBlock fAspectPathBlock;

	/**
	 * Build the page of properties that can be set on a per project basis for the
	 * AspectJ compiler.
	 */
	protected Control createContents(Composite parent) {
		noDefaultAndApplyButton();
		// Grab the resource (must be a project) for which this property page
		// is being created
		thisProject = getProject();

		Composite composite = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.numColumns = 1;
		composite.setLayout(layout);

		TabFolder folder = new TabFolder(composite, SWT.NONE);
		folder.setLayout(new TabFolderLayout());
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));

		fInPathBlock = new InPathBlock(this, 0);
		fInPathBlock.tabContent(folder);

		fAspectPathBlock = new AspectPathBlock(this, 0);
		fAspectPathBlock.tabContent(folder);

		// populate the two path block tabs
		resetPathBlocks();

		TabItem item;
		item = new TabItem(folder, SWT.NONE);
		item.setText(UIMessages.compilerPropsPage_outputJar);
		item.setControl(outputTab(folder));

		folder.setSelection(fPageIndex);
		fCurrPage = (BuildPathBasePage) folder.getItem(fPageIndex).getData();
		folder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				tabChanged(e.item);
			}
		});

		initializeTimeStamps();
		updatePageContents();

		fListener = new PageChangeListener();
        getControl().addListener(SWT.Hide, fListener);
        thisProject.getWorkspace().addResourceChangeListener(fListener, IResourceChangeEvent.POST_CHANGE);
        
		return composite;
	}

    private void resetPathBlocks() {
        // Inpath block
		IClasspathEntry[] initalInpath = null;
		try {
			initalInpath = getInitialPathValue(thisProject, AspectJCorePreferences.INPATH_ATTRIBUTE);
		} catch (CoreException ce) {
			AJDTErrorHandler.handleAJDTError(
					UIMessages.InPathProp_exceptionInitializingInpath_title,
					UIMessages.InPathProp_exceptionInitializingInpath_message,
					ce);
		}
		fInPathBlock.init(JavaCore.create(thisProject), initalInpath);


        // Aspect path block
		IClasspathEntry[] initialAspectpath = null;
		try {
			initialAspectpath = getInitialPathValue(thisProject, AspectJCorePreferences.ASPECTPATH_ATTRIBUTE);
		} catch (CoreException ce) {
			AJDTErrorHandler.handleAJDTError(
							UIMessages.AspectPathProp_exceptionInitializingAspectpath_title,
							UIMessages.AspectPathProp_exceptionInitializingAspectpath_message,
							ce);
		}
		fAspectPathBlock.init(JavaCore.create(thisProject), initialAspectpath);
    }

	private IClasspathEntry[] getInitialPathValue(IProject project, IClasspathAttribute attribute)
			throws CoreException {
		List newPath = new ArrayList();
		
		String[] v;
		if (AspectJCorePreferences.isAspectPathAttribute(attribute)) {
			v = AspectJCorePreferences.getRawProjectAspectPath(project);
		} else {
			v = AspectJCorePreferences.getRawProjectInpath(project);
		}
		if (v == null) {
			return null;
		}
		String paths = v[0];
		String cKinds = v[1];
		String eKinds = v[2];
		if ((paths != null && paths.length() > 0)
				&& (cKinds != null && cKinds.length() > 0)
				&& (eKinds != null && eKinds.length() > 0)) {
			StringTokenizer sTokPaths = new StringTokenizer(paths,
					File.pathSeparator);
			StringTokenizer sTokCKinds = new StringTokenizer(cKinds,
					File.pathSeparator);
			StringTokenizer sTokEKinds = new StringTokenizer(eKinds,
					File.pathSeparator);
			if ((sTokPaths.countTokens() == sTokCKinds.countTokens())
					&& (sTokPaths.countTokens() == sTokEKinds.countTokens())) {
				while (sTokPaths.hasMoreTokens()) {
					IClasspathEntry entry = new ClasspathEntry(Integer
							.parseInt(sTokCKinds.nextToken()), // content kind
							Integer.parseInt(sTokEKinds.nextToken()), // entry
							// kind
							new Path(sTokPaths.nextToken()), // path
							new IPath[] {}, // inclusion patterns
							new IPath[] {}, // exclusion patterns
							null, // src attachment path
							null, // src attachment root path
							null, // output location
							false, // is exported ?
							null, // accessRules
							false, // combine access rules?
							new IClasspathAttribute[0] // extra attributes?
					);
					newPath.add(entry);
				}// end while
			}// end if string token counts tally
		}// end if we have something valid to work with

        // Bug 243356
        // also get entries that are contained in containers
        // where the containers *don't* have the path attribute
        // but the element does.
        // this requires looking inside the containers.
        newPath.addAll(getEntriesInContainers(project, attribute));

		if (newPath.size() > 0) {
			return (IClasspathEntry[]) newPath.toArray(new IClasspathEntry[0]);
		} else {
			return null;
		}
	}
	
    // Bug 243356
    // Look inside all container classpath elements in the project
    protected List /*CPListElement*/ getEntriesInContainers(IProject project, IClasspathAttribute attribute) {
    	try {
    		IJavaProject jProject = JavaCore.create(project);
    		// get the raw classpath of the project
			IClasspathEntry[] allEntries = jProject.getRawClasspath();
			List entriesWithAttribute = new ArrayList();
			for (int i = 0; i < allEntries.length; i++) {
				// for each container element, peek inside it
				if (allEntries[i].getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
		            IClasspathContainer container = 
		                JavaCore.getClasspathContainer(allEntries[i].getPath(), jProject);
		            if (container != null && !(container instanceof JREContainer)) {
			            IClasspathEntry[] containerEntries = container.getClasspathEntries();
			            for (int j = 0; j < containerEntries.length; j++) {
			    			// iterate through each element and add 
			    			// 	to the path those that have the appropriate attribute
			            	if (hasClasspathAttribute(containerEntries[j], attribute)) {
			            		addContainerToAttribute(containerEntries[j], attribute, container);
			            		entriesWithAttribute.add(containerEntries[j]);
			            	}
			            }
		            }
				}
			}
	    	return entriesWithAttribute;

		} catch (JavaModelException e) {
		}
    	return Collections.EMPTY_LIST;
    }
    
    private void addContainerToAttribute(IClasspathEntry classpathEntry,
			IClasspathAttribute attribute, IClasspathContainer container) {
    	// find the attribute
    	IClasspathAttribute[] attributes = classpathEntry.getExtraAttributes();
    	for (int i = 0; i < attributes.length; i++) {
			if (attributes[i].getName().equals(attribute.getName())) {
				attributes[i] = new ClasspathAttribute(attribute.getName(), container.getPath().toPortableString());
			}
		}
	}

	private boolean hasClasspathAttribute(IClasspathEntry entry, IClasspathAttribute attribute) {
    	IClasspathAttribute[] allAttributes = entry.getExtraAttributes();
    	for (int i = 0; i < allAttributes.length; i++) {
			if (allAttributes[i].getName().equals(attribute.getName())) {
				return true;
			}
		}
    	return false;
    }


	private Composite outputTab(Composite composite) {
		Composite pageComposite = createPageComposite(composite, 3);

		// This will cover the top row of the panel.
		Composite row0Composite = createRowComposite(pageComposite, 1);
		//createText(row0Composite, UIMessages.compilerPropsPage_description);
		Label title = new Label(row0Composite, SWT.LEFT | SWT.WRAP);
		title.setText(UIMessages.compilerPropsPage_description);
		
		Composite row3Comp = createRowComposite(pageComposite, 2);

		outputJarEditor = new StringFieldEditor("", //$NON-NLS-1$
				UIMessages.compilerPropsPage_outputJar, row3Comp);

		return pageComposite;

	}

	/**
	 * Creates composite control and sets the default layout data.
	 *
	 * @param parent  the parent of the new composite
	 * @param numColumns  the number of columns for the new composite
	 * @return the newly-created coposite
	 */
	private Composite createPageComposite(Composite parent, int numColumns) {
		Composite composite = new Composite(parent, SWT.NONE);

		//GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		layout.makeColumnsEqualWidth = true;
		composite.setLayout(layout);

		//GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		return composite;
	}

	private Composite createRowComposite(Composite parent, int numColumns) {
		Composite composite = new Composite(parent, SWT.NONE);

		//GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		layout.makeColumnsEqualWidth = true;
		composite.setLayout(layout);

		//GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 3;
		composite.setLayoutData(data);

		return composite;
	}
	 
	private boolean checkIfOnInpath(IProject project, String outJarStr) {
		String[] oldInpath = 
		    AspectJCorePreferences.getRawProjectInpath(project);
		String[] seperatedOldInpath = oldInpath[0].split(";"); //$NON-NLS-1$

		String outJar = ('/'+thisProject.getName()+'/'+outJarStr);
		for (int j = 0; j < seperatedOldInpath.length; j++) {
			if ((seperatedOldInpath[j].equals(outJar))&& 
					!(seperatedOldInpath[j].equals(""))) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}
	
	private boolean checkIfOnAspectpath(IProject project, String string) {
		String[] oldAspectpath = AspectJCorePreferences
				.getRawProjectAspectPath(project);
		String[] seperatedOldAspectpath = oldAspectpath[0].split(";"); //$NON-NLS-1$
		
		String outJar = ('/'+thisProject.getName()+'/'+string);
		for (int j = 0; j < seperatedOldAspectpath.length; j++) {
			if ((seperatedOldAspectpath[j].equals(outJar)) && 
					!(seperatedOldAspectpath[j].equals(""))) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}

	/**
	 * overriding performApply() for PreferencePageBuilder.aj
	 */
	public void performApply() {
		commit();
	}

	/**
	 * When OK is clicked on the property page, this method stores the current
	 * values of all the buttons/fields on the page.  The state is stored as a set
	 * of persistent properties against the project resource.
	 * This method is also called if the user clicks 'Apply' on the property page.
	 */
	public boolean performOk() {
	    return commit();
	}
	    
    private boolean commit() {
        
        // ignore changes to .classpath that occur during commits
        thisProject.getWorkspace().removeResourceChangeListener(fListener);
        try {
    		String oldOutJar = AspectJCorePreferences.getProjectOutJar(thisProject);
    		IClasspathEntry oldEntry = null;
    		if (oldOutJar != null && !oldOutJar.equals("")) { //$NON-NLS-1$
    			oldEntry = new org.eclipse.jdt.internal.core.ClasspathEntry(
    					IPackageFragmentRoot.K_BINARY, // content kind
    					IClasspathEntry.CPE_LIBRARY, // entry kind
    					new Path(thisProject.getName() + '/' + oldOutJar)
    							.makeAbsolute(), // path
    					new IPath[] {}, // inclusion patterns
    					new IPath[] {}, // exclusion patterns
    					null, // src attachment path
    					null, // src attachment root path
    					null, // output location
    					false, // is exported ?
    					null, //accessRules
    					false, //combine access rules?
    					new IClasspathAttribute[0] // extra attributes?
    			);
    		}
    		String outJar = outputJarEditor.getStringValue();
    		IClasspathEntry newEntry = null;
    		if (outJar != null && !outJar.equals("")) { //$NON-NLS-1$
    			newEntry = new org.eclipse.jdt.internal.core.ClasspathEntry(
    					IPackageFragmentRoot.K_BINARY, // content kind
    					IClasspathEntry.CPE_LIBRARY, // entry kind
    					new Path(thisProject.getName() + '/' + outJar) 
    							.makeAbsolute(), // path
    					new IPath[] {}, // inclusion patterns
    					new IPath[] {}, // exclusion patterns
    					null, // src attachment path
    					null, // src attachment root path
    					null, // output location
    					false, // is exported ?
    					null, //accessRules
    					false, //combine access rules?
    					new IClasspathAttribute[0] // extra attributes?
    			);
    		}
    		if (checkIfOnInpath(thisProject, outJar)||
    				checkIfOnAspectpath(thisProject, outJar)){
    			MessageDialog.openInformation(getShell(), UIMessages.buildpathwarning_title, 
    			        UIMessages.buildConfig_invalidOutjar);
    			outputJarEditor.setStringValue(oldOutJar);
    		} else {
    		    LaunchConfigurationManagementUtils.updateOutJar(JavaCore
    		            .create(thisProject), oldEntry, newEntry);
    		    AspectJCorePreferences.setProjectOutJar(thisProject, outputJarEditor
    		            .getStringValue());
    		}
    		if (fInPathBlock != null) {
    			IRunnableWithProgress runnable = new IRunnableWithProgress() {
    				public void run(IProgressMonitor monitor)
    						throws InvocationTargetException, InterruptedException {
    					try {
    						fInPathBlock.configureJavaProject(monitor);
    					} catch (CoreException e) {
    						throw new InvocationTargetException(e);
    					}
    				}
    			};
    			
    			try {
    			    PlatformUI.getWorkbench().getProgressService().run(false, true, runnable);
    			} catch (InvocationTargetException e) {
    				return false;
    			} catch (InterruptedException e) {
    				// cancelled
    				return false;
    			}
    			
    			// set the inpath's output folder
    			// we should only be setting the out path if it is different
    			// from the default
    			// probably should do more checking on this, but hold off for now.
    			AspectJCorePreferences.setProjectInpathOutFolder(getProject(), fInPathBlock.getOutputFolder());
    		}
    
    		if (fAspectPathBlock != null) {
    			getSettings().put(INDEX, fAspectPathBlock.getPageIndex());
    
    			IRunnableWithProgress runnable = new IRunnableWithProgress() {
    				public void run(IProgressMonitor monitor)
    						throws InvocationTargetException, InterruptedException {
    					try {
    						fAspectPathBlock.configureJavaProject(monitor);
    					} catch (CoreException e) {
    						AJDTErrorHandler.handleAJDTError(
    										PreferencesMessages.BuildPathsPropertyPage_error_message,
    										e);
    					}
    				}
    			};
    			IRunnableWithProgress op = new WorkspaceModifyDelegatingOperation(
    					runnable);
    			try {
    			    PlatformUI.getWorkbench().getProgressService().run(false, true, op);
    			} catch (InvocationTargetException e) {
    				return false;
    			} catch (InterruptedException e) {
    				// cancelled
    				return false;
    			}
    		}
    		AJDTUtils.refreshPackageExplorer();
    		initializeTimeStamps();
    		return true;
        } finally {
            // now we care about resource changes again
            thisProject.getWorkspace().addResourceChangeListener(fListener);
        }
	}

	protected IDialogSettings getSettings() {
		IDialogSettings pathSettings = AspectJUIPlugin.getDefault()
				.getDialogSettings();
		IDialogSettings pageSettings = pathSettings.getSection(PAGE_SETTINGS);
		if (pageSettings == null) {
			pageSettings = pathSettings.addNewSection(PAGE_SETTINGS);
			// Important. Give the key INDEX a value which is one less than the
			// number of tabs that will be displayed in the page. The aspectpath
			// page will have two tabs hence ...
			pageSettings.put(INDEX, 2);
		}
		return pageSettings;
	}


	/**
	 * Bug 76811: All fields in the preference page are put back to their
	 * default values. The underlying settings are not changed until "ok" is
	 * clicked. This now behaves like the jdt pages.
	 */
	public void performDefaults() {
		AJLog.log("Compiler properties reset to default for project: " //$NON-NLS-1$ 
		        + thisProject.getName()); 
		        outputJarEditor.setStringValue(""); //$NON-NLS-1$
	}

	/**
	 * Ensure the widgets state reflects the persistent property values.
	 */
	public void updatePageContents() {
		outputJarEditor.setStringValue(AspectJCorePreferences
				.getProjectOutJar(thisProject));
	}

	/**
	 * Returns the project for which this page is currently open.
	 */
	public IProject getThisProject() {
		return thisProject;
	}

	/**
	 * overriding dispose() for PreferencePaageBuilder.aj
	 */
	public void dispose() {
		super.dispose();
		thisProject.getWorkspace().removeResourceChangeListener(fListener);		
	}

	private IProject getProject() {
		if (testing) {
			return thisProject;
		} else {
			return (IProject) getElement();
		}
	}

	// ---------------- methods for testing -----------------
	private boolean testing = false;

    private long fFileTimeStamp;

    private PageChangeListener fListener;

	// set the project for which this properties page is dealing with
	public void setThisProject(IProject project) {
		thisProject = project;
	}

	// set whether or not we are testing
	public void setIsTesting(boolean isTesting) {
		testing = isTesting;
	}

	// set the outjar value
	public void setOutjarValue(String outjar) {
		outputJarEditor.setStringValue(outjar);
	}

	// get the outjar value
	public String getOutjarValue() {
		return outputJarEditor.getStringValue();
	}

	public void statusChanged(IStatus status) {
	    setValid(!status.matches(IStatus.ERROR));
	    StatusUtil.applyToStatusLine(this, status);
	}

	protected void tabChanged(Widget widget) {
	    if (hasChanges()) {
            commit();
        }
		if (widget instanceof TabItem) {
			TabItem tabItem = (TabItem) widget;
			BuildPathBasePage newPage = (BuildPathBasePage) tabItem.getData();
			if (fCurrPage != null) {
				List selection = fCurrPage.getSelection();
				if (!selection.isEmpty()) {
					newPage.setSelection(selection, false);
				}
			}
			fCurrPage = newPage;
			fPageIndex = tabItem.getParent().getSelectionIndex();
		}
	}
	
	private boolean hasChanges() {
	    return (fAspectPathBlock != null && fAspectPathBlock.hasChangesInDialog()) || 
	            (fInPathBlock != null && fInPathBlock.hasChangesInDialog());
	}
	
	public boolean hasChangesInClasspathFile() {
        IFile file= thisProject.getFile(".classpath"); //$NON-NLS-1$
        return fFileTimeStamp != file.getModificationStamp();
    }
    
    public boolean isClassfileMissing() {
        return !thisProject.getFile(".classpath").exists(); //$NON-NLS-1$
    }
    
    public void initializeTimeStamps() {
        IFile file= thisProject.getFile(".classpath"); //$NON-NLS-1$
        fFileTimeStamp= file.getModificationStamp();
    }

	
//	public void setVisible(boolean visible) {
//	    if (visible) {
//	        // must reload
//	        resetPathBlocks();
//	    } else {
//	        // must commit
//	        if (hasChanges()) {
//	            commit();
//	        }
//	    }
//	    super.setVisible(visible);
//	}
}