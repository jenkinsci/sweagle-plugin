package com.sweagle.jenkins.plugins;

import java.util.ArrayList;



import hudson.model.Action;

import hudson.model.Run;

public class ValidationReport implements Action {

	Run<?,?> run;
	String mdsName;
	private ArrayList<ValidatorStatus> validatorStatuses;
	
	
	
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
        return this.run.number;
    }
    
    public String getMdsName() {
        return this.mdsName;
    }
    
    public Run<?,?> getBuild() {
        return this.run;
    }
    
    public ArrayList<ValidatorStatus> getValidatorStatuses() {
	    return  validatorStatuses;	   
    }
    

    
    public ValidationReport(final ArrayList<ValidatorStatus> validatorStatuses, Run<?,?> run )
    {
      
    	this.validatorStatuses = validatorStatuses;
    	this.run = run;
        
        
       
    }
	
}
