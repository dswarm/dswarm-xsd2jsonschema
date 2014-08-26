package org.dswarm.xsd2jsonschema.model.test;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import org.dswarm.xsd2jsonschema.model.JSString;

public class JSStringTest extends BaseJSTest<JSString> {

	public JSStringTest() {
		super(JSString.class);
	}

	@Test
	public void testGetType() throws Exception {

		MatcherAssert.assertThat(obj.getType(), Matchers.equalTo("string"));
	}

	@Test
	public void testGetProperties() throws Exception {

		MatcherAssert.assertThat(obj.getProperties(), Matchers.is(Matchers.nullValue()));
	}
}
