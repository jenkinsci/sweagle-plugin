package com.sweagle.jenkins.plugins;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.JsonPath;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SweagleValidateReportUtils {

	static OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(60, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build();

	static String getMdsId(String mdsName, String sweagleURL, Secret sweagleAPIkey, TaskListener listener,
			boolean showResults, Run<?, ?> run) throws InterruptedException, IOException {
		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		Request request = new Request.Builder().url(sweagleURL + "/api/v1/data/include/search?name=" + mdsName).get()
				.addHeader("cache-control", "no-cache").addHeader("Accept", "*/*")
				.addHeader("Authorization", "Bearer " + Secret.toString(sweagleAPIkey)).build();

		Response response = client.newCall(request).execute();
		String responseString = response.body().string();
		if (showResults)
			loggerUtils.debug(responseString);

		String mdsId = JsonPath.read(responseString, "_entities[0].master.id").toString();

		return mdsId;

	}

	static ArrayList<ValidatorStatus> getAssignedParsers(ArrayList<ValidatorStatus> validatorStatuses, String mdsId,
			String sweagleURL, Secret sweagleAPIkey, TaskListener listener, boolean showResults, Run<?, ?> run)
			throws InterruptedException, IOException {
		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);

		Request request = new Request.Builder()
				.url(sweagleURL + "/api/v1/tenant/metadata-parser/assigned?id=" + mdsId
						+ "&parserType=VALIDATOR&status=published")
				.get().addHeader("cache-control", "no-cache").addHeader("Accept", "*/*")
				.addHeader("Authorization", "Bearer " + Secret.toString(sweagleAPIkey)).build();

		Response response = client.newCall(request).execute();
		String responseString = response.body().string();

		JSONParser parser = new JSONParser();
		JSONObject responseStringJSON = null;
		try {
			responseStringJSON = (JSONObject) parser.parse(responseString);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (showResults)
			loggerUtils.debug(responseString);

		JSONArray entities = (JSONArray) responseStringJSON.get("_entities");
		int len = entities.size();
		for (int i = 0; i < len; i++) {
			JSONObject validatorData = (JSONObject) entities.get(i);
			validatorStatuses.add(new ValidatorStatus(validatorData.getAsString("name"), "Valid", ""));
		}

		return validatorStatuses;

	}

	private static ArrayList<ValidatorStatus> getValidationReport(ArrayList<ValidatorStatus> validatorStatuses,
			String mdsName, String sweagleURL, Secret sweagleAPIkey, TaskListener listener, boolean showResults,
			Run<?, ?> run) {
		
		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);

		Response response = null;
		loggerUtils.info("Creating Sweagle Validation Report for: " + mdsName);

		Request request = new Request.Builder()
				.url(sweagleURL + "/api/v1/data/include/validate?name=" + mdsName
						+ "&forIncoming=true&withCustomValidations=true")
				.get().addHeader("Accept", "application/json;charset=UTF-8")
				.addHeader("Authorization", "Bearer " + Secret.toString(sweagleAPIkey)).build();

		String responseString = null;
				
		
		try {
			response = client.newCall(request).execute();
			responseString = response.body().string();
			response.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int mdsErrors = JsonPath.read(responseString, "summary.errors");
		int mdsWarns = JsonPath.read(responseString, "summary.warnings");
		
		
		
		
		if (mdsErrors > 0) {
			JSONArray validatorErrors = JsonPath.read(responseString,"errors.failedParsers");
			for (int i = 0 ; i < validatorErrors.size(); i++) {
				
				String validatorName=JsonPath.read(validatorErrors.get(i), "validatorName");
			    String errorDescription=JsonPath.read(validatorErrors.get(i),"errorDescription");		
			
			for(ValidatorStatus d : validatorStatuses){
		        if(d.getValidatorName() != null && d.getValidatorName().contains(validatorName) ) {
		        
		        	d.setValidatorStatus("Error");
		        	d.setValidatorInfo(errorDescription);
		        }
		        
			}
		           
		    }
			
			
		}
		
		if (mdsWarns > 0) {
			LinkedHashMap<String, JSONArray> validatorWarnings = JsonPath.read(responseString,"warnings");
			final String responseStringFinal=responseString;
			
			validatorWarnings.forEach((key, value) -> {	
			JSONArray tempLoopWarn=JsonPath.read(responseStringFinal,"warnings."+key);
			
			for (int i = 0 ; i < tempLoopWarn.size(); i++) {
			String vPath= JsonPath.read(tempLoopWarn.get(i), "path");
			String vKey=  JsonPath.read(tempLoopWarn.get(i), "key");
			validatorStatuses.add(new ValidatorStatus(key, "Warning", vPath+ "  key: "+vKey));
			}
			
			});
		           
		    }
			
			
		
		
		
		
		return validatorStatuses;
	}

	public static ArrayList<ValidatorStatus> buildValidatorStatuses(String mdsName, String sweagleURL,
			Secret sweagleAPIkey, TaskListener listener, boolean showResults, Run<?, ?> run)
			throws InterruptedException, IOException {
		ArrayList<ValidatorStatus> validatorStatuses = new ArrayList<ValidatorStatus>();
		String mdsId = getMdsId(mdsName, sweagleURL, sweagleAPIkey, listener, showResults, run);
		validatorStatuses = getAssignedParsers(validatorStatuses, mdsId, sweagleURL, sweagleAPIkey, listener,
				showResults, run);
		validatorStatuses = getValidationReport(validatorStatuses, mdsName, sweagleURL, sweagleAPIkey, listener,
				showResults, run);

		return validatorStatuses;

	}

}
