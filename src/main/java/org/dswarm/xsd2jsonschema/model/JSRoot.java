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
package org.dswarm.xsd2jsonschema.model;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JSRoot extends JSObject {

	public JSRoot(final String name) {
		super(name);
	}

	@Override
	public JSElement withName(final String newName) {
		final JSRoot jsRoot = new JSRoot(newName);

		for (final JSElement jsElement : this) {
			jsRoot.add(jsElement);
		}

		return jsRoot;
	}

	@Override
	public void render(final JsonGenerator jgen) throws IOException {
		jgen.writeStartObject();

		jgen.writeStringField("title", getName());
		jgen.writeStringField("type", getType());

		renderDescription(jgen);

		renderInternal(jgen);

		jgen.writeEndObject();

		jgen.flush();
		jgen.close();
	}

	public ObjectNode toJson(final ObjectMapper mapper) throws IOException {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		render(mapper, outputStream);

		final ObjectNode jsonNode = mapper.readValue(outputStream.toByteArray(), ObjectNode.class);
		outputStream.close();

		return jsonNode;
	}

	public void render(final ObjectMapper mapper, final OutputStream out) throws IOException {
		render(mapper.getFactory(), out);

	}
	public void render(final ObjectMapper mapper, final OutputStream out, final JsonEncoding encoding) throws IOException {
		render(mapper.getFactory(), out, encoding);

	}
	public void render(final ObjectMapper mapper, final Writer writer) throws IOException {
		render(mapper.getFactory(), writer);

	}
	public void render(final ObjectMapper mapper, final File file, final JsonEncoding encoding) throws IOException {
		render(mapper.getFactory(), file, encoding);
	}

	public void render(final JsonFactory jsonFactory, final OutputStream out) throws IOException {
		render(jsonFactory.createGenerator(out));

	}
	public void render(final JsonFactory jsonFactory, final OutputStream out, final JsonEncoding encoding) throws IOException {
		render(jsonFactory.createGenerator(out, encoding));

	}
	public void render(final JsonFactory jsonFactory, final Writer writer) throws IOException {
		render(jsonFactory.createGenerator(writer));

	}
	public void render(final JsonFactory jsonFactory, final File file, final JsonEncoding encoding) throws IOException {
		render(jsonFactory.createGenerator(file, encoding));
	}

	public String render() throws IOException {
		final JsonFactory jsonFactory = new JsonFactory();
		final StringWriter writer = new StringWriter();

		final JsonGenerator generator = jsonFactory.createGenerator(writer);
		generator.setPrettyPrinter(new DefaultPrettyPrinter());

		render(generator);

		return writer.getBuffer().toString();
	}
}
