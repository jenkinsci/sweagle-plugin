package com.sweagle.jenkins.plugins;

public class ValidatorStatus {
	
	private String validatorName;
	private String validatorStatus;
	private String validatorInfo;
	
	public ValidatorStatus(String validatorName, String validatorStatus, String validatorInfo) {
	
		{
			this.validatorName = validatorName;
			this.validatorStatus = validatorStatus;
			this.validatorInfo = validatorInfo;
		
		}
		
	}

	
	 public String getValidatorName() {
	        return this.validatorName;
	        }
	 public String getValidatorStatus() {
	        return this.validatorStatus;
	        }
	 
	 public void setValidatorStatus(String validatorStatus) {
		    this.validatorStatus = validatorStatus;
		  }
	 
	 public String getValidatorInfo() {
	        return this.validatorInfo;
	        }
	 
	 public void setValidatorInfo(String validatorInfo) {
		    this.validatorInfo = validatorInfo;
		  }

}
