package com.sweagle.jenkins.plugins;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;

public class SweagleEnvironmentContributionAction extends InvisibleAction implements EnvironmentContributingAction {

   
    
    private final String envVariable;
    
    private final String varValue;
    
    public String getEnvVariable(){
        return envVariable;
    }
    
    public String getVarValue() {
        return varValue;
    }
    
    public SweagleEnvironmentContributionAction(String envVariable, String varValue) {
        this.envVariable = envVariable;
        this.varValue = varValue;
    }
    

	@Override
	public void buildEnvironment(Run<?, ?> run, EnvVars env) {
		 if (env != null){
	            env.put(envVariable, varValue);
	       
	        }
		
	}

    
	
}
