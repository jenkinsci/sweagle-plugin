package com.sweagle.jenkins.plugins;


import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;



public class SweagleUtils {

	static OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(60, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build();

	static String validateConfig(String mdsName, String sweagleURL, Secret sweagleAPIkey, boolean markFailed,
			int warnMax, int errMax, TaskListener listener, boolean showResults,  boolean stored,  Run<?, ?> run)
			throws InterruptedException, IOException {

		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		boolean forIncoming=!stored;
		
		Response response = null;
		if (stored)
		loggerUtils.info("Checking Stored CDS Validity: " + mdsName);
		else
		loggerUtils.info("Checking Pending CDS Validity: " + mdsName);	
		final EnvVars env = run.getEnvironment(listener);
		mdsName = env.expand(mdsName);
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
		loggerUtils.info(mdsName + " contains " + mdsWarns + " warnings and " + mdsErrors + " errors");
		if (mdsErrors > errMax & errMax != -1) {
			//if (showResults)
				loggerUtils.info("validateResults:"+responseString);
			
			if (markFailed)
				throw new AbortException(" Errors: " + mdsErrors + " Exceeds error threshold: " + errMax);
		}
		if (mdsWarns > warnMax & warnMax != -1) {
			if (showResults)
				loggerUtils.debug("validateResults:"+responseString);
			if (markFailed)
				throw new AbortException(" Warnings: " + mdsWarns + " Exceeds warning threshold: " + warnMax);
		}
		return responseString;

	}

	static String uploadConfig(String sweagleURL, Secret sweagleAPIkey, FilePath fileName, String nodePath, String format,
		boolean allowDelete, boolean onlyParent, boolean filenameNodes, String identifierWords, boolean autoRecognize, boolean markFailed, FilePath workspace, TaskListener listener,
		boolean showResults, int changeset, EnvVars env) throws AbortException, UnsupportedEncodingException {
		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		loggerUtils.info("Uploading Config from " + fileName + " to " + nodePath);
		String responseString = null;
		String content = null;

		try {
	
			content = fileName.readToString();
		
		} catch (InterruptedException | IOException e) {
			if (markFailed)
				throw new AbortException(e.toString());
			else
				loggerUtils.error(e.toString());
		}

		if (filenameNodes) {
			nodePath = nodePath + "," + fileName;
			nodePath = nodePath.replace(workspace.toString(),""); //ensure nodepath relative to workspace only
			nodePath = nodePath.replace("/", ",");
			nodePath = nodePath.replace("\\", ",");	
			nodePath = nodePath.replace(",,", ",");		
			if (showResults)	
				loggerUtils.debug("uploading nodePath:" +nodePath);
		}

		MediaType mediaType = MediaType.parse("text/plain");
		RequestBody body = RequestBody.create(mediaType, content);
		Request request = new Request.Builder()
				.url(sweagleURL + "/api/v1/data/bulk-operations/dataLoader/upload?nodePath=" + nodePath + "&format="
						+ format + "&allowDelete=" + allowDelete + "&validationLevel=error" + "&changeset=" + changeset
						+ "&autoRetry=true" + "&onlyParent=" + onlyParent + "&autoRecognize="+ autoRecognize +"&identifierWords="+identifierWords)
				.post(body).addHeader("Authorization", "Bearer " + Secret.toString(sweagleAPIkey))
				.addHeader("Accept", "*/*").addHeader("Cache-Control", "no-cache").addHeader("Connection", "keep-alive")
				.build();

		try {
			Response response = client.newCall(request).execute();
			responseString = response.body().string();
			if (showResults) {
				loggerUtils.debug("Upload request:" + request.toString());
				loggerUtils.debug("Upload response code: " + response.code() + "  " + response.body());
				loggerUtils.debug("Upload Response:"+response.toString());
			}
			if (response.code() > 299 && markFailed) {
				throw new AbortException("Error " + response.code() + "  " + responseString);
			}
			response.close();
		} catch (IOException e) {
			if (markFailed)
				throw new AbortException(e.toString());
			else
				loggerUtils.error(e.toString());
		}

		return responseString;
	}

	static String snapshotConfig(String mdsName, String sweagleURL, Secret sweagleAPIkey, String description,
			String tag, boolean markFailed, TaskListener listener) throws UnsupportedEncodingException {
		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		loggerUtils.info("Creating Snapshot from pending data for " + mdsName);
		String responseString = null;

		Response response = null;
		RequestBody body = RequestBody.create(null, new byte[0]);
		Request request = new Request.Builder()
				.url(sweagleURL + "/api/v1/data/include/snapshot/byname?name=" + mdsName + "&level=none&description="
						+ URLEncoder.encode(description, "UTF-8").replace("+", "%20") + "&tag="
						+ URLEncoder.encode(tag, "UTF-8").replace("+", "%20"))
				.post(body).addHeader("Authorization", "Bearer " + Secret.toString(sweagleAPIkey))
				.addHeader("Accept", "*/*").build();

		try {
			response = client.newCall(request).execute();
			responseString = response.body().string();
			if (response.code() > 299 && markFailed) {
				throw new AbortException("Error " + response.code() + "  " + responseString);
			}
			response.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return responseString;
	}

	static String exportConfig(String sweagleURL, Secret sweagleAPIkey, String mdsName, String fileLocation,
			String exporter, String args, String format, String tag, boolean markFailed, FilePath workspace, TaskListener listener, EnvVars env)
			throws AbortException {
		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		loggerUtils.info("Exporting from " + mdsName + " " +tag + " with exporter " + exporter + " in format " + format + " at "
				+ sweagleURL);
		String responseString = null;
		Response response = null;
		

		MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
		RequestBody body = RequestBody.create(mediaType,
				"mds=" + mdsName + "&parser=" + exporter + "&args=" + args + "&format=" + format+ "&tag="+tag);
		Request request = new Request.Builder().url(sweagleURL + "/api/v1/tenant/metadata-parser/parse").post(body)
				.addHeader("Authorization", "Bearer " + Secret.toString(sweagleAPIkey)).addHeader("Accept", "*/*")
				.addHeader("content-type", "application/x-www-form-urlencoded").addHeader("Connection", "keep-alive")
				.addHeader("cache-control", "no-cache").build();

		try {
			response = client.newCall(request).execute();
			responseString = response.body().string();
			if (response.code() > 299 && markFailed) {
				throw new AbortException("Error " + response.code() + "  " + responseString);
			}
			response.close();
		} catch (Exception e) {
			if (markFailed)
				throw new AbortException(e.toString());
			else
				loggerUtils.error(e.toString());
		}

		String content = responseString;
        FilePath destination = workspace.child(fileLocation);
        
		
		try {
 
			destination.write(content, "UTF-8");
			
		} catch (Exception e) {
			if (markFailed)
				throw new AbortException(e.toString());
			else
				loggerUtils.error(e.toString());
		}

		return responseString;
	}

	static boolean validateProgress(String mdsName, String sweagleURL, Secret sweagleAPIkey, boolean markFailed, boolean stored,
			TaskListener listener) throws AbortException, InterruptedException {
		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		String responseString = null;
		Response response = null;
		boolean forIncoming=!stored;
		Request request = new Request.Builder()
				.url(sweagleURL + "/api/v1/data/include/validation_progress?name=" + mdsName + "&forIncoming="+forIncoming)
				.get().addHeader("Authorization", "Bearer " + Secret.toString(sweagleAPIkey)).addHeader("Accept", "*/*")
				.build();

		try {
			response = client.newCall(request).execute();
			responseString = response.body().string();
			response.close();
		} catch (Exception e) {
			if (markFailed)
				throw new AbortException(e.toString());
			else
				loggerUtils.error(e.toString());
		}

		if (responseString.contains("FINISHED")) {
			if (stored)
			loggerUtils.info("Checking stored validation status for " + mdsName + " validation complete ");
			else
			loggerUtils.info("Checking pending validation progress for " + mdsName + " validation complete ");
			return true;

		} else {
			if (stored)
				loggerUtils.info("Checking stored validation status for " + mdsName + " status:" + responseString);
			else
				loggerUtils.info("Checking pending validation progress for " + mdsName + " status:" + responseString);
			return false;
		}
	}

	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	static String getErrorfromResponse(String responseAsString) {
		String errorMessage = "";
		if (responseAsString.isEmpty()) {
			return "";
		} else {
			try {
				errorMessage = JsonPath.read(responseAsString, "$.error");
			} catch (Exception e) {
				errorMessage = "Not a valid response from SWEAGLE API " + responseAsString;
			}

			return errorMessage;
		}
	}

	public static int createChangeSet(String sweagleURL, Secret sweagleAPIkey, boolean showResults, String description,
			TaskListener listener, EnvVars env) throws IOException, InterruptedException {

		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		String title = null;

		try {
			title = URLEncoder.encode("Jenkins Upload Changeset ", "UTF-8");
			if (description == null || description.equals("") || description.isEmpty())
				description = URLEncoder.encode(env.get("JOB_NAME") + "Build Number: " + env.get("BUILD_NUMBER"),
						"UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		final RequestBody requestBody = RequestBody.create((MediaType) null, "");
		final Request createChangeSet = new Request.Builder()
				.url(sweagleURL + "/api/v1/data/changeset?title=" + title + "&description=" + description)
				.post(requestBody).addHeader("Accept", "*/*")
				.addHeader("Authorization", "Bearer " + Secret.toString(sweagleAPIkey)).build();
		String createChangeStResponseString = null;
		try {
			final Response createChangeStResponse = client.newCall(createChangeSet).execute();
			createChangeStResponseString = createChangeStResponse.body().string();
		} catch (IOException e2) {
			e2.printStackTrace();
		}

		if (showResults)
			loggerUtils.debug("create changeset: " + createChangeStResponseString);

		final int changeSetId = (int) JsonPath.read(createChangeStResponseString, "$.id", new Predicate[0]);
		return changeSetId;
	}

	public static void approveChangeSet(String sweagleURL, Secret sweagleAPIkey, String tag, String description,
			boolean withSnapshot, int changeSetId, boolean showResults, TaskListener listener, EnvVars env)
			throws UnsupportedEncodingException {

		if (description == null || description.equals("") || description.isEmpty()) {
			description = "Jenkins Job: " + env.get("JOB_NAME") + "  Build Number: " + env.get("BUILD_NUMBER");
		}
		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		final RequestBody requestBody = RequestBody.create((MediaType) null, "");
		final Request approveChangeSet = new Request.Builder()
				.url(sweagleURL + "/api/v1/data/changeset/" + changeSetId + "/approve" + "?tag="
						+ URLEncoder.encode(tag, "UTF-8").replace("+", "%20") + "&description="
						+ URLEncoder.encode(description, "UTF-8").replace("+", "%20") + "&snapshotDescription="
						+ URLEncoder.encode(description, "UTF-8").replace("+", "%20") + "&withSnapshot=" + withSnapshot)
				.post(requestBody).addHeader("Accept", "*/*")
				.addHeader("Authorization", "Bearer " + Secret.toString(sweagleAPIkey)).build();
		String approveChangeStResponseString = null;
		try {
			final Response approveChangeStResponse = client.newCall(approveChangeSet).execute();
			approveChangeStResponseString = approveChangeStResponse.body().string();

		} catch (IOException e2) {
			e2.printStackTrace();
		}
		if (showResults)
			loggerUtils.debug("approve changeset: " + approveChangeStResponseString);

	}
	
	static String getMdsValue(String sweagleURL, Secret sweagleAPIkey, String mdsName, 
			String tag, String jsonPath, boolean showResults, boolean markFailed, TaskListener listener)
			throws AbortException {
		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		loggerUtils.info("Retreiving value for " + jsonPath + " in " + mdsName );
		String responseString = null;
		Response response = null;
		

		MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
		RequestBody body = RequestBody.create(mediaType,
				"mds=" + mdsName + "&parser=All" + "&format=JSON"+"&tag="+tag);
		Request request = new Request.Builder().url(sweagleURL + "/api/v1/tenant/metadata-parser/parse").post(body)
				.addHeader("Authorization", "Bearer " + Secret.toString(sweagleAPIkey)).addHeader("Accept", "*/*")
				.addHeader("content-type", "application/x-www-form-urlencoded").addHeader("Connection", "keep-alive")
				.addHeader("cache-control", "no-cache").build();

		try {
			response = client.newCall(request).execute();
			responseString = response.body().string();
			if (response.code() > 299 && markFailed) {
				throw new AbortException("Error " + response.code() + "  " + responseString);
			}
			response.close();
			if(showResults)
				loggerUtils.debug(mdsName+": "+responseString);
			
			
		} catch (Exception e) {
			if (markFailed)
				throw new AbortException(e.toString());
			else
				loggerUtils.error(e.toString());
		}

		String content = JsonPath.parse(responseString).read(jsonPath).toString();
		if (content.contentEquals("")||content.isEmpty()) {
			if (markFailed) 
				throw new AbortException("Json Path: "+ jsonPath + " not found in "+ mdsName);
			 else
				loggerUtils.error("Json Path: "+ jsonPath + " not found in "+ mdsName);
						
		}
		
		
		return content;
		
		
		}
	
	
	

}
