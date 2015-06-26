/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.nsidc.nutch.index.links;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlink;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.parse.Outlink;
import org.apache.nutch.parse.Parse;
import org.slf4j.LoggerFactory;

public class LinksIndexingFilter implements IndexingFilter {

	public final static org.slf4j.Logger LOG = LoggerFactory
			.getLogger(LinksIndexingFilter.class);

	private Configuration conf;
	private int MAX_OUTLINKS=100;
	private int MAX_INLINKS=100;

	// Inherited JavaDoc
	@Override
	public NutchDocument filter(NutchDocument doc, Parse parse, Text url,
			CrawlDatum datum, Inlinks inlinks) throws IndexingException {

		if (doc != null) {
			// Add the outlinks
			Outlink[] o = parse.getData().getOutlinks();
			Collection<String> outlinks = new HashSet<String>();

			if (o != null) {
				for (Outlink outlink : o) {
					outlinks.add(outlink.getToUrl());
				}
				int count = 0;
				for (String outlink : outlinks) {
					
					doc.add("outlinks", outlink);					
					if ((count += 1) >= MAX_OUTLINKS) break;
				}
			}
			// add the inlinks
			if (inlinks != null) {
				int count = 0;
				Iterator<Inlink> iterator = inlinks.iterator();
				while (iterator.hasNext()) {
					Inlink link = iterator.next();
					doc.add("inlinks", link.getFromUrl());
					if ((count += 1) >= MAX_INLINKS) break;
				}
			}
		}

		return doc;
	}
	
	public void setMaxOutlinks(int maxLinks) {
		this.MAX_OUTLINKS = maxLinks;
	}
	
	public int getMaxOutlinks() {
		return this.MAX_OUTLINKS;
	}
	
	public void setMaxInlinks(int maxLinks) {
		this.MAX_INLINKS = maxLinks;
	}
	
	public int getMaxInlinks() {
		return this.MAX_INLINKS;
	}

	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	public Configuration getConf() {
		return this.conf;
	}

}
