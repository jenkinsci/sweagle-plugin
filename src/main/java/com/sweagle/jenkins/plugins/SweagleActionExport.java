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

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import javax.annotation.CheckForNull;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.util.DescribableList;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;


/**
 * 
 * Upload a file to SWEAGLE
 */
public class SweagleActionExport extends hudson.tasks.Builder implements SimpleBuildStep {
	@CheckForNull
	private String actionName;

	@CheckForNull
	private String mdsName;	
	
	
	private String fileLocation;		
	
	@CheckForNull
	private String format;
	
	@CheckForNull
	private String tag="";
	
	@CheckForNull
	private String exporter;
	
	private String mapToVariables;

	@CheckForNull
	private String args="";
	
	private boolean markFailed=false;
	private boolean showResults=false;
	
	
	
	@DataBoundConstructor
	public SweagleActionExport(@CheckForNull String actionName, @CheckForNull String mdsName,  @CheckForNull String exporter, @CheckForNull String format) {
		this.actionName = Util.fixEmptyAndTrim(actionName);
		this.mdsName = mdsName;
		this.exporter=exporter;
		this.format=format;
				
	}
	
	@DataBoundSetter
	public void setFileLocation(String fileLocation){
		this.fileLocation=fileLocation;
	}
	
	@DataBoundSetter
	public void setMarkFailed(boolean markFailed) {
		this.markFailed = markFailed;
	}
	
	

	@DataBoundSetter
	public void setShowResults(boolean showResults) {
		this.showResults = showResults;
	}
	

	@DataBoundSetter
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	@DataBoundSetter
	public void setMapToVariables(String mapToVariables) {
		this.mapToVariables = mapToVariables;
	}
	
	
	@DataBoundSetter
	public void setArgs(String args) {
		this.args = args;
	}


	public String getActionName() {
		return actionName;
	}

	public String getMdsName() {
		return mdsName;
	}
		
	public String getFileLocation() {
		return fileLocation;
	}
	
	public String getExporter() {
		return exporter;
	}
	
	public String getArgs() {
		return args;
	}
	
	public String getTag() {
		return tag;
	}
	
	public String getFormat() {
		return format;
	}
	
	public String getMapToVariables() {
		return mapToVariables;
	}
	
	public boolean getMarkFailed() {
		return markFailed;
	}

	public boolean getShowResults() {
		return showResults;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
	

	@Override
	public DescriptorImpl_Export getDescriptor() {
		return (DescriptorImpl_Export) super.getDescriptor();
	}
	
	
	
	

	// Job Plugin execution code
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		
		
		
		final EnvVars env = run.getEnvironment(listener);
		Jenkins jenkins = Jenkins.getInstance();
		DescriptorImpl_Validate descriptorImpl = jenkins.getDescriptorByType(DescriptorImpl_Validate.class);
		String sweagleURL = descriptorImpl.getSweagleURL();
		Secret sweagleAPIkey = descriptorImpl.getSweagleAPIkey();
		if (sweagleURL == null) {throw new AbortException("Sweagle URL not set in Jenkins Configuration.");}
		

		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		loggerUtils.info("SWEAGLE Export Action: " + actionName + " at: " + sweagleURL);
		
		String actionResonse = null;
			String mdsNameEnv = env.expand(mdsName);
			String argsEnv = env.expand(args);
			String fileLocationEnv = env.expand(fileLocation);
			if (tag==null) {tag="";}
			String tagEnv=env.expand(tag);
			
			if (fileLocationEnv.length()>2) {
			actionResonse = SweagleUtils.exportConfig(sweagleURL, sweagleAPIkey, mdsNameEnv, fileLocationEnv,  exporter, argsEnv, format, tagEnv, markFailed, workspace, listener, env);
			}
			if (mapToVariables !=null) {
			
				String[] lines = mapToVariables.split(System.getProperty("line.separator"));
				for( int i = 0; i <= lines.length - 1; i++) {
					String[] keyVal=lines[i].split(":");
					String envVariable=keyVal[0];
					String jsonPath=keyVal[1];
					String varValue=SweagleUtils.getMdsValue(sweagleURL, sweagleAPIkey, mdsName, tag, jsonPath, showResults, markFailed, listener);
					loggerUtils.info("Setting " + envVariable + " to value retrieved from " + varValue);
				
				
					putEnvVar(envVariable, varValue);
					
					
				}
				
			
			}
		
		
		if (showResults)
		loggerUtils.debug(actionResonse);
		

	}
	
	 private void putEnvVar(String key, String value) throws IOException {
	     Jenkins jenkins = Jenkins.getInstance();
	     DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = jenkins.getGlobalNodeProperties();
	     List<EnvironmentVariablesNodeProperty> envVarsNodePropertyList = globalNodeProperties.getAll(hudson.slaves.EnvironmentVariablesNodeProperty.class);

	     EnvironmentVariablesNodeProperty newEnvVarsNodeProperty = null;
	     EnvVars envVars = null;

	     if (envVarsNodePropertyList == null || envVarsNodePropertyList.isEmpty()) {
	        newEnvVarsNodeProperty = new hudson.slaves.EnvironmentVariablesNodeProperty();
	        globalNodeProperties.add(newEnvVarsNodeProperty);
	        envVars = newEnvVarsNodeProperty.getEnvVars();
	     } else {
	        envVars = envVarsNodePropertyList.get(0).getEnvVars();
	     }
	     envVars.put(key, value);
	  }
	
}
