package com.sweagle.jenkins.plugins;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.JsonPath;

import hudson.EnvVars;
import hudson.FilePath;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
			loggerUtils.debug("getMdsId:"+responseString);

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
		JSONObject responseStringJSON = new JSONObject();
		try {
			responseStringJSON = (JSONObject) parser.parse(responseString);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (showResults) {
			loggerUtils.debug("get assigned parsers:" + responseString);
		}
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
			boolean stored, Run<?, ?> run) {
		
		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		boolean forIncoming=!stored;

		Response response = null;
		loggerUtils.info("Creating Sweagle Validation Report for: " + mdsName);

		Request request = new Request.Builder()
				.url(sweagleURL + "/api/v1/data/include/validate?name=" + mdsName
						+ "&forIncoming="+forIncoming+"&withCustomValidations=true")
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
		
		final String responseStringFinal=responseString;
		
		
		if (mdsErrors > 0) {
			
			
			LinkedHashMap<String, JSONArray> validatorErrors = JsonPath.read(responseString,"errors");
			validatorErrors.forEach((key, value) -> {	
			JSONArray tempLoopError = JsonPath.read(responseStringFinal,"errors."+key);
			if (key.equals("failedParsers")) {
			for (int i = 0 ; i < tempLoopError.size(); i++) {
				
				String validatorName=JsonPath.read(tempLoopError.get(i), "validatorName");
			    String errorDescription=JsonPath.read(tempLoopError.get(i),"errorDescription");		
			
			for(ValidatorStatus d : validatorStatuses){
		        if(d.getValidatorName() != null && d.getValidatorName().contains(validatorName) ) {
		        
		        	d.setValidatorStatus("Error");
		        	d.setValidatorInfo(key +": "+errorDescription);
		        	}
		        
				}
				}
				
				
				
				
		    }
			else {
				
				for (int i = 0 ; i < tempLoopError.size(); i++) {
					String vPath= JsonPath.read(tempLoopError.get(i), "path");
					String vKey=  JsonPath.read(tempLoopError.get(i), "key");
					String vValue= JsonPath.read(tempLoopError.get(i), "value");
					validatorStatuses.add(new ValidatorStatus(key, "Error", vPath+ "   key: "+vKey+  "  value: "+vValue));
					}	
				
				
			} 
			});
			
		}
		
		if (mdsWarns > 0) {
			LinkedHashMap<String, JSONArray> validatorWarnings = JsonPath.read(responseString,"warnings");
			
			
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
			Secret sweagleAPIkey, TaskListener listener, boolean showResults, boolean stored, Run<?, ?> run)
			throws InterruptedException, IOException {
		ArrayList<ValidatorStatus> validatorStatuses = new ArrayList<ValidatorStatus>();
		String mdsId = getMdsId(mdsName, sweagleURL, sweagleAPIkey, listener, showResults, run);
		validatorStatuses = getAssignedParsers(validatorStatuses, mdsId, sweagleURL, sweagleAPIkey, listener,
				showResults, run);
		validatorStatuses = getValidationReport(validatorStatuses, mdsName, sweagleURL, sweagleAPIkey, listener,
				showResults, stored, run);

		return validatorStatuses;

	}
	public static void writeJunitXmlFile(ArrayList<ValidatorStatus> list, FilePath workspace) {

		try {

			DocumentBuilderFactory dFact = DocumentBuilderFactory.newInstance();
			DocumentBuilder build = dFact.newDocumentBuilder();
			Document doc = build.newDocument();

			Element root = doc.createElement("testsuite");
			root.setAttribute("name","Sweagle Validators");
			doc.appendChild(root);

			for (ValidatorStatus validatorStatus : list) {

				Element testcase = doc.createElement("testcase");
				root.appendChild(testcase);
				testcase.setAttribute("name",validatorStatus.getValidatorName());
				if (!validatorStatus.getValidatorInfo().equals("")) {
					Element failure = doc.createElement("failure");
					failure.setAttribute("type",validatorStatus.getValidatorStatus());
					failure.setTextContent(validatorStatus.getValidatorInfo());
					testcase.appendChild(failure);
								}
			}

			// Save the document to the disk file
			TransformerFactory tranFactory = TransformerFactory.newInstance();
			Transformer aTransformer = tranFactory.newTransformer();

			// format the XML nicely
			aTransformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");

			aTransformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "4");
			aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");

			DOMSource source = new DOMSource(doc);
			try {
				// location and name of XML file you can change as per need
				FileWriter fos = new FileWriter(String.valueOf(workspace.child("sweagle-validation.xml")));
				StreamResult result = new StreamResult(fos);
				aTransformer.transform(source, result);

			} catch (IOException e) {

				e.printStackTrace();
			}

		} catch (TransformerException ex) {
			System.out.println("Error outputting document");

		} catch (ParserConfigurationException ex) {
			System.out.println("Error building document");
		}
	}

}
