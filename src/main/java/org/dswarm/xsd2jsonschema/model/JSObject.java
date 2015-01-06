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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;

import static com.google.common.base.Preconditions.checkNotNull;

public class JSObject extends JSElement implements Iterable<JSElement> {

	private final List<JSElement> list;
	private final boolean mixed;

	public JSObject(final String name, final List<JSElement> elements) {
		super(name);
		this.mixed = false;
		this.list = checkNotNull(elements);
	}

	public JSObject(final String name, final boolean mixed, final List<JSElement> elements) {
		super(name);
		this.mixed = mixed;
		this.list = checkNotNull(elements);
	}

	public JSObject(final String name) {
		super(name);
		this.mixed = false;
		this.list = new ArrayList<>();
	}

	public JSObject(final String name, final boolean mixed) {
		super(name);
		this.mixed = mixed;
		this.list = new ArrayList<>();
	}

	public JSObject add(final JSElement element) {
		this.list.add(element);

		return this;
	}

	public boolean addAll(final Collection<? extends JSElement> c) {
		return list.addAll(c);
	}

	@Override
	public Iterator<JSElement> iterator() {
		return list.iterator();
	}

	@Override
	public String getType() {
		return "object";
	}

	public boolean isMixed() {

		return mixed;
	}

	@Override
	public List<JSElement> getProperties() {
		return list;
	}

	@Override
	public JSElement withName(final String newName) {
		return new JSObject(newName, list);
	}

	@Override
	protected void renderInternal(final JsonGenerator jgen) throws IOException {
		final List<JSElement> properties = getProperties();

		jgen.writeBooleanField("mixed", isMixed());
		jgen.writeObjectFieldStart("properties");

		for (final JSElement property : properties) {

			property.render(jgen);
		}

		jgen.writeEndObject();
	}
}
