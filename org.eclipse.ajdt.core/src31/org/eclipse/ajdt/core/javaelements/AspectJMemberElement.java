/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Luzius Meisser - initial implementation
 *******************************************************************************/
package org.eclipse.ajdt.core.javaelements;

import java.util.List;

import org.aspectj.asm.IProgramElement.Accessibility;
import org.aspectj.asm.IProgramElement.ExtraInformation;
import org.aspectj.asm.IProgramElement.Kind;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.jdom.IDOMMethod;
import org.eclipse.jdt.core.jdom.IDOMNode;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.Member;
import org.eclipse.jdt.internal.core.NamedMember;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * Most code copied from org.eclipse.jdt.internal.core.SourceMethod
 * 
 * @author Luzius Meisser
 */
public class AspectJMemberElement extends NamedMember implements IMethod, IAspectJElement{


	public AspectJMemberElement(JavaElement parent, String name, String[] parameterTypes) {
		super(parent, name);
		if (parameterTypes == null) {
			fParameterTypes= fgEmptyList;
		} else {
			fParameterTypes= parameterTypes;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IJavaElement#getElementType()
	 */
	/**
	 * The parameter type signatures of the method - stored locally
	 * to perform equality test. <code>null</code> indicates no
	 * parameters.
	 */
	protected String[] fParameterTypes;

	/**
	 * An empty list of Strings
	 */
	protected static final String[] fgEmptyList= new String[] {};

public boolean equals(Object o) {
	if (!(o instanceof AspectJMemberElement)) return false;
	return super.equals(o) && Util.equalArraysOrNull(fParameterTypes, ((AspectJMemberElement)o).fParameterTypes);
}
/**
 * @see JavaElement#equalsDOMNode
 * @deprecated JDOM is obsolete
 */
// TODO - JDOM - remove once model ported off of JDOM
protected boolean equalsDOMNode(IDOMNode node) {
	if (node.getNodeType() == IDOMNode.METHOD) {
		try {
			IDOMMethod m = (IDOMMethod)node;
			if (isConstructor()) {
				return 
					(m.isConstructor() || m.getName().equals(this.getElementName()) /* case of a constructor that is being renamed */) 
						&& signatureEquals(m);
			} else {
				return super.equalsDOMNode(node) && signatureEquals(m);
			}
		} catch (JavaModelException e) {
			return false;
		}
	} else {
		return false;
	}

}
/**
 * @see IJavaElement
 */
public int getElementType() {
	return METHOD;
}
/**
 * @see IMethod
 */
public String[] getExceptionTypes() throws JavaModelException {
	AspectJMemberElementInfo info = (AspectJMemberElementInfo) getElementInfo();
	char[][] exs= info.getExceptionTypeNames();
	return convertTypeNamesToSigs(exs);
}

/* default */ static String[] convertTypeNamesToSigs(char[][] typeNames) {
	if (typeNames == null)
		return new String[0];
	int n = typeNames.length;
	if (n == 0)
		return new String[0];
	String[] typeSigs = new String[n];
	for (int i = 0; i < n; ++i) {
		typeSigs[i] = Signature.createTypeSignature(typeNames[i], false);
	}
	return typeSigs;
}
/**
 * @see JavaElement#getHandleMemento()
 */
public String getHandleMemento() {
	StringBuffer buff = new StringBuffer(((JavaElement) getParent()).getHandleMemento());
	char delimiter = getHandleMementoDelimiter();
	buff.append(delimiter);
	escapeMementoName(buff, getElementName());
	for (int i = 0; i < fParameterTypes.length; i++) {
		buff.append(delimiter);
		buff.append(fParameterTypes[i]);
	}
	if (this.occurrenceCount > 1) {
		buff.append(JEM_COUNT);
		buff.append(this.occurrenceCount);
	}
	return buff.toString();
}
/**
 * @see JavaElement#getHandleMemento()
 */
protected char getHandleMementoDelimiter() {
	return JavaElement.JEM_METHOD;
}
/**
 * @see IMethod
 */
public int getNumberOfParameters() {
	return fParameterTypes == null ? 0 : fParameterTypes.length;
}
/**
 * @see IMethod
 */
public String[] getParameterNames() throws JavaModelException {
	AspectJMemberElementInfo info = (AspectJMemberElementInfo) getElementInfo();
	char[][] names= info.getArgumentNames();
	if (names == null || names.length == 0) {
		return fgEmptyList;
	}
	String[] strings= new String[names.length];
	for (int i= 0; i < names.length; i++) {
		strings[i]= new String(names[i]);
	}
	return strings;
}
/**
 * @see IMethod
 */
public String[] getParameterTypes() {
	return fParameterTypes;
}

/**
 * @see IMethod#getTypeParameterSignatures()
 * @since 3.0
 */
public String[] getTypeParameterSignatures() throws JavaModelException {
	// TODO (jerome) - missing implementation
	return new String[0];
}

/*
 * @see JavaElement#getPrimaryElement(boolean)
 */
public IJavaElement getPrimaryElement(boolean checkOwner) {
	if (checkOwner) {
		CompilationUnit cu = (CompilationUnit)getAncestor(COMPILATION_UNIT);
		if (cu.isPrimary()) return this;
	}
	IJavaElement primaryParent = this.parent.getPrimaryElement(false);
	return ((IType)primaryParent).getMethod(this.name, fParameterTypes);
}
/**
 * @see IMethod
 */
public String getReturnType() throws JavaModelException {
	AspectJMemberElementInfo info = (AspectJMemberElementInfo) getElementInfo();
	return Signature.createTypeSignature(info.getReturnTypeName(), false);
}
/**
 * @see IMethod
 */
public String getSignature() throws JavaModelException {
	AspectJMemberElementInfo info = (AspectJMemberElementInfo) getElementInfo();
	return info.getSignature();
}
/**
 * @see org.eclipse.jdt.internal.core.JavaElement#hashCode()
 */
public int hashCode() {
   int hash = super.hashCode();
	for (int i = 0, length = fParameterTypes.length; i < length; i++) {
	    hash = Util.combineHashCodes(hash, fParameterTypes[i].hashCode());
	}
	return hash;
}
/**
 * @see IMethod
 */
public boolean isConstructor() throws JavaModelException {
	AspectJMemberElementInfo info = (AspectJMemberElementInfo) getElementInfo();
	return info.isConstructor();
}
/**
 * @see IMethod#isMainMethod()
 */
public boolean isMainMethod() throws JavaModelException {
	return this.isMainMethod(this);
}

/**
 * @see IMethod#isSimilar(IMethod)
 */
public boolean isSimilar(IMethod method) {
	return 
		this.areSimilarMethods(
			this.getElementName(), this.getParameterTypes(),
			method.getElementName(), method.getParameterTypes(),
			null);
}

/**
 * Returns <code>true</code> if the signature of this <code>SourceMethod</code> matches that of the given
 * <code>IDOMMethod</code>, otherwise <code>false</code>. 
 * @deprecated JDOM is obsolete
 */
// TODO - JDOM - remove once model ported off of JDOM
protected boolean signatureEquals(IDOMMethod method) {
	String[] otherTypes= method.getParameterTypes();
	String[] types= getParameterTypes();
	boolean ok= true;

	// ensure the number of parameters match
	if (otherTypes == null || otherTypes.length == 0) {
		ok= (types == null || types.length == 0);
	} else if (types != null) {
		ok= (otherTypes.length == types.length);
	} else {
		return false;
	}

	// ensure the parameter type signatures match
	if (ok) {
		if (types != null) {
			int i;
			for (i= 0; i < types.length; i++) {
				String otherType= Signature.createTypeSignature(otherTypes[i].toCharArray(), false);
				if (!types[i].equals(otherType)) {
					ok= false;
					break;
				}
			}
		}
	}

	return ok;
}
/**
 * @private Debugging purposes
 */
protected void toStringInfo(int tab, StringBuffer buffer, Object info) {
	buffer.append(this.tabString(tab));
	if (info == null) {
		toStringName(buffer);
		buffer.append(" (not open)"); //$NON-NLS-1$
	} else if (info == NO_INFO) {
		toStringName(buffer);
	} else {
		try {
			if (Flags.isStatic(this.getFlags())) {
				buffer.append("static "); //$NON-NLS-1$
			}
			if (!this.isConstructor()) {
				buffer.append(Signature.toString(this.getReturnType()));
				buffer.append(' ');
			}
			toStringName(buffer);
		} catch (JavaModelException e) {
			buffer.append("<JavaModelException in toString of " + getElementName()); //$NON-NLS-1$
		}
	}
}


public int getType() {
	return METHOD;
}
/* (non-Javadoc)
 * @see org.eclipse.ajdt.javamodel.javaelements.IAspectJElement#getKind()
 */
public Kind getAJKind() throws JavaModelException {
	IAspectJElementInfo info = (IAspectJElementInfo) getElementInfo();
	return info.getAJKind();
}
/* (non-Javadoc)
 * @see org.eclipse.ajdt.javamodel.javaelements.IAspectJElement#getAccessibility()
 */
public Accessibility getAJAccessibility() throws JavaModelException {
	IAspectJElementInfo info = (IAspectJElementInfo) getElementInfo();
	return info.getAJAccessibility();
}
/* (non-Javadoc)
 * @see org.eclipse.ajdt.javamodel.javaelements.IAspectJElement#getAJModifiers()
 */
public List getAJModifiers() throws JavaModelException {
	IAspectJElementInfo info = (IAspectJElementInfo) getElementInfo();
	return info.getAJModifiers();
}
/* (non-Javadoc)
 * @see org.eclipse.ajdt.javamodel.javaelements.IAspectJElement#getAJExtraInformation()
 */
public ExtraInformation getAJExtraInformation() throws JavaModelException {
	IAspectJElementInfo info = (IAspectJElementInfo) getElementInfo();
	return info.getAJExtraInfo();
}
/* (non-Javadoc)
 * @see org.eclipse.jdt.core.IMethod#getTypeParameters()
 */
public ITypeParameter[] getTypeParameters() throws JavaModelException {
	// TODO Auto-generated method stub
	return null;
}
/* (non-Javadoc)
 * @see org.eclipse.jdt.core.IMethod#getTypeParameter(java.lang.String)
 */
public ITypeParameter getTypeParameter(String name) {
	// TODO Auto-generated method stub
	return null;
}

}
