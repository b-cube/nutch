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

package org.nsidc.nutch.parse.bayes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Outlink;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.protocol.Content;
import org.junit.Test;
import org.nsidc.nutch.parse.bayes.training.TrainingHandler;


public class BayesParseFilterTest {


	//TODO: This needs to be refactored to make tests easier.
	@Test
	public void filter_should_retain_only_relevant_outlinks_from_irrelevant_documents() {
		String url = "http://spam.url";
		String rawContentValue = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?> <some>xml</some>";
		// The parsed content will be labeled using the Bayes classifier
		String parsedContent = "this should be spam! offer for free now download";
		Outlink[] outlinks =  new Outlink[4];
		try {
			// relevant
			outlinks[0] = new Outlink("http://geoscience-repository.edu", null);
			outlinks[1] = new Outlink("http://geoscience-metadata.org", null);
			// non relevant
			outlinks[2] = new Outlink("http://world-of-spam.org", null);
			outlinks[3] = new Outlink("http://helpless-site.com", null);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ParseResult mockParseResult = createMockParseResultWithMetadata(outlinks, parsedContent);
		Content fakeContent = createFakeContent(url, rawContentValue);
		// These words will be compared against the outlinks
		String words = "1\trelevant\n2\tmetadata\n3\tgeo";
		String trainingSet = createTrainingSet();
		Configuration mockConf = createMockConf(words, trainingSet);
		
		BayesParseFilter parseFilter = new BayesParseFilter();
		parseFilter.setConf(mockConf);
		// act
		ParseResult returnedParseResult = parseFilter.filter(fakeContent, mockParseResult, null, null);

		// assert
		assertEquals(2, returnedParseResult.get(url).getData().getOutlinks().length);
		assertEquals("http://geoscience-repository.edu", returnedParseResult.get(url).getData().getOutlinks()[0].getToUrl());
		assertEquals("http://geoscience-metadata.org", returnedParseResult.get(url).getData().getOutlinks()[1].getToUrl());

	}

	@Test
	public void filter_should_retain_all_outlinks_from_relevant_documents() {
		String url = "http://relevant.url";
		String rawContentValue = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?> <some>xml</some>";
		// The parsed content will be labeled using the Bayes classifier
		String parsedContent = "this should be relevant: doppler information climate modeling";
		Outlink[] outlinks =  new Outlink[4];
		try {
			// relevant
			outlinks[0] = new Outlink("http://geoscience-repository.edu", null);
			outlinks[1] = new Outlink("http://geoscience-metadata.org", null);
			// non relevant
			outlinks[2] = new Outlink("http://world-of-spam.org", null);
			outlinks[3] = new Outlink("http://helpless-site.com", null);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ParseResult mockParseResult = createMockParseResultWithMetadata(outlinks, parsedContent);
		Content fakeContent = createFakeContent(url, rawContentValue);
		// These words will be compared against the outlinks
		String words = "1\trelevant\n2\tmetadata\n3\tgeo";
		String trainingSet = createTrainingSet();
		Configuration mockConf = createMockConf(words, trainingSet);
		
		BayesParseFilter parseFilter = new BayesParseFilter();
		parseFilter.setConf(mockConf);
		// act
		ParseResult returnedParseResult = parseFilter.filter(fakeContent, mockParseResult, null, null);

		// assert
		assertEquals(4, returnedParseResult.get(url).getData().getOutlinks().length);	
		assertEquals("http://helpless-site.com", returnedParseResult.get(url).getData().getOutlinks()[3].getToUrl());
		

	}
	
	@Test
	public void filter_should_adquire_new_training_samples_if_available() {
		String url = "http://unseen.url";
		String rawContentValue = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?> <some>xml</some>";
		// The parsed content will be labeled using the Bayes classifier
		String parsedContent = "valid with current info, not after retrainning with justin bieber and twilight saga";
		Outlink[] outlinks =  new Outlink[4];
		try {
			// relevant
			outlinks[0] = new Outlink("http://geoscience-repository.edu", null);
			outlinks[1] = new Outlink("http://geoscience-metadata.org", null);
			// non relevant
			outlinks[2] = new Outlink("http://world-of-spam.org", null);
			outlinks[3] = new Outlink("http://helpless-site.com", null);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ParseResult mockParseResult = createMockParseResultWithMetadata(outlinks, parsedContent);
		Content fakeContent = createFakeContent(url, rawContentValue);
		// These words will be compared against the outlinks
		String words = "1\trelevant\n2\tmetadata\n3\tgeo";
		String trainingSet = createTrainingSet();
		Configuration mockConf = createMockConf(words, trainingSet);
		
		BayesParseFilter parseFilter = new BayesParseFilter();		
		parseFilter.setConf(mockConf);
		assertEquals(6, parseFilter.getTrainner().getLastTrainingId());
		//act 1
		ParseResult returnedParseResult = parseFilter.filter(fakeContent, mockParseResult, null, null);
		// assert
		assertEquals(4, returnedParseResult.get(url).getData().getOutlinks().length);
		
		parseFilter.getTrainner().setBayesAPIurl("http://www.mocky.io/v2/562555b1100000704523eb17");		
		// act
		returnedParseResult = parseFilter.filter(fakeContent, mockParseResult, null, null);
		assertEquals(10, parseFilter.getTrainner().getLastTrainingId());
		// assert
		assertEquals(2, returnedParseResult.get(url).getData().getOutlinks().length);		
		

	}	
	
	private ParseResult createMockParseResultWithMetadata(Outlink[] outlinks, String parsedContent) {
		ParseData parseData = new ParseData();
		Parse mockParse = mock(Parse.class);
		ParseResult mockParseResult = mock(ParseResult.class);

		if (outlinks != null) {
			parseData.setOutlinks(outlinks);
		}
		
		when(mockParse.getData()).thenReturn(parseData);
		when(mockParse.getText()).thenReturn(parsedContent);
		when(mockParseResult.get(anyString())).thenReturn(mockParse);
		return mockParseResult;
	}

	private Content createFakeContent(final String url, final String content) {
		byte[] contentByteArray = {};
		if (content != null) {
			contentByteArray = content.getBytes();
		}
		Content fakeContent = new Content(url, "", contentByteArray, null, mock(Metadata.class), mock(Configuration.class));
		return fakeContent;
	}
	
	private Configuration createMockConf(String urls, String trainingSet) {
		// We are mocking a Nutch configuration file, it's simpler for unit testing.
		Configuration mockConf = mock(Configuration.class);
		Reader urlReader = new StringReader(urls);
		Reader trainingReader = new StringReader(trainingSet);
		
		when(mockConf.get("bayes.training.file")).thenReturn("bayes");
		when(mockConf.get("bayes.words.file")).thenReturn("words");
		
		when(mockConf.getConfResourceAsReader("bayes")).thenReturn(trainingReader);
		when(mockConf.getConfResourceAsReader("words")).thenReturn(urlReader);
		
		return mockConf;
	}	
	
	private String createTrainingSet(){
		String trainingSet =        "1\trelevant\tObservatory Acoustic Doppler Model Current Profiler Service Temperature Climate\n";
		trainingSet = trainingSet + "2\trelevant\tHawaiian Islands Multibeam Synthesis Bathymetry Data Elevation Topography Ice\n";
		trainingSet = trainingSet + "3\trelevant\tRegional Ocean Modeling System Weather South Dataset Shore Variable Ocean Atmosphere\n";
		trainingSet = trainingSet + "4\tignore\tInternet Click Ad News Company Promotion Browser Promo Your Comic\n";
		trainingSet = trainingSet + "5\tignore\tBilling Copy DVDs Upload Free Now Started Company Refund Email Extra Guarantee\n";
		trainingSet = trainingSet + "6\tignore\tTraffic Millions Notice Download Offers Offert Password Order Form\n";	
		return trainingSet;
	}	

}
