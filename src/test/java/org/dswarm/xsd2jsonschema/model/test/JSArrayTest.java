package org.dswarm.xsd2jsonschema.model.test;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import org.dswarm.xsd2jsonschema.model.JSArray;
import org.dswarm.xsd2jsonschema.model.JSElement;
import org.dswarm.xsd2jsonschema.model.JSString;

public class JSArrayTest extends BaseJSTest<JSArray> {

	private JSElement	item;

	public JSArrayTest() {
		super(JSArray.class);

	}

	@Override
	@Before
	public void setUp() throws Exception {

		item = new JSString("bar");
		obj = new JSArray(item);
	}

	@Test
	public void testGetType() throws Exception {

		MatcherAssert.assertThat(obj.getType(), Matchers.equalTo("array"));
	}

	@Test
	public void testGetProperties() throws Exception {

		MatcherAssert.assertThat(obj.getProperties(), Matchers.is(Matchers.nullValue()));
	}

	@Test
	public void testGetItem() throws Exception {

		MatcherAssert.assertThat(obj.getItem(), Matchers.is(Matchers.sameInstance(item)));
	}

	@Test
	public void testRenderInternal() throws Exception {
		// TODO

	}
}
