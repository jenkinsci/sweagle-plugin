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
import java.util.ArrayList;

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
import hudson.tasks.BuildStepMonitor;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;

/**
 * 
 * If result is above it marks build as unstable.
 */
public class SweagleActionValidate extends hudson.tasks.Builder implements SimpleBuildStep {
	@CheckForNull
	private String actionName;

	@CheckForNull
	private String mdsName;

	private int warnMax = 0;
	private int errMax = 0;
	private boolean markFailed=false;
	private boolean noPending=false;
	private boolean showResults=false;
	long retryInterval = 10;
	int retryCount = 0;
	
	

	@DataBoundConstructor
	public SweagleActionValidate(@CheckForNull String actionName,  @CheckForNull String mdsName) {
		this.actionName = Util.fixEmptyAndTrim(actionName);
		this.mdsName = mdsName;

	}


	
	@DataBoundSetter
	public void setWarnMax(int warnMax) {
		this.warnMax = warnMax;
	}
	
	@DataBoundSetter
	public void setErrMax(int errMax) {
		this.errMax = errMax;
	}
	
	@DataBoundSetter
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}
	
	@DataBoundSetter
	public void setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
	}
	
	@DataBoundSetter
	public void setMarkFailed(boolean markFailed) {
		this.markFailed = markFailed;
	}

	@DataBoundSetter
	public void setNoPending(boolean noPending) {
		this.noPending = noPending;
	}
	
	@DataBoundSetter
	public void setShowResults(boolean showResults) {
		this.showResults = showResults;
	}


	public String getActionName() {
		return actionName;
	}


	public String getMdsName() {
		return mdsName;
	}


	
	public int getWarnMax() {
		return warnMax;
	}
	
	public long getRetryInterval() {
		return retryInterval;
	}
	
	public int getRetryCount() {
		return retryCount;
	}
	
	public int getErrMax() {
		return errMax;
	}
	

	public boolean getMarkFailed() {
		return markFailed;
	}

	public boolean getNoPending() {
		return noPending;
	}
	
	public boolean getShowResults() {
		return showResults;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public DescriptorImpl_Validate getDescriptor() {
		return (DescriptorImpl_Validate) super.getDescriptor();
	}

	
	
	
	// Job Plugin execution code
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		final EnvVars env = run.getEnvironment(listener);
		DescriptorImpl_Validate descriptorImpl = getDescriptor();
		String sweagleURL = descriptorImpl.getSweagleURL();
		Secret sweagleAPIkey = descriptorImpl.getSweagleAPIkey();
		if (sweagleURL == null) {throw new AbortException("Sweagle URL not set in Jenkins Configuration.");}

		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		loggerUtils.info("Executing SWEAGLE Validate Action: " + actionName + " at: " + sweagleURL);
		ArrayList<ValidatorStatus> validatorStatuses = new ArrayList<ValidatorStatus>();
		
		
		String mdsNameExp = env.expand(mdsName);
		if (retryInterval<10)
			retryInterval=10;
		String actionResonse = null;
		int retry=2;
		
			while (!SweagleUtils.validateProgress(mdsName, sweagleURL, sweagleAPIkey, markFailed, listener)&&(retry<retryCount||retryCount==-1)) {
				Thread.sleep(retryInterval*1000);
				retry ++;}
			if (SweagleUtils.validateProgress(mdsName, sweagleURL, sweagleAPIkey, markFailed, listener)) {
			//Generate Validation Report
			validatorStatuses=SweagleValidateReportUtils.buildValidatorStatuses(mdsNameExp, sweagleURL, sweagleAPIkey,  listener, showResults, run);
			ValidationReport validationReport = new ValidationReport(validatorStatuses, mdsName, run);
			run.addAction(validationReport);
			actionResonse = SweagleUtils.validateConfig(mdsNameExp, sweagleURL, sweagleAPIkey, markFailed,  warnMax, errMax, listener, showResults, run );
			}
			else {
				if (noPending)
					throw new AbortException("Pending data for " + mdsNameExp + " not found.");
				else 
					loggerUtils.info("Pending data for " + mdsNameExp + " not found.");	
			}
		if (showResults)
		loggerUtils.debug(actionResonse);
		
		

	}



	
}
