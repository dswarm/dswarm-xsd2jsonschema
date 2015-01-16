/**
 * Copyright (C) 2013 – 2015 SLUB Dresden & Avantgarde Labs GmbH (<code@dswarm.org>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dswarm.xsd2jsonschema;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSDeclaration;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.XSWildcard;
import com.sun.xml.xsom.parser.XSOMParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.dswarm.xsd2jsonschema.model.JSArray;
import org.dswarm.xsd2jsonschema.model.JSElement;
import org.dswarm.xsd2jsonschema.model.JSNull;
import org.dswarm.xsd2jsonschema.model.JSObject;
import org.dswarm.xsd2jsonschema.model.JSOther;
import org.dswarm.xsd2jsonschema.model.JSRoot;
import org.dswarm.xsd2jsonschema.model.JSString;

public class JsonSchemaParser {

	private final XSOMParser parser = new XSOMParser();

	private List<JSElement> iterateParticle(final XSParticle particle) {

		final XSTerm term = particle.getTerm();

		if (term.isModelGroup()) {

			final XSModelGroup modelGroup = term.asModelGroup();
			return iterateModelGroup(modelGroup);
		} else if (term.isModelGroupDecl()) {

			final XSModelGroupDecl xsModelGroupDecl = term.asModelGroupDecl();
			return iterateModelGroup(xsModelGroupDecl.getModelGroup());
		}

		final ArrayList<JSElement> jsElements = new ArrayList<>(1);
		jsElements.add(iterateSingleParticle(particle));

		return jsElements;
	}

	private JSElement iterateSingleParticle(final XSParticle particle) {

		final XSTerm term = particle.getTerm();
		if (term.isElementDecl()) {

			final XSElementDecl xsElementDecl = term.asElementDecl();

			final JSElement element = iterateElement(xsElementDecl);

			return particle.isRepeated() ? new JSArray(element) : element;
		} else if (term.isModelGroupDecl()) {

			final XSModelGroupDecl xsModelGroupDecl = term.asModelGroupDecl();
			final String name = getDeclarationName(xsModelGroupDecl);

			final List<JSElement> elements = iterateModelGroup(xsModelGroupDecl.getModelGroup());

			return new JSObject(name, elements);

		} else if (term.isWildcard() && term instanceof XSWildcard.Other) {

			final XSWildcard.Other xsWildcardOther = (XSWildcard.Other) term;

			return new JSOther("wildcard", xsWildcardOther.getOtherNamespace());
		}

		return new JSNull("null");
	}

	private List<JSElement> iterateModelGroup(final XSModelGroup modelGroup) {

		final List<JSElement> list = new ArrayList<>();

		for (final XSParticle xsParticle : modelGroup) {

			final XSTerm term = xsParticle.getTerm();
			if (term.isModelGroup()) {

				list.addAll(iterateParticle(xsParticle));
			} else if (term.isModelGroupDecl()) {

				list.addAll(iterateModelGroup(term.asModelGroupDecl().getModelGroup()));
			} else {

				list.add(iterateSingleParticle(xsParticle));
			}
		}

		return list;
	}

	private JSElement iterateElement(final XSElementDecl elementDecl) {

		final XSType xsElementDeclType = elementDecl.getType();

		final String elementName = getDeclarationName(elementDecl);

		if (xsElementDeclType.isSimpleType()) {

			return iterateSimpleType(xsElementDeclType.asSimpleType()).withName(elementName);

		} else if (xsElementDeclType.isComplexType()) {

			final XSComplexType xsComplexType = xsElementDeclType.asComplexType();
			final boolean isMixed = xsComplexType.isMixed();
			final XSSimpleType type = xsComplexType.getContentType().asSimpleType();

			if (type != null) {
				final JSElement simpleJsElement = iterateSimpleType(type).withName(elementName);

				final int numAttributes = xsComplexType.getAttributeUses().size();
				if (numAttributes > 0) {
					final List<JSElement> elements = new ArrayList<>(numAttributes);
					final JSObject jsElements;

					// to avoid doubling of attribute in attribute path
					if (!elementName.equals(simpleJsElement.getName())) {

						jsElements = new JSObject(elementName, isMixed);

						jsElements.add(simpleJsElement);
					} else {

						jsElements = new JSObject(elementName, true);
					}

					iterateComplexAttributes(xsComplexType, elements);

					jsElements.addAll(elements);

					return jsElements;
				} else {

					return simpleJsElement;
				}

			}

			final JSObject jsElements = new JSObject(elementName, isMixed);

			final List<JSElement> elements = iterateComplexType(xsComplexType);

			if (elements.size() == 1 && elements.get(0) instanceof JSOther) {

				return elements.get(0).withName(jsElements.getName());
			}

			jsElements.addAll(elements);

			return jsElements;
		}

		return new JSNull(elementName);
	}

	private List<JSElement> iterateComplexType(final XSComplexType complexType) {

		final ArrayList<JSElement> result = new ArrayList<>();

		final XSContentType contentType = complexType.getContentType();

		final XSParticle xsParticle = contentType.asParticle();
		if (xsParticle != null) {

			result.addAll(iterateParticle(xsParticle));
		} else {
			final XSSimpleType xsSimpleType = contentType.asSimpleType();
			if (xsSimpleType != null) {

				result.add(iterateSimpleType(xsSimpleType));
			}
		}

		iterateComplexAttributes(complexType, result);

		return result;
	}

	private void iterateComplexAttributes(final XSComplexType complexType, final List<JSElement> result) {

		final Collection<? extends XSAttributeUse> attributeUses = complexType.getAttributeUses();

		for (final XSAttributeUse attributeUse : attributeUses) {
			final XSAttributeDecl attributeUseDecl = attributeUse.getDecl();
			final XSSimpleType type = attributeUseDecl.getType();

			final String attributeName = getDeclarationName(attributeUseDecl, complexType);

			result.add(iterateSimpleType(type).withName("@" + attributeName));
		}
	}

	private JSElement iterateSimpleType(final XSSimpleType simpleType) {

		final String simpleTypeName = getDeclarationName(simpleType);

		return new JSString(simpleTypeName);
	}

	public void parse(final InputStream is) throws SAXException {
		parser.parse(is);
	}

	public void parse(final Reader reader) throws SAXException {
		parser.parse(reader);
	}

	public void parse(final File schema) throws SAXException, IOException {
		parser.parse(schema);
	}

	public void parse(final URL url) throws SAXException {
		parser.parse(url);
	}

	public void parse(final String systemId) throws SAXException {
		parser.parse(systemId);
	}

	public void parse(final InputSource source) throws SAXException {
		parser.parse(source);
	}

	public JSRoot apply(final String rootName) throws SAXException {

		final XSSchemaSet result = parser.getResult();

		final Iterator<XSSchema> xsSchemaIterator = result.iterateSchema();

		final JSRoot root = new JSRoot(rootName);

		while (xsSchemaIterator.hasNext()) {
			final XSSchema xsSchema = xsSchemaIterator.next();
			final Iterator<XSElementDecl> xsElementDeclIterator = xsSchema.iterateElementDecls();

			while (xsElementDeclIterator.hasNext()) {

				final XSElementDecl elementDecl = xsElementDeclIterator.next();
				final JSElement element = iterateElement(elementDecl);

				root.add(element);

			}
		}

		return root;
	}

	private String getDeclarationName(final XSDeclaration decl) {

		final String targetNameSpace = decl.getTargetNamespace();

		return getDeclarationNameWithNamespace(decl, targetNameSpace);
	}

	private String getDeclarationNameWithNamespace(final XSDeclaration decl, final String targetNameSpace) {
		final String declName;

		if (targetNameSpace != null && !targetNameSpace.trim().isEmpty()) {

			declName = targetNameSpace + "#" + decl.getName();
		} else {

			declName = decl.getName();
		}

		return declName;
	}

	private String getDeclarationName(final XSDeclaration decl, final XSDeclaration alternativeDecl) {

		final String declName = getDeclarationName(decl);

		if (declName.startsWith("http://")) {

			return declName;
		}

		return getDeclarationNameWithNamespace(decl, alternativeDecl.getTargetNamespace());
	}
}

