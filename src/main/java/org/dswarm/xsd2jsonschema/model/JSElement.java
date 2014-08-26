package org.dswarm.xsd2jsonschema.model;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;

public abstract class JSElement {

	private final String name;

	private String description;

	protected JSElement(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	protected abstract String getType();

	public abstract List<JSElement> getProperties();

	public abstract JSElement withName(final String newName);

	void render(final JsonGenerator jgen) throws IOException {

		jgen.writeObjectFieldStart(getName());

		jgen.writeStringField("type", getType());
		renderDescription(jgen);
		renderInternal(jgen);

		jgen.writeEndObject();
	}

	void renderDescription(final JsonGenerator jgen) throws IOException {
		if (getDescription() != null) {
			jgen.writeStringField("description", getDescription());
		}
	}

	void renderInternal(final JsonGenerator jgen) throws IOException {}
}
