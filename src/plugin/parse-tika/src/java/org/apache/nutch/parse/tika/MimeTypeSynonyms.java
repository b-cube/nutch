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

package org.apache.nutch.parse.tika;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;

public class MimeTypeSynonyms {

	private static final String MIME_SYNONYMS_FILE = "mime.type.synonyms.file";
    private List<Rule> rules = new ArrayList<Rule>();
    private boolean synonymsReplacement = false;

	public MimeTypeSynonyms(Configuration conf) {
		String mimeTypeRules = conf.get(MIME_SYNONYMS_FILE);
		if (mimeTypeRules != null ) {
			Reader rulesReader = conf.getConfResourceAsReader(mimeTypeRules);
			if (rulesReader != null) {
				try {
					setRules(getRegexRules(rulesReader));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			
			}
		}
	}
	
	public String replace(String originalMimeType) {
		for (Rule r : rules) {
			if (r.matches(originalMimeType)) {
				return r.getReplacement();
			}
		}
		return originalMimeType;
	}
	
	protected List<Rule> getRegexRules(Reader reader) throws IOException {
	    BufferedReader in = new BufferedReader(reader);
	    String line;
	    List<Rule> rules = new ArrayList<Rule>();
		while ((line = in.readLine()) != null) {			
			if (!line.isEmpty()) {
				String[] ruleReplacement = line.split("=");
				if (ruleReplacement.length == 2) {
					Rule r = new Rule(ruleReplacement[0], ruleReplacement[1]);
					rules.add(r);
					setSynonymsReplacement(true);
				}
			}
	    }		
		return rules;
	}
	
	
	
	public List<Rule> getRules() {
		return rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	
	public boolean isReplacementEnabled() {
		return synonymsReplacement;
	}

	public void setSynonymsReplacement(boolean synonymsReplacement) {
		this.synonymsReplacement = synonymsReplacement;
	}


	public class Rule {
	    private Pattern pattern;
	    private String replacement;

	    Rule(String regex, String replacement) {
	      pattern = Pattern.compile(regex);
	      this.replacement = replacement;
	    }
	    
	    public String getReplacement() {
	    	return replacement;
	    }

	    protected Boolean matches(String originalMimeType) {
	      return pattern.matcher(originalMimeType).find();
	    }
		
	}

}
