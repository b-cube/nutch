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

package org.nsidc.nutch.parse.bayes.training;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import org.nsidc.nutch.parse.bayes.classifier.Classifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TrainingHandler {
	
	private static final String BAYES_TRAINING_FILE = "bayes.training.file";
	private static final String BAYES_URL_WORDS = "bayes.words.file";
	private static final String BAYES_REST_API = "bayes.api.url";
	
	private long lastUrlSubstringId = 0;
	private long lastTrainingId = 0;
	
	private Set<String> urlWords = new HashSet<String>();
	private Set<String> trainingIds = new HashSet<String>();
	
	protected Classifier<String, String> bayes; 
	
	public static final Logger LOG = LoggerFactory
			.getLogger(TrainingHandler.class);
	
	
	private String bayesAPIurl = "";
	
	
	public TrainingHandler (Configuration conf, Classifier<String, String> bayesClassifier) throws IOException {
		this.bayes = bayesClassifier;
		String trainingFile = conf.get(BAYES_TRAINING_FILE);
		String urlWordsFile = conf.get(BAYES_URL_WORDS);	
		String bayesAPI = conf.get(BAYES_REST_API);
		if (trainingFile != null) {
			Reader trainingFileReader = conf.getConfResourceAsReader(trainingFile);
			BufferedReader trainingReader = new BufferedReader(trainingFileReader);
			this.loadTrainingSet(trainingReader);
		}
		if (urlWordsFile != null) {			
			Reader urlFileReader = conf.getConfResourceAsReader(urlWordsFile);			
			BufferedReader urlsReader = new BufferedReader(urlFileReader);
			this.loadUrlSubstrings(urlsReader);			
		}
		if (bayesAPI != null ) {
		 this.setBayesAPIurl(bayesAPI + "/lastSample/" + Long.toString(this.lastTrainingId));
		}
	}
	
	public String getCategoryLabel(String text){
		String[] samples = text.toLowerCase().split("\\s");
		return this.bayes.classify(Arrays.asList(samples)).getCategory();
	}
	

	public void updateTrainningset(JSONObject json) {
		 try {
			String sampleId = "";
			JSONArray samples = json.getJSONArray("samples");
			for (int i=0; i< samples.length(); i++) {
				JSONObject sampleItem = samples.getJSONObject(i);
				sampleId = sampleItem.names().getString(0);
				JSONObject newSample = sampleItem.getJSONObject(sampleId);
				String label = newSample.names().getString(0);
				String sample = newSample.getString(label).toLowerCase();
				if (!this.trainingIds.contains(sampleId)) {
					LOG.info("Adding new sample: " + sampleId);
					this.bayes.learn(label, Arrays.asList(sample.split("\\s")));					
				}				
			}	
			this.lastTrainingId = Long.parseLong(sampleId);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
	}	
	
	public JSONObject getSamples() {
		if (this.getBayesAPIurl() != "") {
			try {
				Client client = Client.create();
				WebResource webRS = client.resource(this.getBayesAPIurl());
				ClientResponse response = webRS.accept("application/json").get(ClientResponse.class);
				if (response.getStatus() != 200) {
					throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
				} else {				
					JSONObject json = new JSONObject(response.getEntity(String.class));
					return json;
				}
			} catch (Exception e) {
				LOG.error(e.getMessage());
			}
		} else {
			LOG.info("API URL not set, no new training samples were added to the Bayes classifier");
		}
		return null;
	}
	
	
	protected void loadUrlSubstrings(BufferedReader urlsReader) throws IOException {	
		String line;
		while ((line = urlsReader.readLine()) != null) {			
			this.urlWords.add(line);			
		}
	}
	
	protected void loadTrainingSet(BufferedReader trainingReader) throws IOException {
		String line;
		String tId = "";
		while ((line = trainingReader.readLine()) != null) {
			String[] trainSet = line.split("\t");
			tId = trainSet[0];
			String trainingLabel = trainSet[1].toLowerCase();
			String[] trainingWords = trainSet[2].toLowerCase().split("\\s");
			if (!trainingIds.contains(tId)) {
				LOG.info(" Trainig sample " + tId);
				this.bayes.learn(trainingLabel, Arrays.asList(trainingWords));
			} else {
				LOG.info(" Trainig sample " + tId + " already included, skipping");
			}
		}
		this.lastTrainingId = Long.parseLong(tId);		
	}

	public long getLastUrlSubstringId() {
		return lastUrlSubstringId;
	}


	public long getLastTrainingId() {
		return lastTrainingId;
	}

	public String getBayesAPIurl() {
		return bayesAPIurl;
	}

	public void setBayesAPIurl(String bayesAPIurl) {
		this.bayesAPIurl = bayesAPIurl;
	}
	
	public Set<String> getRelevantUrlTokens() {
		return this.urlWords;
	}


}
