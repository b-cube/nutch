/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.nutch.tika;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.parse.tika.MimeTypeSynonyms;
import org.apache.nutch.parse.tika.MimeTypeSynonyms.Rule;
import org.apache.nutch.util.NutchConfiguration;
import org.junit.Test;

public class TestTikaMimeSynonyms {

	@Test
	public void test_tika_mime_synonyms_with_no_configuration_file() {
		// just testing that we don't replace the original mime if there is no 
		// configuration file set in nutch-site/nutch-default
		Configuration conf;
		conf = NutchConfiguration.create();
		conf.set("mime.type.synonyms.file", "this-file-does-not-exist.txt");
		MimeTypeSynonyms mimeSynonyms = new MimeTypeSynonyms(conf);
		assertTrue(mimeSynonyms.isReplacementEnabled() == false);
	}

	@Test
	public void test_tika_mime_synonyms_with_a_configuration_file() {
		// Using Ascii files escape symbols \n \t etc.
		String ruleSet = "\\+xml$=application/xml\n\\+json$=application/json";
		Configuration mockConf = createMockConf(ruleSet);

		MimeTypeSynonyms mimeSynonyms = new MimeTypeSynonyms(mockConf);
		List<Rule> rules = mimeSynonyms.getRules();

		assertTrue(mimeSynonyms.isReplacementEnabled());
		assertTrue(rules.size() == 2);
		assertEquals(rules.get(0).getReplacement(), "application/xml");
		assertEquals(rules.get(1).getReplacement(), "application/json");

	}
	
	@Test
	public void test_tika_mime_synonyms_with_empty_configuration_file() {
		// Testing that that we don't use replacement if there are no rules.
		String ruleSet = "";
		Configuration mockConf = createMockConf(ruleSet);

		MimeTypeSynonyms mimeSynonyms = new MimeTypeSynonyms(mockConf);
		List<Rule> rules = mimeSynonyms.getRules();

		assertFalse(mimeSynonyms.isReplacementEnabled());
		assertTrue(rules.size() == 0);

	}

	@Test
	public void test_tika_mime_synonyms_replaces_ocurrences() {
		// Test that we can replace a mime type with a synonym.
		String ruleSet = "\\+xml$=application/xml";
		Configuration mockConf = createMockConf(ruleSet);
		MimeTypeSynonyms mimeSynonyms = new MimeTypeSynonyms(mockConf);
		List<Rule> rules = mimeSynonyms.getRules();

		String originalOpenSearchMimeType = "application/opensearchdescription+xml";
		String expectedMimeType = "application/xml";

		assertTrue(mimeSynonyms.isReplacementEnabled());
		assertTrue(rules.size() == 1);
		String result = mimeSynonyms.replace(originalOpenSearchMimeType);
		assertEquals(result, expectedMimeType);

	}

	private Configuration createMockConf(String rules) {
		// We are mocking a Nutch configuration file, it's simpler for unit testing.
		Configuration mockConf = mock(Configuration.class);
		Reader r = new StringReader(rules);
		when(mockConf.get("mime.type.synonyms.file")).thenReturn(
				"virtual-file.txt");
		when(mockConf.getConfResourceAsReader(anyString())).thenReturn(r);
		return mockConf;
	}

}
