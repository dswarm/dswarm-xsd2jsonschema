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
package org.dswarm.xsd2jsonschema.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import org.dswarm.xsd2jsonschema.JsonSchemaParser;
import org.dswarm.xsd2jsonschema.model.JSRoot;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class JsonSchemaParserTest {

	@Test
	public void testJsonSchemaParserDCElements() throws Exception {

		testJsonSchemaParserInternal("dc.xsd", "dcelements.jsonschema", "__ROOT_NODE__");
	}

	// doesn't work yet
	@Test
	public void testJsonSchemaParserDCTerms() throws Exception {

		testJsonSchemaParserInternal("dcterms.xsd", "dcterms.jsonschema", "__ROOT_NODE__");
	}

	@Test
	public void testJsonSchemaParserWMabxml() throws Exception {

		testJsonSchemaParserInternal("mabxml-1.xsd", "mabxml.jsonschema", "bla");
	}

	@Test
	public void testJsonSchemaParserWMarc21() throws Exception {

		testJsonSchemaParserInternal("MARC21slim.xsd", "marc21.jsonschema", "bla");
	}

	private void testJsonSchemaParserInternal(final String xsdSchemaFileName, final String resultFileName, final String rootNodeIdentifier) throws Exception {

		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		final JsonSchemaParser schemaParser = new JsonSchemaParser();

		final ByteSource byteSource = getByteSource(xsdSchemaFileName);

		schemaParser.parse(byteSource.openStream());
		final JSRoot root = schemaParser.apply(rootNodeIdentifier);

		final ObjectNode json = root.toJson(objectMapper);

		final URL expectedResourceURL = Resources.getResource(resultFileName);
		final String expectedJSONString = Resources.toString(expectedResourceURL, Charsets.UTF_8);

		final String actualJSONString = objectMapper.writeValueAsString(json);

		Assert.assertEquals(expectedJSONString, actualJSONString);
	}

	private ByteSource getByteSource(final String xsdSchemaFileName) {

		final URL resourceURL = Resources.getResource(xsdSchemaFileName);

		return Resources.asByteSource(resourceURL);
	}
}
