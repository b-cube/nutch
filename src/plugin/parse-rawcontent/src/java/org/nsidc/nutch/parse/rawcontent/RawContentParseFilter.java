package org.nsidc.nutch.parse.rawcontent;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.HtmlParseFilter;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.protocol.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;

/**
 * Makes full raw content of XML (and HTML) documents available; since Solr is a
 * likely repository for the content, the whole document is wrapped in CDATA
 * tags.
 */
public class RawContentParseFilter implements HtmlParseFilter {

	public final static String RAW_CONTENT = "raw_content";

	public static final Logger LOG = LoggerFactory
			.getLogger(RawContentParseFilter.class);

	private Configuration conf;

	@Override
	public Configuration getConf() {
		return conf;
	}

	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	@Override
	public ParseResult filter(Content content, ParseResult parseResult,
			HTMLMetaTags metaTags, DocumentFragment doc) {

		String url = content.getUrl();
		LOG.info("Getting raw content for " + url);

		Metadata metadata = parseResult.get(url).getData().getParseMeta();

		String rawContent = new String(content.getContent());
		metadata.add(RAW_CONTENT, rawContent);

		return parseResult;
	}

}
