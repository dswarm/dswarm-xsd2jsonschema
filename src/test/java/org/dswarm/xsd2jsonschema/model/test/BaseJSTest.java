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

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.dswarm.xsd2jsonschema.model.JSElement;

public abstract class BaseJSTest<T extends JSElement> {

	protected static ObjectMapper	om;

	private final Class<T>			clazz;

	protected T						obj;

	protected BaseJSTest(final Class<T> clazz) {

		this.clazz = clazz;
	}

	@BeforeClass
	public static void startUp() throws Exception {
		BaseJSTest.om = new ObjectMapper();
	}

	@Before
	public void setUp() throws Exception {
		obj = clazz.getConstructor(String.class).newInstance("foo");

	}

	@Test
	public void testWithName() throws Exception {

		MatcherAssert.assertThat(obj.withName("bar").getName(), Matchers.equalTo("bar"));
		MatcherAssert.assertThat(obj.withName("bar"), Matchers.is(Matchers.instanceOf(clazz)));
	}

}
