/*
 * The MIT License
 *
 * Copyright (c) 2018 Joe Offenberg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.sweagle.jenkins.plugins;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.POST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;

import net.sf.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//DescriptorImpl governs the global config settings

@Extension
@Symbol("SWEAGLEValidate")
public final class DescriptorImpl_Validate extends BuildStepDescriptor<Builder> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SweagleActionValidate.class);
    private String sweagleURL;
    private Secret sweagleAPIkey;
    
    public DescriptorImpl_Validate() {
        super(SweagleActionValidate.class);
        load();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "SWEAGLE Validate";
    }
    
    // Allows for persisting global config settings in JSONObject
    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        formData = formData.getJSONObject("SWEAGLE");
        sweagleURL = formData.getString("sweagleURL");
        sweagleAPIkey = Secret.fromString(formData.getString("sweagleAPIkey"));
        save();
        return false;
    }


    public String getSweagleURL() {
        return sweagleURL;
    }

    public Secret getSweagleAPIkey() {
        return sweagleAPIkey;
    }



    public void setsweagleURL(String sweagleURL) {
        this.sweagleURL = sweagleURL;
    }

    public void setSweagleAPIkey(Secret sweagleAPIkey) {
        this.sweagleAPIkey = sweagleAPIkey;
    }

    // Added @POST to help protect against CSRF
    @POST
    public FormValidation doTestConnection(
    		
            @QueryParameter("sweagleURL") final String sweagleURL,
            @QueryParameter("sweagleAPIkey") final Secret sweagleAPIkey) {
        // Admin permission check
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        OkHttpClient client = new OkHttpClient();
        String responseAsString = null;
        try {        
        	Request request = new Request.Builder()
        			  .url(sweagleURL + "/api/v1/data/report/count/metadataset")
        			  .get()
        			  .addHeader("Authorization", "Bearer " + sweagleAPIkey)
        			  .build();

        	Response response = client.newCall(request).execute();
        	responseAsString = response.body().string();
        	int numMeasurements=JsonPath.read(responseAsString, "$.data[0].total");
        			
			LOGGER.info("Connection Successful. Found {} MDS", numMeasurements);
            return FormValidation.ok("Connection Successful.  Found " + numMeasurements + " Metadata sets");

        } catch (Exception e) {
            LOGGER.error("Error testing connection to url({}), error : {} \n response: " + responseAsString, sweagleURL,
                    e.getMessage());
            if (e.getMessage() != null) {
                
				return FormValidation.error(e, "API Connection error : " + e.getMessage() + "\n response:    " + responseAsString);
            }
            return FormValidation.error(e, "Connection Error");
        } 
        
        
        finally {
           
            }
        }
   
}
