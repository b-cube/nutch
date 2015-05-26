package org.nsidc.nutch.index.rawcontent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.indexer.NutchField;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.util.NutchConfiguration;
import org.junit.Test;

public class RawContentIndexingFilterTest {

	@Test
	public void testIndexingFilterAdds_raw_content_Field() throws Exception {
		Configuration conf = NutchConfiguration.create();
		conf.setBoolean("moreIndexingFilter.indexMimeTypeParts", false);
		RawContentIndexingFilter filter = new RawContentIndexingFilter();
		filter.setConf(conf);

		NutchDocument doc = new NutchDocument();

		Parse parse = mock(Parse.class);
		Metadata metadata = new Metadata();
		metadata.set("raw_content", "Some value");
		ParseData parseData = new ParseData();
		parseData.setParseMeta(metadata);

		// Mock parser response
		when(parse.getData()).thenReturn(parseData);

		filter.filter(doc, parse, null, null, null);

		assertTrue(doc.getFieldNames().contains("raw_content"));

		NutchField rawContentField = doc.getField("raw_content");
		String rawContentValue = rawContentField.getValues().get(0).toString();
		assertTrue(rawContentValue.equals("Some value"));
	}

	@Test
	public void testIndexingFilterSkips_raw_content_Field() throws Exception {
		// If the field was not added by the raw content parser plugin then
		// nothing is indexed
		Configuration conf = NutchConfiguration.create();
		conf.setBoolean("moreIndexingFilter.indexMimeTypeParts", false);
		RawContentIndexingFilter filter = new RawContentIndexingFilter();
		filter.setConf(conf);

		NutchDocument doc = new NutchDocument();

		Parse parse = mock(Parse.class);
		Metadata metadata = new Metadata();
		ParseData parseData = new ParseData();
		parseData.setParseMeta(metadata);

		// Mock parser response
		when(parse.getData()).thenReturn(parseData);

		filter.filter(doc, parse, null, null, null);

		assertFalse(doc.getFieldNames().contains("raw_content"));

	}	
}
