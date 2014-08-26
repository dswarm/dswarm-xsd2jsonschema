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
	public void testJsonSchemaParser() throws Exception {

		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		final JsonSchemaParser schemaParser = new JsonSchemaParser();

		final URL resourceURL = Resources.getResource("mabxml-1.xsd");
		final ByteSource byteSource = Resources.asByteSource(resourceURL);

		schemaParser.parse(byteSource.openStream());
		final JSRoot root = schemaParser.apply("bla");

		final ObjectNode json = root.toJson(objectMapper);
		
		final URL expectedResourceURL = Resources.getResource("mabxml.jsonschema");
		final String expectedJSONString = Resources.toString(expectedResourceURL, Charsets.UTF_8);
		
		final String actualJSONString = objectMapper.writeValueAsString(json);
		
		Assert.assertEquals(expectedJSONString, actualJSONString);
	}
}
