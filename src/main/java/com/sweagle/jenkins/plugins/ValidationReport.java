package com.sweagle.jenkins.plugins;

import hudson.model.Action;

import hudson.model.Run;

public class ValidationReport implements Action {

	Run<?,?> build;
	String mdsName;
	
    @Override
    public String getIconFileName() {
        return "/plugin/sweagle/images/sweagle2.png";
    }

    @Override
    public String getDisplayName() {
        return "Sweagle Validation Report";
    }

    @Override
    public String getUrlName() {
        return "ValidationReport";
    }
    
    public int getBuildNumber() {
        return this.build.number;
    }
    
    public String getMdsName() {
        return this.mdsName;
    }
    
    public Run<?,?> getBuild() {
        return this.build;
    }

    
    public ValidationReport(Run<?,?> build, String mdsName)
    {
      
        this.build = build;
        this.mdsName = mdsName;
    }
	
}
