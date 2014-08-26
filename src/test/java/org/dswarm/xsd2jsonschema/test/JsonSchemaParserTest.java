package org.dswarm.xsd2jsonschema.test;

import java.io.InputStream;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;

import org.dswarm.xsd2jsonschema.JsonSchemaParser;
import org.dswarm.xsd2jsonschema.model.JSRoot;

public class JsonSchemaParserTest {

	@Test
	public void testJsonSchemaParser() throws Exception {

		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		final JsonSchemaParser schemaParser = new JsonSchemaParser();

		final URL resourceURL = Resources.getResource("mabxml-1.xsd");
		InputSupplier<InputStream> bla = Resources.newInputStreamSupplier(resourceURL);

		schemaParser.parse(bla.getInput());
		final JSRoot root = schemaParser.apply("bla");

		final ObjectNode json = root.toJson(objectMapper);
		
		final URL expectedResourceURL = Resources.getResource("mabxml.jsonschema");
		final String expectedJSONString = Resources.toString(expectedResourceURL, Charsets.UTF_8);
		
		final String actualJSONString = objectMapper.writeValueAsString(json);
		
		Assert.assertEquals(expectedJSONString, actualJSONString);
	}
}
