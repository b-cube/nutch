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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.HtmlParseFilter;
import org.apache.nutch.parse.Outlink;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.protocol.Content;
import org.codehaus.jettison.json.JSONObject;
import org.nsidc.nutch.parse.bayes.classifier.BayesClassifier;
import org.nsidc.nutch.parse.bayes.classifier.Classifier;
import org.nsidc.nutch.parse.bayes.training.TrainingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;


/**
 * This filter uses a naive-Bayes classifier to trim outlinks from documents to perform focused crawls.
 * The classifier takes the initial training set from a text file in the form {ID}TAB{LABEL}TAB{TOKENS}NEWLINE
 * 
 * {ID} should be a unique LONG number for each sample
 * {LABEL} should be a String for a given category
 * {TOKENS} should be a string with the training samples
 *  
 * After the TrainingHandler class is instantiated the filter will ask for new samples following the last used ID.
 * The new samples should follow the convention from http://www.mocky.io/v2/562555b1100000704523eb17
 * If there are no new samples the filter should continue with the current training set.
 * 
 * The more training samples the better, The naive-Bayes classifier used here is  is forgetful. 
 * This means, that the classifier will forget recent classifications it uses for future classifications 
 * after - defaulting to 1.000 - classifications learned.
 *  
 * @see https://github.com/ptnplanet/Java-Naive-Bayes-Classifier 
 * @see http://github.com/b-cube
 * @author @ betolink
 *
 */
public class BayesParseFilter implements HtmlParseFilter {

	public static final Logger LOG = LoggerFactory
			.getLogger(BayesParseFilter.class);

	private Configuration conf;
	private TrainingHandler trainer;
	private static Classifier<String, String> bayesClassifier = new BayesClassifier<String, String>();

	@Override
	public Configuration getConf() {
		return conf;
	}

	
	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;		
		try {
			this.trainer = new TrainingHandler(conf, bayesClassifier);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public TrainingHandler getTrainner() {
		return trainer;
	}


	public void setTrainner(TrainingHandler trainner) {
		this.trainer = trainner;
	}


	@Override
	public ParseResult filter(Content content, ParseResult parseResult,
			HTMLMetaTags metaTags, DocumentFragment doc) {
		
		JSONObject newSamples = trainer.getSamples();
		if (newSamples != null) {
			LOG.info("New trainning samples acquired, retraining the classifier...");
			trainer.updateTrainningset(newSamples);
		}

		String url = content.getUrl();
		Parse parse = parseResult.get(url);
		String[] text = parse.getText().toLowerCase().split("\\s");
		String label = bayesClassifier.classify(Arrays.asList(text)).getCategory();
		LOG.info(url + " was classified as: " + label);
		Outlink[] outlinks = getRelevantOutLinks(url, label, parse);		
		parseResult.get(url).getData().setOutlinks(outlinks);

		return parseResult;
	}
	
	protected Outlink[] getRelevantOutLinks(String url, String label, Parse parse) {
	   if (label.contentEquals("ignore")) {
			ArrayList<Outlink> relevantLinks = new ArrayList<Outlink>();
			 for (int i = 0; i < parse.getData().getOutlinks().length; i++) {
				 Outlink outlink = parse.getData().getOutlinks()[i];
				 if (filterUrl(outlink.getToUrl())) {
					 relevantLinks.add(outlink);
				 }
			 }
			Outlink[] relevantOutLinks = relevantLinks.toArray(new Outlink[relevantLinks.size()]);
			return relevantOutLinks;
	   } else {
		   return parse.getData().getOutlinks();
	   }	
	}

	protected boolean filterUrl(String url) {
		for(String relevantToken : this.trainer.getRelevantUrlTokens()) {
			if (url.contains(relevantToken)) {
				return true;
			}
		}
		return false;
	}
	
}
