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

import okhttp3.*;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.POST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jayway.jsonpath.JsonPath;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;

import net.sf.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

//DescriptorImpl governs the global config settings

@Extension
@Symbol("SWEAGLEValidate")
public final class DescriptorImpl_Validate extends BuildStepDescriptor<Builder> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SweagleActionValidate.class);
    private String sweagleURL;
    private Secret sweagleAPIkey;
    private String proxyHost;
    private int proxyPort;
    private String proxyUser;
    private Secret proxyPassword;
    
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
        proxyHost = formData.getString("proxyHost");
        proxyPort = formData.getInt("proxyPort");
        proxyUser = formData.getString("proxyUser");
        proxyPassword = Secret.fromString(formData.getString("proxyPassword"));
        save();
        return false;
    }


    public String getSweagleURL() {
        return sweagleURL;
    }
    public String getProxyHost() {
        return proxyHost;
    }
    public int getProxyPort() { return proxyPort; }
    public String getProxyUser() {
        return proxyUser;
    }
    public Secret getSweagleAPIkey() {
        return sweagleAPIkey;
    }
    public Secret getProxyPassword() {
        return proxyPassword;
    }


    public void setsweagleURL(String sweagleURL) {
        this.sweagleURL = sweagleURL;
    }
    public void setProxyHostL(String proxyHost) {
        this.proxyHost = proxyHost;
    }
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }
    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }
    public void setSweagleAPIkey(Secret sweagleAPIkey) {
        this.sweagleAPIkey = sweagleAPIkey;
    }
    public void setProxyPassword(Secret proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    // Added @POST to help protect against CSRF
    @POST
    public FormValidation doTestConnection(

            @QueryParameter("sweagleURL") final String sweagleURL,
            @QueryParameter("proxyHost") final String proxyHost,
            @QueryParameter("proxyPort") final int proxyPort,
            @QueryParameter("proxyUser") final String proxyUser,
            @QueryParameter("proxyPassword") final Secret proxyPassword,
            @QueryParameter("sweagleAPIkey") final Secret sweagleAPIkey) {
        // Admin permission check
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);

        Proxy sweagleProxy = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(proxyHost, proxyPort));
        OkHttpClient client = new OkHttpClient();
        Authenticator proxyAuthenticator = new Authenticator() {
            @Override
            public Request authenticate(Route route, Response response) throws IOException {
                String credential = Credentials.basic(proxyUser, Secret.toString(proxyPassword));
                return response.request().newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build();
            }
        };
      if (!proxyHost.isEmpty()) {
          client = client.newBuilder().proxy(sweagleProxy).build();
          if (!proxyUser.isEmpty()) {
              client = client.newBuilder().proxy(sweagleProxy).proxyAuthenticator(proxyAuthenticator).build();
          }
      }

        String responseAsString = "";
        int responseCode = 200;
        try {        
        	Request request = new Request.Builder()
        			  .url(sweagleURL + "/api/v1/data/report/count/metadataset")
        			  .get()
        			  .addHeader("Authorization", "Bearer " + sweagleAPIkey)
        			  .build();

        	Response response = client.newCall(request).execute();
        	responseAsString = response.body().string();
        	responseCode = response.code();

        } catch (Exception e) {
            LOGGER.error("Error testing connection to url({}), error : {} \n response: " + responseAsString, sweagleURL,
                    e.getMessage());
            if (e.getMessage() != null) {
                
				return FormValidation.error(e, "API Connection error : " + e.getMessage() + SweagleUtils.getErrorfromResponse(responseAsString));
            }
            return FormValidation.error(e, "Connection Error");
        } 
        if (responseCode==200) {
    	int numMeasurements=JsonPath.read(responseAsString, "$.data[0].total");
		LOGGER.debug("Connection Successful. Found {} CDS", numMeasurements);
        return FormValidation.ok("Connection Successful.  Found " + numMeasurements + " Config Data Sets");
        } else 
        return	FormValidation.error("Connection Error: " + SweagleUtils.getErrorfromResponse(responseAsString));
        }
   
}
