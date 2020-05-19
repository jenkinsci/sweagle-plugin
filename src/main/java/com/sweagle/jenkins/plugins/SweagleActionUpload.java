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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;


import javax.annotation.CheckForNull;

import java.util.Arrays;

import org.apache.commons.lang.SystemUtils;
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
import jenkins.model.Jenkins;
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
	private boolean allowDelete = false;
	private boolean onlyParent = false;
	private boolean filenameNodes = false;
	private boolean subDirectories = false;
	private boolean withSnapshot = false;
	private boolean autoRecognize = false;
	private String tag;
	private String identifierWords;
	private String description;
	private boolean markFailed = false;
	private boolean showResults = false;

	@DataBoundConstructor
	public SweagleActionUpload(@CheckForNull String actionName, @CheckForNull String fileLocation,
			@CheckForNull String format, @CheckForNull String nodePath) {
		this.actionName = Util.fixEmptyAndTrim(actionName);
		this.fileLocation = fileLocation;
		this.nodePath = nodePath;
		this.format = format;

	}

	@DataBoundSetter
	public void setAllowDelete(boolean allowDelete) {
		this.allowDelete = allowDelete;
	}

	@DataBoundSetter
	public void setOnlyParent(boolean onlyParent) {
		this.onlyParent = onlyParent;
	}

	@DataBoundSetter
	public void setFilenameNodes(boolean filenameNodes) {
		this.filenameNodes = filenameNodes;
	}

	@DataBoundSetter
	public void setSubDirectories(boolean subDirectories) {
		this.subDirectories = subDirectories;
	}

	@DataBoundSetter
	public void setWithSnapshot(boolean withSnapshot) {
		this.withSnapshot = withSnapshot;
	}
	
	@DataBoundSetter
	public void setAutoRecognize(boolean autoRecognize) {
		this.autoRecognize = autoRecognize;
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
	public void setDescription(String description) {
		this.description = description;
	}
	
	@DataBoundSetter
	public void setIdentifierWords(String identifierWords) {
		this.identifierWords = identifierWords;
	}	
	
	// getters

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

	public boolean getAllowDelete() {
		return allowDelete;
	}

	public boolean getOnlyParent() {
		return onlyParent;
	}

	public boolean getFilenameNodes() {
		return filenameNodes;
	}

	public boolean getSubDirectories() {
		return subDirectories;
	}

	public boolean getWithSnapshot() {
		return withSnapshot;
	}
	
	public boolean getAutoRecognize() {
		return autoRecognize;
	}

	public String getTag() {
		return tag;
	}

	public String getDescription() {
		return description;
	}
	
	public String getIdentifierWords() {
		return identifierWords;
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
		Jenkins jenkins = Jenkins.getInstance();
		DescriptorImpl_Validate descriptorImpl = jenkins.getDescriptorByType(DescriptorImpl_Validate.class);
		String sweagleURL = descriptorImpl.getSweagleURL();
		Secret sweagleAPIkey = descriptorImpl.getSweagleAPIkey();
		if (sweagleURL == null) {throw new AbortException("Sweagle URL not set in Jenkins Configuration.");}

		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		loggerUtils.info("Executing SWEAGLE Upload " + actionName + " at: " + sweagleURL);

		String fileLocationExp = env.expand(fileLocation);
		String nodePathExp = env.expand(nodePath);
		String descriptionEnv = env.expand(description);
		String identifierWordsEnv = env.expand(identifierWords);
		String tagEnv = env.expand(tag);
		String actionResonse = null;

		if (subDirectories) {
			String separator = "/";
			if (SystemUtils.IS_OS_WINDOWS)
				separator = "\\";

			String[] fileLocationExps = fileLocationExp.split(separator);
			fileLocationExps[fileLocationExps.length - 1] = "**" + File.separator
					+ fileLocationExps[fileLocationExps.length - 1];
			fileLocationExp = Arrays.toString(fileLocationExps).replace("[", "").replace("]", "")
					.replace(",", File.separator).replace(" ", "");
	
			loggerUtils.debug(fileLocationExp);
			
		}
		if (showResults)
			loggerUtils.debug("DirectoryScanner setIncludes: " + fileLocationExp);

		// remote slave support
			
		FilePath[] files=workspace.list(fileLocationExp);


		loggerUtils.debug(" Found: " + files.length + " files.");
		if (files.length > 0) {

			int changeSetId = SweagleUtils.createChangeSet(sweagleURL, sweagleAPIkey, showResults, descriptionEnv,
					listener, env);

			for (FilePath file : files) {

				actionResonse = SweagleUtils.uploadConfig(sweagleURL, sweagleAPIkey, file, nodePathExp, format,
						allowDelete, onlyParent, filenameNodes, identifierWordsEnv, autoRecognize, markFailed, workspace, listener, showResults, changeSetId, env);
			}

			SweagleUtils.approveChangeSet(sweagleURL, sweagleAPIkey, tagEnv, descriptionEnv, withSnapshot, changeSetId,
					showResults, listener, env);

			if (showResults)
				loggerUtils.debug(actionResonse);
		} else {
			if (markFailed) {
				throw new AbortException("No matching files");
			} else {
				loggerUtils.info("No matching files");
			}
		}

	}
}
