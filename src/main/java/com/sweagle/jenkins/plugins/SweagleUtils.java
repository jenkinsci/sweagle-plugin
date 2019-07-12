package com.sweagle.jenkins.plugins;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;

import hudson.model.Build;
import hudson.model.Executor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import hudson.AbortException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SweagleUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(SweagleUtils.class);

	static String validateConfig(String mdsName, String sweagleURL, Secret sweagleAPIkey, boolean markFailed,
			int warnMax, int errMax, TaskListener listener, boolean showResults, Run<?, ?> run) throws InterruptedException, AbortException {

		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		OkHttpClient client = new OkHttpClient();
		Response response = null;
		loggerUtils.info("Checking MDS Validity: " + mdsName);

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
		loggerUtils.info(mdsName + " contains " + mdsWarns + " warnings and "+mdsErrors+" errors");
		if (mdsErrors > errMax & errMax != -1) {
			if (showResults)
				loggerUtils.debug(responseString);
			if (markFailed)
			throw new AbortException(" Errors: " + mdsErrors + " Exceeds error threshold: " + errMax);
		}
		if (mdsWarns > warnMax & warnMax != -1) {
			if (showResults)
				loggerUtils.debug(responseString);
			if (markFailed)
			throw new AbortException(" Warnings: " + mdsWarns + " Exceeds warning threshold: " + warnMax);
		}
		return responseString;

	}

	static String uploadConfig(String sweagleURL, Secret sweagleAPIkey, String fileLocation, String nodePath, TaskListener listener ) {
		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		loggerUtils.info("Uploading Config from " + fileLocation + " to " + nodePath);
		String responseString = null;
		try {
			String content = readFile(fileLocation, Charset.defaultCharset());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return responseString;
	}

	static String snapshotConfig(String mdsName, String sweagleURL, Secret sweagleAPIkey, String description, String tag,  TaskListener listener) throws UnsupportedEncodingException {
		PrintStream logger = listener.getLogger();
		LoggerUtils loggerUtils = new LoggerUtils(logger);
		loggerUtils.info("Creating Snapshot from pending data for " + mdsName);
		String responseString = null;
		
		OkHttpClient client = new OkHttpClient();
		Response response = null;
		RequestBody body = RequestBody.create(null, new byte[0]);
		Request request = new Request.Builder()
		  .url(sweagleURL + "/api/v1/data/include/snapshot/byname?name="+mdsName+"&level=none&description="+URLEncoder.encode(description, "UTF-8").replace("+", "%20")+"&tag="+URLEncoder.encode(tag, "UTF-8").replace("+", "%20"))
		  .post(body)
		  .addHeader("Authorization", "Bearer " + Secret.toString(sweagleAPIkey))
		  .addHeader("Accept", "*/*")
		  .build();

		try {
			response = client.newCall(request).execute();
			responseString = response.body().string();
			response.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseString;
	}

	static boolean exportConfig(String mdsName, String exporter, String format) {
		return true;
	}
	
	static String readFile(String path, Charset encoding) 
			  throws IOException 
			{
			  byte[] encoded = Files.readAllBytes(Paths.get(path));
			  return new String(encoded, encoding);
			}
}
