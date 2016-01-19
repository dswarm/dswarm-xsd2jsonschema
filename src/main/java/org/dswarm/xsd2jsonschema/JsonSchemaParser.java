/**
 * Copyright (C) 2013 – 2016 SLUB Dresden & Avantgarde Labs GmbH (<code@dswarm.org>)
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.sun.org.apache.xerces.internal.dom.DOMInputImpl;
import com.sun.org.apache.xerces.internal.xs.XSAttributeDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSAttributeUse;
import com.sun.org.apache.xerces.internal.xs.XSComplexTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSElementDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSImplementation;
import com.sun.org.apache.xerces.internal.xs.XSLoader;
import com.sun.org.apache.xerces.internal.xs.XSModel;
import com.sun.org.apache.xerces.internal.xs.XSModelGroup;
import com.sun.org.apache.xerces.internal.xs.XSModelGroupDefinition;
import com.sun.org.apache.xerces.internal.xs.XSNamedMap;
import com.sun.org.apache.xerces.internal.xs.XSObject;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSParticle;
import com.sun.org.apache.xerces.internal.xs.XSSimpleTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSTerm;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSWildcard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.LSInput;
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

	private static final String HASH        = "#";
	private static final String HTTP_PREFIX = "http://";
	private static final String SLASH       = "/";
	private static final String AT          = "@";
	private static final String WILDCARD    = "wildcard";
	private static final String NULL        = "null";

	private static final Logger LOG = LoggerFactory.getLogger(JsonSchemaParser.class);

	//private static final XSImplementation impl         = (XSImplementation) new XSImplementationImpl();

	private static final XSLoader LOADER;

	static {
		System.setProperty(DOMImplementationRegistry.PROPERTY, "com.sun.org.apache.xerces.internal.dom.DOMXSImplementationSourceImpl");

		XSLoader loader;
		try {
			final DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
			final XSImplementation impl = (XSImplementation) registry.getDOMImplementation("XS-Loader");
			loader = impl.createXSLoader(null);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			LOG.error("Could not initialise schema parser", e);
			loader = null;
		}
		LOADER = loader;
	}

	private XSModel model;

	private Collection<JSElement> iterateParticle(final XSParticle particle) {

		final XSTerm term = particle.getTerm();

		if (term instanceof XSModelGroup) {

			final XSModelGroup modelGroup = (XSModelGroup) term;
			return iterateModelGroup(modelGroup);
		}
		else if (term instanceof XSModelGroupDefinition) {
			final XSModelGroupDefinition xsModelGroupDefinition = (XSModelGroupDefinition) term;
			return iterateModelGroup(xsModelGroupDefinition.getModelGroup());
		}

		final Collection<JSElement> jsElements = new ArrayList<>(1);
		final Optional<JSElement> optionalJSElement = iterateSingleParticle(particle);

		if (optionalJSElement.isPresent()) {

			jsElements.add(optionalJSElement.get());
		}

		return jsElements;
	}

	private Optional<JSElement> iterateSingleParticle(final XSParticle particle) {

		final XSTerm term = particle.getTerm();
		if (term instanceof XSElementDeclaration) {

			final XSElementDeclaration xsElementDecl = (XSElementDeclaration) term;

			final Optional<JSElement> optionalElement = iterateElement(xsElementDecl);

			return optionalElement.map(el -> isRepeated(particle) ? new JSArray(el) : el);
		}

		else if (term instanceof XSModelGroupDefinition) {

			final XSModelGroupDefinition xsModelGroupDefinition = (XSModelGroupDefinition) term;
			final String name = getDeclarationName(xsModelGroupDefinition);

			final List<JSElement> elements = iterateModelGroup(xsModelGroupDefinition.getModelGroup());
			return Optional.of(new JSObject(name, elements));
		}

		else if (term instanceof XSWildcard) {

			final XSWildcard wildcard = (XSWildcard) term;

			// TODO: what should we do with other XS wildcard types, i.e., 'any' and 'union'

			// is this XSOM XSWildCard.Other ??
			if (XSWildcard.NSCONSTRAINT_NOT == wildcard.getConstraintType()) {

				// wo do not have "other namespace" available here
				return Optional.of(new JSOther(WILDCARD, null));
			} else if (XSWildcard.NSCONSTRAINT_ANY == wildcard.getConstraintType()) {

				// TODO: shall we do something else here??

				return Optional.empty();
			}
		}

		return Optional.of(new JSNull(NULL));
	}

	private List<JSElement> iterateModelGroup(final XSModelGroup modelGroup) {

		final List<JSElement> list = new ArrayList<>();

		final XSObjectList particles = modelGroup.getParticles();

		for (int i = 0, l = particles.getLength(); i < l; i++) {

			final XSParticle xsParticle = (XSParticle) particles.item(i);
			final XSTerm term = xsParticle.getTerm();
			if (term instanceof XSModelGroup) {

				list.addAll(iterateParticle(xsParticle));
			}

			else if (term instanceof XSModelGroupDefinition) {
				final XSModelGroupDefinition xsModelGroupDefinition = (XSModelGroupDefinition) term;
				list.addAll(iterateModelGroup(xsModelGroupDefinition.getModelGroup()));
			}

			else {

				final Optional<JSElement> optionalJSElement = iterateSingleParticle(xsParticle);
				optionalJSElement.ifPresent(list::add);
			}
		}

		return list;
	}

	private Optional<JSElement> iterateElement(final XSElementDeclaration elementDecl) {

		try {

			final String elementName = getDeclarationName(elementDecl);

			//System.out.println(elementName);

			final XSTypeDefinition xsElementDeclType = elementDecl.getTypeDefinition();

			if (XSTypeDefinition.SIMPLE_TYPE == xsElementDeclType.getTypeCategory()) {

				return Optional.of(iterateSimpleType(xsElementDeclType).withName(elementName));

			} else if (XSTypeDefinition.COMPLEX_TYPE == xsElementDeclType.getTypeCategory()) {

				final XSComplexTypeDefinition xsComplexType = (XSComplexTypeDefinition) xsElementDeclType;
				final boolean isMixed = isMixed(xsComplexType);

				final JSElement element = Optional.ofNullable(xsComplexType.getSimpleType()).map(type -> {

					final JSElement simpleJsElement = iterateSimpleType(type).withName(elementName);

					final int numAttributes = xsComplexType.getAttributeUses().size();
					if (numAttributes <= 0) {
						return simpleJsElement;
					}

					final Collection<JSElement> elements = new ArrayList<>(numAttributes);
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
				}).orElseGet(() -> {
					final JSObject jsElements = new JSObject(elementName, isMixed);

					final List<JSElement> elements = iterateComplexType(xsComplexType);

					if (elements.size() == 1 && elements.get(0) instanceof JSOther) {

						return elements.get(0).withName(jsElements.getName());
					}

					jsElements.addAll(elements);

					return jsElements;
				});

				return Optional.of(element);
			}

			return Optional.of(new JSNull(elementName));
		} catch (final InternalError e) {
			LOG.warn("Could not traverse this element", e);
		}

		return Optional.empty();
	}

	private List<JSElement> iterateComplexType(final XSComplexTypeDefinition complexType) {

		final List<JSElement> result = new ArrayList<>();

		final XSParticle xsParticle = complexType.getParticle();
		if (xsParticle != null) {

			result.addAll(iterateParticle(xsParticle));
		} else {
			final XSSimpleTypeDefinition xsSimpleType = complexType.getSimpleType();
			if (xsSimpleType != null) {

				result.add(iterateSimpleType(xsSimpleType));
			}
		}

		iterateComplexAttributes(complexType, result);

		return result;
	}

	private static void iterateComplexAttributes(final XSComplexTypeDefinition complexType, final Collection<JSElement> result) {

		final XSObjectList attributeUses = complexType.getAttributeUses();

		for (int i = 0; i < attributeUses.getLength(); i++) {
			final XSAttributeDeclaration attributeUseDecl = ((XSAttributeUse) attributeUses.item(i)).getAttrDeclaration();
			final XSSimpleTypeDefinition type = attributeUseDecl.getTypeDefinition();

			final String attributeName = getDeclarationName(attributeUseDecl, complexType);

			result.add(iterateSimpleType(type).withName(AT + attributeName));
		}
	}

	private static JSElement iterateSimpleType(final XSObject simpleType) {

		final String simpleTypeName = getDeclarationName(simpleType);

		return new JSString(simpleTypeName);
	}

	public void parse(final InputStream is) throws SAXException {
		Preconditions.checkNotNull(LOADER, "The parser could not be initialised");

		final LSInput input = new DOMInputImpl();
		input.setByteStream(is);

		model = LOADER.load(input);
	}

	public void parse(final Reader reader) throws SAXException {
		Preconditions.checkNotNull(LOADER, "The parser could not be initialised");

		final LSInput input = new DOMInputImpl();
		input.setCharacterStream(reader);

		model = LOADER.load(input);
	}

	public void parse(final File schema) throws SAXException, IOException {
		parse(new FileInputStream(schema));
	}

	public void parse(final URL url) throws SAXException, IOException {
		parse(url.openStream());
	}

	public void parse(final String schema) throws SAXException {
		parse(new StringReader(schema));
	}

	public void parse(final InputSource source) throws SAXException {
		parse(source.getByteStream());
	}

	public JSRoot apply(final String rootName) throws SAXException {

		Preconditions.checkState(model != null, "You have to call parse() first");

		//final XSSchemaSet result = parser.getResult();

		//final Iterator<XSSchema> xsSchemaIterator = result.iterateSchema();

		final JSRoot root = new JSRoot(rootName);

		//while (xsSchemaIterator.hasNext()) {
		//final XSSchema xsSchema = xsSchemaIterator.next();

		final XSNamedMap elements = model.getComponents(XSConstants.ELEMENT_DECLARATION);

		for (int i = 0; i < elements.getLength(); i++) {

			final XSObject object = elements.item(i);

			if (object instanceof XSElementDeclaration) {

				final XSElementDeclaration elementDecl = (XSElementDeclaration) object;

				if (elementDecl.getAbstract()) {

					// skip abstract elements for now (however, we should treat them separately somehow)

					continue;
				}

				iterateElement(elementDecl).ifPresent(root::add);
			}
		}
		//
		//		final Set<SchemaDocument> docs = parser.getDocuments();
		//
		//		XSSchema xsSchema = null;
		//
		//		for (final SchemaDocument doc : docs) {
		//
		//			if ("http://purl.org/dc/terms/".equals(doc.getSchema().getTargetNamespace())) {
		//
		//				xsSchema = doc.getSchema();
		//
		//				break;
		//			}
		//		}
		//
		//		final Iterator<XSElementDecl> xsElementDeclIterator = xsSchema.iterateElementDecls();
		//		//final Map<String, XSElementDecl> xsElementDeclMap = xsSchema.getElementDecls();
		//
		//		while (xsElementDeclIterator.hasNext()) {
		//			//for(final XSElementDecl elementDecl : xsElementDeclMap.values()) {
		//
		//			final XSElementDecl elementDecl = xsElementDeclIterator.next();
		//
		//			if (elementDecl.isAbstract()) {
		//
		//				// skip abstract elements for now (however, we should treat them separately somehow)
		//
		//				continue;
		//			}
		//
		//			final Optional<JSElement> optionalElement = iterateElement(elementDecl);
		//
		//			if (optionalElement.isPresent()) {
		//
		//				root.add(optionalElement.get());
		//			}

		//}
		//}
		//}

		return root;
	}

	private static String getDeclarationName(final XSObject decl) {

		final String targetNameSpace = decl.getNamespace();

		return getDeclarationNameWithNamespace(decl, targetNameSpace);
	}

	private static String getDeclarationNameWithNamespace(final XSObject decl, final String targetNameSpace) {

		final String declName;

		if (targetNameSpace != null && !targetNameSpace.trim().isEmpty()) {

			if (targetNameSpace.endsWith(SLASH)) {

				declName = targetNameSpace + decl.getName();
			} else {

				declName = targetNameSpace + HASH + decl.getName();
			}
		} else {

			declName = decl.getName();
		}

		return declName;
	}

	private static String getDeclarationName(final XSObject decl, final XSObject alternativeDecl) {

		final String declName = getDeclarationName(decl);

		if (declName.startsWith(HTTP_PREFIX)) {

			return declName;
		}

		return getDeclarationNameWithNamespace(decl, alternativeDecl.getNamespace());
	}

	private static boolean isMixed(final XSComplexTypeDefinition decl) {

		final short contentType = decl.getContentType();

		return XSComplexTypeDefinition.CONTENTTYPE_MIXED == contentType;
	}

	private static boolean isRepeated(final XSParticle particle) {

		return particle.getMaxOccursUnbounded() || particle.getMaxOccurs() > 1;
	}
}

