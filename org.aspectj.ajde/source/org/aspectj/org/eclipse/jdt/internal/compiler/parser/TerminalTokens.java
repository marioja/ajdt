/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.aspectj.org.eclipse.jdt.internal.compiler.parser;

/**
 * IMPORTANT NOTE: These constants are dedicated to the internal Scanner implementation.
 * It is mirrored in org.aspectj.org.eclipse.jdt.core.compiler public package where it is API.
 * The mirror implementation is using the backward compatible ITerminalSymbols constant
 * definitions (stable with 2.0), whereas the internal implementation uses TerminalTokens
 * which constant values reflect the latest parser generation state.
 */
/**
 * Maps each terminal symbol in the java-grammar into a unique integer.
 * This integer is used to represent the terminal when computing a parsing action.
 *
 * Disclaimer : These constant values are generated automatically using a Java
 * grammar, therefore their actual values are subject to change if new keywords
 * were added to the language (for instance, 'assert' is a keyword in 1.4).
 */
public interface TerminalTokens {

	// special tokens not part of grammar - not autogenerated
	int TokenNameNotAToken = 0,
		TokenNameWHITESPACE = 1000,
		TokenNameCOMMENT_LINE = 1001,
		TokenNameCOMMENT_BLOCK = 1002,
		TokenNameCOMMENT_JAVADOC = 1003;

	public final static int
    TokenNameIdentifier = 14,
    TokenNameabstract = 56,
    TokenNameassert = 81,
    TokenNameboolean = 114,
    TokenNamebreak = 82,
    TokenNamebyte = 115,
    TokenNamecase = 92,
    TokenNamecatch = 93,
    TokenNamechar = 116,
    TokenNameclass = 72,
    TokenNamecontinue = 83,
    TokenNameconst = 134,
    TokenNamedefault = 98,
    TokenNamedo = 84,
    TokenNamedouble = 117,
    TokenNameelse = 97,
    TokenNameenum = 85,
    TokenNameextends = 91,
    TokenNamefalse = 47,
    TokenNamefinal = 57,
    TokenNamefinally = 96,
    TokenNamefloat = 118,
    TokenNamefor = 86,
    TokenNamegoto = 135,
    TokenNameif = 87,
    TokenNameimplements = 101,
    TokenNameimport = 94,
    TokenNameinstanceof = 16,
    TokenNameint = 119,
    TokenNameopens = 123,
    TokenNameinterface = 76,
    TokenNamelong = 120,
    TokenNamenative = 58,
    TokenNamenew = 41,
    TokenNamenull = 48,
    TokenNamepackage = 90,
    TokenNameprivate = 59,
    TokenNameprotected = 60,
    TokenNamepublic = 61,
    TokenNamereturn = 88,
    TokenNameshort = 121,
    TokenNamestatic = 45,
    TokenNamestrictfp = 62,
    TokenNamesuper = 43,
    TokenNameswitch = 66,
    TokenNamesynchronized = 46,
    TokenNamethis = 44,
    TokenNamethrow = 79,
    TokenNamethrows = 99,
    TokenNametransient = 63,
    TokenNametrue = 49,
    TokenNametry = 89,
    TokenNamevoid = 122,
    TokenNamevolatile = 64,
    TokenNamewhile = 80,
    TokenNamemodule = 124,
    TokenNameopen = 125,
    TokenNamerequires = 126,
    TokenNametransitive = 130,
    TokenNameexports = 127,
    TokenNameto = 131,
    TokenNameuses = 128,
    TokenNameprovides = 129,
    TokenNamewith = 132,
    TokenNameaspect = 27,
    TokenNamepointcut = 29,
    TokenNamearound = 34,
    TokenNamebefore = 30,
    TokenNameafter = 31,
    TokenNamedeclare = 32,
    TokenNameprivileged = 28,
    TokenNameIntegerLiteral = 50,
    TokenNameLongLiteral = 51,
    TokenNameFloatingPointLiteral = 52,
    TokenNameDoubleLiteral = 53,
    TokenNameCharacterLiteral = 54,
    TokenNameStringLiteral = 55,
    TokenNamePLUS_PLUS = 3,
    TokenNameMINUS_MINUS = 4,
    TokenNameEQUAL_EQUAL = 23,
    TokenNameLESS_EQUAL = 18,
    TokenNameGREATER_EQUAL = 19,
    TokenNameNOT_EQUAL = 20,
    TokenNameLEFT_SHIFT = 21,
    TokenNameRIGHT_SHIFT = 12,
    TokenNameUNSIGNED_RIGHT_SHIFT = 15,
    TokenNamePLUS_EQUAL = 102,
    TokenNameMINUS_EQUAL = 103,
    TokenNameMULTIPLY_EQUAL = 104,
    TokenNameDIVIDE_EQUAL = 105,
    TokenNameAND_EQUAL = 106,
    TokenNameOR_EQUAL = 107,
    TokenNameXOR_EQUAL = 108,
    TokenNameREMAINDER_EQUAL = 109,
    TokenNameLEFT_SHIFT_EQUAL = 110,
    TokenNameRIGHT_SHIFT_EQUAL = 111,
    TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL = 112,
    TokenNameOR_OR = 38,
    TokenNameAND_AND = 37,
    TokenNamePLUS = 2,
    TokenNameMINUS = 6,
    TokenNameNOT = 69,
    TokenNameREMAINDER = 10,
    TokenNameXOR = 33,
    TokenNameAND = 22,
    TokenNameMULTIPLY = 8,
    TokenNameOR = 36,
    TokenNameTWIDDLE = 73,
    TokenNameDIVIDE = 11,
    TokenNameGREATER = 13,
    TokenNameLESS = 7,
    TokenNameLPAREN = 17,
    TokenNameRPAREN = 25,
    TokenNameLBRACE = 65,
    TokenNameRBRACE = 40,
    TokenNameLBRACKET = 5,
    TokenNameRBRACKET = 70,
    TokenNameSEMICOLON = 26,
    TokenNameQUESTION = 35,
    TokenNameCOLON = 67,
    TokenNameCOMMA = 39,
    TokenNameDOT = 1,
    TokenNameEQUAL = 75,
    TokenNameAT = 42,
    TokenNameELLIPSIS = 100,
    TokenNameARROW = 113,
    TokenNameCOLON_COLON = 9,
    TokenNameBeginLambda = 68,
    TokenNameBeginIntersectionCast = 74,
    TokenNameBeginTypeArguments = 95,
    TokenNameElidedSemicolonAndRightBrace = 77,
    TokenNameAT308 = 24,
    TokenNameAT308DOTDOTDOT = 133,
    TokenNameBeginCaseExpr = 78,
    TokenNameEOF = 71,
    TokenNameERROR = 136;
}
