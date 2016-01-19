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
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import org.dswarm.xsd2jsonschema.model.JSElement;

public class JSElementTest {

	private TestJS			js;

	private final String	expectedName	= "foo";
	private final String	expectedType	= "test";

	private class TestJS extends JSElement {

		private TestJS() {
			super(expectedName);
		}

		private TestJS(final String name) {
			super(name);
		}

		@Override
		public String getType() {
			return expectedType;
		}

		@Override
		public List<JSElement> getProperties() {
			final ArrayList<JSElement> jsElements = new ArrayList<JSElement>(1);
			jsElements.add(this);

			return jsElements;
		}

		@Override
		public JSElement withName(final String newName) {
			return new TestJS(newName);
		}
	}

	@Before
	public void setUp() throws Exception {
		js = new TestJS();

	}

	@Test
	public void testGetName() throws Exception {

		MatcherAssert.assertThat(js.getName(), Matchers.equalTo(expectedName));
	}

	@Test
	public void testDescription() throws Exception {

		final String description = "description";
		js.setDescription(description);

		MatcherAssert.assertThat(js.getDescription(), Matchers.equalTo(description));
		MatcherAssert.assertThat(js.getDescription(), Is.is(Matchers.sameInstance(description)));
	}

	@Test
	public void testGetType() throws Exception {

		MatcherAssert.assertThat(js.getType(), Matchers.equalTo(expectedType));
	}

	@Test
	public void testGetProperties() throws Exception {

		final List<JSElement> properties = js.getProperties();

		MatcherAssert.assertThat(properties, Is.is(Matchers.instanceOf(List.class)));
		MatcherAssert.assertThat(properties.size(), Matchers.equalTo(1));
		MatcherAssert.assertThat(properties, Matchers.hasItem(js));
	}

	@Test
	public void testWithName() throws Exception {

		final String name = "bar";

		final JSElement withName = js.withName(name);

		MatcherAssert.assertThat(withName, Is.is(Matchers.instanceOf(TestJS.class)));

		final TestJS withNameJS = (TestJS) withName;

		MatcherAssert.assertThat(withNameJS, Is.is(Matchers.not(Matchers.sameInstance(js))));

		MatcherAssert.assertThat(withNameJS.getName(), Matchers.equalTo(name));
	}

	@Test
	public void testRender() throws Exception {
		// TODO

	}

	@Test
	public void testRenderDescription() throws Exception {
		// TODO

	}

	@Test
	public void testRenderInternal() throws Exception {
		// TODO

	}
}
