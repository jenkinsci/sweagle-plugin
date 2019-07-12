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


import javax.annotation.CheckForNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;

/**
 * 
 * Upload a file to SWEAGLE
 */
public class SweagleActionUpload extends hudson.tasks.Builder implements SimpleBuildStep {
	@CheckForNull
	private String actionName;

	@CheckForNull
	private String fileLocation;
	
	@CheckForNull
	private String nodePath;
	
	@CheckForNull
	private String format;

	private boolean markFailed;
	private boolean showResults;

	@DataBoundConstructor
	public SweagleActionUpload(@CheckForNull String actionName, @CheckForNull String fileLocation, @CheckForNull String format, @CheckForNull String nodePath, boolean markFailed, boolean showResults) {
		this.actionName = Util.fixEmptyAndTrim(actionName);
		this.fileLocation=fileLocation;
		this.nodePath=nodePath;
		this.format=format;
		this.markFailed = markFailed;
		this.showResults = showResults;
				
	}


	public String getActionName() {
		return actionName;
	}


	public String getFileLocation() {
		return fileLocation;
	}
	
	public String getNodePath() {
		return nodePath;
	}
	
	public String getFormat() {
		return format;
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
	public DescriptorImpl_Upload getDescriptor() {
		return (DescriptorImpl_Upload) super.getDescriptor();
	}
	
	
	
	

	// Job Plugin execution code
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		final EnvVars env = run.getEnvironment(listener);
		DescriptorImpl_Upload descriptorImpl = getDescriptor();
		String sweagleURL = descriptorImpl.getSweagleURL();
		Secret sweagleAPIkey = descriptorImpl.getSweagleAPIkey();

		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		loggerUtils.info("Executing SWEAGLE Upload " + actionName + " at: " + sweagleURL);
		
		
		String actionResonse = null;
		
			
			actionResonse = SweagleUtils.uploadConfig(sweagleURL, sweagleAPIkey, fileLocation,  nodePath, listener);

		
		
		
		if (showResults)
		loggerUtils.debug(actionResonse);
		

	}
}
