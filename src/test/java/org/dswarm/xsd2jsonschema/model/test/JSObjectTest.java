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
package org.dswarm.xsd2jsonschema.model.test;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import org.dswarm.xsd2jsonschema.model.JSElement;
import org.dswarm.xsd2jsonschema.model.JSObject;

public class JSObjectTest extends BaseJSTest<JSObject> {

	public JSObjectTest() {
		super(JSObject.class);
	}

	@Override
	public void setUp() throws Exception {

		final List<JSElement> jsElements = new ArrayList<JSElement>(2);

		// TODO

		obj = new JSObject("foo", jsElements);
	}

	@Test
	public void testAdd() throws Exception {
		// TODO

	}

	@Test
	public void testAddAll() throws Exception {
		// TODO

	}

	@Test
	public void testIterator() throws Exception {
		// TODO

	}

	@Test
	public void testGetType() throws Exception {

		MatcherAssert.assertThat(obj.getType(), Matchers.equalTo("object"));
	}

	@Test
	public void testGetProperties() throws Exception {
		// TODO

	}

	@Test
	public void testRenderInternal() throws Exception {
		// TODO

	}
}
