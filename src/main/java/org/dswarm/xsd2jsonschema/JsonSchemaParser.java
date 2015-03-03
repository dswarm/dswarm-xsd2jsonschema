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
import java.util.List;
import java.util.Optional;

import com.sun.org.apache.xerces.internal.dom.DOMInputImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSImplementationImpl;
import com.sun.org.apache.xerces.internal.xs.XSAttributeDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSAttributeUse;
import com.sun.org.apache.xerces.internal.xs.XSComplexTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSElementDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSImplementation;
import com.sun.org.apache.xerces.internal.xs.XSLoader;
import com.sun.org.apache.xerces.internal.xs.XSModel;
import com.sun.org.apache.xerces.internal.xs.XSModelGroup;
import com.sun.org.apache.xerces.internal.xs.XSNamedMap;
import com.sun.org.apache.xerces.internal.xs.XSObject;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSParticle;
import com.sun.org.apache.xerces.internal.xs.XSSimpleTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSTerm;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSWildcard;
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

	private static final XSImplementation impl         = (XSImplementation) new XSImplementationImpl();
	private static final XSLoader         schemaLoader = impl.createXSLoader(null);

	private XSModel model = null;

	private List<JSElement> iterateParticle(final XSParticle particle) {

		final XSTerm term = particle.getTerm();

		if (term instanceof XSModelGroup) {

			final XSModelGroup modelGroup = (XSModelGroup) term;
			return iterateModelGroup(modelGroup);
		}

		// TODO: dunno how to replace this? -> we can cast it to XSModelGroup but maybe it should ba a XSModelGroupDefinition and has something to do with substition groups
		/*else if (term.isModelGroupDecl()) {

			final XSModelGroupDecl xsModelGroupDecl = term.asModelGroupDecl();
			return iterateModelGroup(xsModelGroupDecl.getModelGroup());
		}
		*/

		final ArrayList<JSElement> jsElements = new ArrayList<>(1);
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

			if (!optionalElement.isPresent()) {

				return Optional.empty();
			}

			return isRepeated(particle) ? Optional.of(new JSArray(optionalElement.get())) : Optional.ofNullable(optionalElement.get());
		}
		// TODO: dunno how to replace this? -> we can cast it to XSModelGroup but maybe it should ba a XSModelGroupDefinition and has something to do with substition groups
		/*else if (term.isModelGroupDecl()) {

			final XSModelGroupDecl xsModelGroupDecl = term.asModelGroupDecl();
			final String name = getDeclarationName(xsModelGroupDecl);

			final List<JSElement> elements = iterateModelGroup(xsModelGroupDecl.getModelGroup());

			return Optional.of(new JSObject(name, elements));

		}*/
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

		for (int i = 0; i < particles.getLength(); i++) {

			final XSParticle xsParticle = (XSParticle) particles.item(i);
			final XSTerm term = xsParticle.getTerm();
			if (term instanceof XSModelGroup) {

				list.addAll(iterateParticle(xsParticle));
			}
			// TODO: dunno how to replace this? -> we can cast it to XSModelGroup but maybe it should ba a XSModelGroupDefinition and has something to do with substition groups
			/*else if (term.isModelGroupDecl()) {

				list.addAll(iterateModelGroup(term.asModelGroupDecl().getModelGroup()));
			}*/

			else {

				final Optional<JSElement> optionalJSElement = iterateSingleParticle(xsParticle);

				if (optionalJSElement.isPresent()) {

					list.add(optionalJSElement.get());
				}
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

				return Optional.ofNullable(iterateSimpleType((XSSimpleTypeDefinition) xsElementDeclType).withName(elementName));

			} else if (XSTypeDefinition.COMPLEX_TYPE == xsElementDeclType.getTypeCategory()) {

				final XSComplexTypeDefinition xsComplexType = (XSComplexTypeDefinition) xsElementDeclType;
				final boolean isMixed = isMixed(xsComplexType);
				final XSSimpleTypeDefinition type = xsComplexType.getSimpleType();

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

						return Optional.ofNullable(jsElements);
					} else {

						return Optional.ofNullable(simpleJsElement);
					}

				}

				final JSObject jsElements = new JSObject(elementName, isMixed);

				final List<JSElement> elements = iterateComplexType(xsComplexType);

				if (elements.size() == 1 && elements.get(0) instanceof JSOther) {

					return Optional.ofNullable(elements.get(0).withName(jsElements.getName()));
				}

				jsElements.addAll(elements);

				return Optional.ofNullable(jsElements);
			}

			return Optional.of(new JSNull(elementName));
		} catch (final InternalError e) {

			e.printStackTrace();
		}

		return Optional.empty();
	}

	private List<JSElement> iterateComplexType(final XSComplexTypeDefinition complexType) {

		final ArrayList<JSElement> result = new ArrayList<>();

		final short contentType = complexType.getContentType();

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

	private void iterateComplexAttributes(final XSComplexTypeDefinition complexType, final List<JSElement> result) {

		final XSObjectList attributeUses = complexType.getAttributeUses();

		for (int i = 0; i < attributeUses.getLength(); i++) {
			final XSAttributeDeclaration attributeUseDecl = ((XSAttributeUse) attributeUses.item(i)).getAttrDeclaration();
			final XSSimpleTypeDefinition type = attributeUseDecl.getTypeDefinition();

			final String attributeName = getDeclarationName(attributeUseDecl, complexType);

			result.add(iterateSimpleType(type).withName(AT + attributeName));
		}
	}

	private JSElement iterateSimpleType(final XSSimpleTypeDefinition simpleType) {

		final String simpleTypeName = getDeclarationName(simpleType);

		return new JSString(simpleTypeName);
	}

	public void parse(final InputStream is) throws SAXException {

		final LSInput input = new DOMInputImpl();
		input.setByteStream(is);

		model = schemaLoader.load(input);
	}

	public void parse(final Reader reader) throws SAXException {
		//parser.parse(reader);
	}

	public void parse(final File schema) throws SAXException, IOException {
		//parser.parse(schema);
	}

	public void parse(final URL url) throws SAXException {
		//parser.parse(url);
	}

	public void parse(final String systemId) throws SAXException {
		//parser.parse(systemId);
	}

	public void parse(final InputSource source) throws SAXException {
		//parser.parse(source);
	}

	public JSRoot apply(final String rootName) throws SAXException {

		//final XSSchemaSet result = parser.getResult();

		//final Iterator<XSSchema> xsSchemaIterator = result.iterateSchema();

		final JSRoot root = new JSRoot(rootName);

		//while (xsSchemaIterator.hasNext()) {
		//final XSSchema xsSchema = xsSchemaIterator.next();

		final XSNamedMap elements = model.getComponents(XSConstants.ELEMENT_DECLARATION);

		for (int i = 0; i < elements.getLength(); i++) {

			XSObject object = elements.item(i);

			if (object instanceof XSElementDeclaration) {

				XSElementDeclaration elementDecl = (XSElementDeclaration) object;

				if (elementDecl.getAbstract()) {

					// skip abstract elements for now (however, we should treat them separately somehow)

					continue;
				}

				final Optional<JSElement> optionalElement = iterateElement(elementDecl);

				if (optionalElement.isPresent()) {

					root.add(optionalElement.get());
				}
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

	private String getDeclarationName(final XSObject decl) {

		final String targetNameSpace = decl.getNamespace();

		return getDeclarationNameWithNamespace(decl, targetNameSpace);
	}

	private String getDeclarationNameWithNamespace(final XSObject decl, final String targetNameSpace) {

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

	private String getDeclarationName(final XSObject decl, final XSObject alternativeDecl) {

		final String declName = getDeclarationName(decl);

		if (declName.startsWith(HTTP_PREFIX)) {

			return declName;
		}

		return getDeclarationNameWithNamespace(decl, alternativeDecl.getNamespace());
	}

	private boolean isMixed(final XSComplexTypeDefinition decl) {

		final short contentType = decl.getContentType();

		return XSComplexTypeDefinition.CONTENTTYPE_MIXED == contentType;
	}

	private boolean isRepeated(final XSParticle particle) {

		return particle.getMaxOccursUnbounded() || particle.getMaxOccurs() > 1;
	}
}

