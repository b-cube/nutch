/**
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

package org.apache.nutch.indexer.bcubefilter;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlink;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.metadata.HttpHeaders;
import org.apache.nutch.parse.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Discards a document if the content is empty
 */
public class DiscardBCubeIndexingFilter implements IndexingFilter {
  public static final Logger LOG = LoggerFactory.getLogger(DiscardBCubeIndexingFilter.class);

  private Configuration conf;
  private List<String> allowedMimeTypes;

 /**
  * The {@link DiscardBCubeIndexingFilter} filter object 
  *  
  * @param doc The {@link NutchDocument} object
  * @param parse The relevant {@link Parse} object passing through the filter 
  * @param url URL to be filtered for anchor text
  * @param datum The {@link CrawlDatum} entry
  * @param inlinks The {@link Inlinks} containing anchor text
  * @return filtered NutchDocument
  */
  public NutchDocument filter(NutchDocument doc, Parse parse, Text url, CrawlDatum datum, Inlinks inlinks)
    throws IndexingException {
	  // For now just the mime type affects this filter
	  if(mimeTypeFilter(doc) && urlFilter(doc) && relevantOrNot(doc)) {
          doc.removeField("content"); // we don't want it.
          doc.add("url_hash", DigestUtils.shaHex(doc.getFieldValue("id").toString()));
          for (String header: parse.getData().getContentMeta().names()) {
        	  doc.add("response_headers", header + ": " + parse.getData().getContentMeta().get(header));          	  
          }          
          // doc.setWeight(1); // Do something here
		  return doc;
	  }
	  return null;
  }
  
  public boolean urlFilter(NutchDocument doc) {
	  //TODO [This should not be a replacement for the URL regex.]
	  return true;
  }
  
  public boolean relevantOrNot(NutchDocument doc) {
	  //TODO [This method will send the URL, anchor text and perhaps 
	  //  a set of limited tokens to a service that will decide
	  //  if it is relevant (meaning is a web service or data) 
	  //  and therefore it should be indexed.
	  return true;
  }  
  
  
  public boolean mimeTypeFilter(NutchDocument doc) {
    if (doc.getField("type") != null) {
    	String documentType = doc.getField("type").getValues().get(0).toString();
	    for (String allowedType : this.allowedMimeTypes) {
    	  if (documentType.contains(allowedType)) {
    		  // will be indexed
    		  return true;
    	  }
	    }
	    return false;
    } else {
      LOG.warn("The index-more plugin should be added before this plugin in indexingfilter.order");
      return false;
    }
  }

  /**
   * Set the {@link Configuration} object
   */
  public void setConf(Configuration conf) {
    this.conf = conf;
    String allowedTypes = conf.get("indexingfilter.bcube.allowed.mimetypes");
    if (allowedTypes != null && !allowedTypes.trim().isEmpty()) {
        this.allowedMimeTypes = Arrays.asList(allowedTypes.trim().split("\\s+"));
    } else {
    	this.allowedMimeTypes = Arrays.asList("application/xml", "text/xml", "json", "opensearchdescription+xml", "text/plain");
    }
  }

  /**
   * Get the {@link Configuration} object
   */
  public Configuration getConf() {
    return this.conf;
  }

}
