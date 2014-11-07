/**
 * Copyright (C) 2013, 2014 SLUB Dresden & Avantgarde Labs GmbH (<code@dswarm.org>)
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
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;

public class JSOther extends JSElement {

	private final String nameSpace;

	public JSOther(final String name, final String nameSpace) {
		super(name);
		this.nameSpace = nameSpace;
	}

	@Override
	public String getType() {
		return "other";
	}

	@Override
	public List<JSElement> getProperties() {
		return null;
	}

	@Override
	public JSElement withName(final String newName) {
		return new JSOther(newName, nameSpace);
	}

	public String getNameSpace() {
		return nameSpace;
	}

	@Override
	protected void renderInternal(final JsonGenerator jgen) throws IOException {
		jgen.writeStringField("namespace", getNameSpace());
	}
}
