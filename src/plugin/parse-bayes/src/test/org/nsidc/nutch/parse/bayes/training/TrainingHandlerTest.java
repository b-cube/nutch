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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.util.NutchConfiguration;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.nsidc.nutch.parse.bayes.classifier.BayesClassifier;
import org.nsidc.nutch.parse.bayes.classifier.Classifier;


public class TrainingHandlerTest {

	@Test
	public void test_handler_does_nothing_if_no_bayes_in_configuration_file() {
		// just testing that we don't replace the original mime if there is no 
		Configuration conf;
		final Classifier<String, String> bayes = new BayesClassifier<String, String>();
		TrainingHandler trainer = null;
		conf = NutchConfiguration.create();		
		try {
			trainer = new TrainingHandler(conf, bayes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(trainer.getLastUrlSubstringId() == 0);
		assertTrue(trainer.getLastTrainingId() == 0);		
	}
	
	
	@Test
	public void test_handler_trains_bayes_on_instantiation() {
		String words = "1\tword1\n2\tword2\n3\tword3";
		String trainingSet = createTrainingSet();
		TrainingHandler trainer = null;
		final Classifier<String, String> bayes = new BayesClassifier<String, String>();				
		Configuration mockConf = createMockConf(words, trainingSet);
		try {
			trainer = new TrainingHandler(mockConf, bayes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assertEquals(trainer.getLastTrainingId(), 6);	
		
	}
	
	@Test
	public void test_handler_classifies_correctly() {
		String words = "1\tword1\n2\tword2\n3\tword3";
		String trainingSet = createTrainingSet();
		TrainingHandler trainer = null;
		String ignore_text = "Company Lego Ripped Off Make Plastic Bricks";
		String relevant_text = "Tide Model for the Pacific Ocean";
		String mix_text = "Will you Ever Be Able to Upload your Brain Model for Free";
		final Classifier<String, String> bayes = new BayesClassifier<String, String>();				
		Configuration mockConf = createMockConf(words, trainingSet);
		try {
			trainer = new TrainingHandler(mockConf, bayes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assertEquals(trainer.getLastTrainingId(), 6);
		assertEquals(trainer.getCategoryLabel(ignore_text), "ignore");
		assertEquals(trainer.getCategoryLabel(relevant_text), "relevant");
		assertEquals(trainer.getCategoryLabel(mix_text), "ignore");
	}
	
	@Test
	public void test_handler_updates_trainning_samples(){
		String words = "1\tword1\n2\tword2\n3\tword3";
		String trainingSet = createTrainingSet();
		TrainingHandler trainer = null;
		String unclassified_text = "twilight saga";
		final Classifier<String, String> bayes = new BayesClassifier<String, String>();				
		Configuration mockConf = createMockConf(words, trainingSet);
		try {
			trainer = new TrainingHandler(mockConf, bayes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// An unknown object is most likely to be classified as relevant
		assertEquals("relevant", trainer.getCategoryLabel(unclassified_text));		
		trainer.setBayesAPIurl("http://www.mocky.io/v2/562555b1100000704523eb17");
		JSONObject newSamples = trainer.getSamples();
		if (newSamples != null) {
			// we don't want anymore twilight movies!
			trainer.updateTrainningset(newSamples);
		}
		// Now with new samples this should be ignored
		assertEquals("ignore", trainer.getCategoryLabel(unclassified_text));
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
