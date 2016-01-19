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
import org.junit.Test;

import org.dswarm.xsd2jsonschema.model.JSNull;

public class JSNullTest extends BaseJSTest<JSNull> {

	public JSNullTest() {
		super(JSNull.class);
	}

	@Test
	public void testGetType() throws Exception {

		MatcherAssert.assertThat(obj.getType(), Matchers.equalTo("null"));
	}

	@Test
	public void testGetProperties() throws Exception {

		MatcherAssert.assertThat(obj.getProperties(), Matchers.is(Matchers.nullValue()));
	}

	@Test
	public void testRender() throws Exception {
		// TODO

	}
}
