/*******************************************************************************
 * Copyright 2015-16 AutoCognite Testing Research Pvt Ltd
 * 
 * Website: www.AutoCognite.com
 * Email: support [at] autocognite.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.autocognite.pvt.unitee.config;

import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;

import com.autocognite.arjuna.annotations.DataGenerator;
import com.autocognite.arjuna.annotations.DataMethodContainer;
import com.autocognite.arjuna.enums.LoggingLevel;
import com.autocognite.arjuna.interfaces.DataSource;
import com.autocognite.arjuna.interfaces.Value;
import com.autocognite.arjuna.utils.FileSystemBatteries;
import com.autocognite.pvt.arjuna.enums.ArjunaProperty;
import com.autocognite.pvt.arjuna.enums.PickerTargetType;
import com.autocognite.pvt.arjuna.enums.TestPickerProperty;
import com.autocognite.pvt.batteries.cli.CLIConfigurator;
import com.autocognite.pvt.batteries.config.Batteries;
import com.autocognite.pvt.batteries.console.Console;
import com.autocognite.pvt.batteries.enums.BatteriesPropertyType;
import com.autocognite.pvt.batteries.hocon.HoconConfigObjectReader;
import com.autocognite.pvt.batteries.hocon.HoconFileReader;
import com.autocognite.pvt.batteries.hocon.HoconReader;
import com.autocognite.pvt.batteries.hocon.HoconResourceReader;
import com.autocognite.pvt.batteries.hocon.HoconStringReader;
import com.autocognite.pvt.batteries.lib.ComponentIntegrator;
import com.autocognite.pvt.batteries.logging.Log;
import com.autocognite.pvt.batteries.property.ConfigPropertyBatteries;
import com.autocognite.pvt.batteries.value.StringValue;
//import com.autocognite.pvt.uiautomator.UiAutomator;
import com.autocognite.pvt.unitee.reporter.lib.CentralExecutionState;
import com.autocognite.pvt.unitee.reporter.lib.Reporter;
import com.autocognite.pvt.unitee.testobject.lib.loader.group.PickerMisConfiguration;
import com.autocognite.pvt.unitee.testobject.lib.loader.group.TestGroupsDB;
import com.autocognite.pvt.unitee.testobject.lib.loader.session.MSession;
import com.autocognite.pvt.unitee.testobject.lib.loader.session.Session;
import com.autocognite.pvt.unitee.testobject.lib.loader.session.UserDefinedSession;
import com.typesafe.config.ConfigObject;

public enum ArjunaSingleton {
	INSTANCE;
	private String version = "0.0b9";

	private HashMap<String,String> cliHashMap = null;
	private HashMap<String, HashMap<String,String>> testBucketProps = new HashMap<String, HashMap<String,String>>();

	private String[] cliArgs;
	
	boolean lazyAssertions = false;
	boolean lazyAssertionProcessed = false;
	
	private DataMethodContainerMap dataMethodContainers =  new DataMethodContainerMap();
	private DataGeneratorMap dataGenerators =  new DataGeneratorMap();
	private Reporter reporter = null;

	private CentralExecutionState execState;
	private TestGroupsDB groupsDB = null;
	private CLIConfigurator cliConfigurator = null;
	private Session session = null;
	
	private String name = "Arjuna Pro Platform Edition";
	
	ComponentIntegrator integrator;
	
	private Map<TestPickerProperty,String> cliPickerOptions = null;

	public void setName(String name){
		this.name = name;
	}
	
	public void setVersion(String version){
		this.version = version;
	}
	
	public void setCliConfigurator(CLIConfigurator cliConfigurator) {
		this.cliConfigurator = cliConfigurator;
	}
	
	public void init() throws Exception{
		String refPath = FileSystemBatteries.getAbsolutePathFromJar(FileSystemBatteries.getJarFilePathForObject(this), "./../../..");
		Batteries.init(refPath);
		//Batteries.addConfigurator(UiAutomator.getComponentConfigurator());
		ArjunaConfigurator uConf = new ArjunaConfigurator();
		Batteries.addConfigurator(uConf);
		Batteries.processConfigDefaults();
		integrator = uConf.getIntegrator();
		//integrator.enumerate();
		HoconReader reader = new HoconResourceReader(this.getClass().getResourceAsStream("/com/autocognite/pvt/text/autocognite_defaults_updated.conf"));
		reader.process();		
		integrator.setProjectDir(Batteries.getBaseDir());
		Batteries.processConfigProperties(reader.getProperties());
		//integrator.enumerate();
		
		// arjuna file
		HoconReader reader2 = new HoconFileReader(integrator.value(BatteriesPropertyType.DIRECTORY_CONFIG).asString() + "/" + integrator.value(BatteriesPropertyType.CONFIG_FILE_NAME).asString());
		reader2.process();
		
		ConfigObject configObj = reader2.getConfig().getObject("config");
		HoconReader configReader = new HoconConfigObjectReader(configObj);
		configReader.process();
		Batteries.processConfigProperties(configReader.getProperties());
		
		ConfigObject udvObj = reader2.getConfig().getObject("udv");
		HoconReader udvReader = new HoconConfigObjectReader(udvObj);
		udvReader.process();
		Batteries.processCentralUDVProperties(udvReader.getProperties());
		
		//CLI
		cliConfigurator.setIntegrator(integrator);
		cliConfigurator.setArgs(cliArgs);
		cliConfigurator.processUserOptions();
		HashMap<String,Value> options = cliConfigurator.getUserOptions();
		Batteries.processConfigProperties(options);

		HashMap<String, Value> updateOptions = new HashMap<String, Value>();
		String reportDir = integrator.value(ArjunaProperty.DIRECTORY_REPORT).asString();
		String runID =  integrator.value(ArjunaProperty.RUNID).asString();
		String timestampedRunID = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss").format(new Date()) + "-" + runID;
		String runIDReportDir = integrator.value(ArjunaProperty.DIRECTORY_RUNID_REPORT_ROOT).asString()
				.replace("%%slugREPORT_DIR", reportDir)
				.replace("%%slugRUNID", timestampedRunID);
		updateOptions.put(
				ConfigPropertyBatteries.enumToPropPath(ArjunaProperty.DIRECTORY_RUNID_REPORT_ROOT),
				new StringValue(runIDReportDir)
		);
		
		String rawJsonReportDir = integrator.value(ArjunaProperty.DIRECTORY_RUNID_REPORT_JSON_RAW_ROOT).asString()
				.replace("%%slugRUNID_RPT_DIR", runIDReportDir);
		updateOptions.put(
				ConfigPropertyBatteries.enumToPropPath(ArjunaProperty.DIRECTORY_RUNID_REPORT_JSON_RAW_ROOT),
				new StringValue(rawJsonReportDir)
		);
		
		updateOptions.put(
				ConfigPropertyBatteries.enumToPropPath(ArjunaProperty.DIRECTORY_RUNID_REPORT_JSON_RAW_EVENTS),
				new StringValue(integrator.value(ArjunaProperty.DIRECTORY_RUNID_REPORT_JSON_RAW_EVENTS).asString()
						.replace("%%slugRAW_DIR", rawJsonReportDir))
		);
		
		updateOptions.put(
				ConfigPropertyBatteries.enumToPropPath(ArjunaProperty.DIRECTORY_RUNID_REPORT_JSON_RAW_TESTS),
				new StringValue(integrator.value(ArjunaProperty.DIRECTORY_RUNID_REPORT_JSON_RAW_TESTS).asString()
						.replace("%%slugRAW_DIR", rawJsonReportDir))
		);
		
		updateOptions.put(
				ConfigPropertyBatteries.enumToPropPath(ArjunaProperty.DIRECTORY_RUNID_REPORT_JSON_RAW_ISSUES),
				new StringValue(integrator.value(ArjunaProperty.DIRECTORY_RUNID_REPORT_JSON_RAW_ISSUES).asString()
						.replace("%%slugRAW_DIR", rawJsonReportDir))
		);
		
		updateOptions.put(
				ConfigPropertyBatteries.enumToPropPath(ArjunaProperty.DIRECTORY_RUNID_REPORT_JSON_RAW_FIXTURES),
				new StringValue(integrator.value(ArjunaProperty.DIRECTORY_RUNID_REPORT_JSON_RAW_FIXTURES).asString()
						.replace("%%slugRAW_DIR", rawJsonReportDir))
		);
		
		updateOptions.put(
				ConfigPropertyBatteries.enumToPropPath(ArjunaProperty.SESSION_NAME),
				new StringValue("msession")
		);
		
		Batteries.processConfigProperties(updateOptions);
		
		Log log = new Log();
		log.configure(
				Level.toLevel(integrator.value(BatteriesPropertyType.LOGGING_CONSOLE_LEVEL).asString()),
				Level.toLevel(integrator.value(BatteriesPropertyType.LOGGING_FILE_LEVEL).asString()),
				integrator.value(BatteriesPropertyType.LOGGING_NAME).asString(),
				integrator.value(BatteriesPropertyType.DIRECTORY_LOG).asString()
		);
		
		groupsDB = new TestGroupsDB();
		
		String sessionName = integrator.value(ArjunaProperty.SESSION_NAME).asString();
		
		SessionCreator sCreator = null;
		try{
			sCreator = new SessionCreator(integrator, this.cliPickerOptions, sessionName);
		} catch (PickerMisConfiguration e){
			displayPickerConfigError();
		} catch (Exception e){
			throw e;
		}
		
		session = sCreator.getSession();
		if (session.getConfigObject() != null){
			HoconReader cReader = new HoconStringReader(session.getConfigObject().toString());
			cReader.process();
			Batteries.processConfigProperties(cReader.getProperties());	
		}
		
		if (session.getUDVObject() != null){
			HoconReader sessionUDVReader = new HoconStringReader(session.getUDVObject().toString());
			sessionUDVReader.process();
			Batteries.processCentralUDVProperties(sessionUDVReader.getProperties());
		}
		
	}
	
	public void freeze() throws Exception{
		// Post processing
		Batteries.freezeCentralConfig();
		integrator.enumerate();
		initlogger();
	}
	
	public void loadSession() throws Exception{
		session.setUDVs(Batteries.cloneCentralUDVs());
		try{
			groupsDB.createGroupForCLIOptions(this.cliPickerOptions);
		} catch (PickerMisConfiguration e){
			e.printStackTrace();
			displayPickerConfigError();
		} catch (Exception e){
			e.printStackTrace();
			throw e;
		}
		groupsDB.createAllCapturingGroup();
		groupsDB.createUserDefinedGroups();
		session.load();
	}
	
	private void displayPickerConfigError() throws Exception{
		Console.displayError("Your test picker switches are not valid.");
		Console.displayError("Evaluate your usage in the light of following rules.");
		Console.displayError("Note: -pn, and -cn switches take single and actual name as argument.");
		Console.displayError("Note: -i* and -c* switches take multiple comma separated names or regex patterns.");
		Console.displayError("Package Picker Valid Switch Combinations");
		Console.displayError("----------------------------------------");
		Console.displayError("-cp");
		Console.displayError("-pn");
		Console.displayError("-ip");
		Console.displayError("-cp -ip");
		Console.displayError("");
		Console.displayError("Class Picker Valid Switch Combinations");
		Console.displayError("--------------------------------------");
		Console.displayError("-pn -cc");
		Console.displayError("-pn -cn");
		Console.displayError("-pn -ic");
		Console.displayError("");
		Console.displayError("Method Picker Valid Switch Combinations");
		Console.displayError("--------------------------------------");
		Console.displayError("-pn -cn -cm");
		Console.displayError("-pn -cn -im");
		Console.displayError("");
		this.cliConfigurator.help();
		System.exit(1);		
		
	}
		
	public String getVersion() {
		return version;
	}
	
	private void initlogger() throws Exception {
		Log log = new Log();
		log.configure(
				Batteries.getDisplayLevel(),
				Batteries.getLogLevel(),
				Batteries.getCentralLogName(),
				Batteries.getLogDir()
					);
	}
	
	public void printUniteeHeader(){
		Console.display("   ___         _                      ");
		Console.display("  / _ \\       (_)                     ");
		Console.display(" / /_\\ \\ _ __  _  _   _  _ __    __ _ ");
		Console.display(" |  _  || '__|| || | | || '_ \\  / _` |");
		Console.display(" | | | || |   | || |_| || | | || (_| |");
		Console.display(" \\_| |_/|_|   | | \\__,_||_| |_| \\__,_|");
		Console.display("             _/ |                     ");
		Console.display("            |__/                      ");
		                              
		Console.marker(60);	
		Console.display("Copyright (c) 2015-17 AutoCognite Testing Research Pvt Ltd");
		Console.marker(60);
		Console.displayPaddedKeyValue("Product Name", this.name);
		Console.displayPaddedKeyValue("Version", this.version);
		Console.displayPaddedKeyValue("Website", "www.arjunapro.com");
		Console.displayPaddedKeyValue("Contact", "support@autocognite.com");
		Console.marker(60);	
	}

	public void setCliArgs(String[] args) {
		cliArgs = args;
	}
	
	public Reporter getReporter(){
		return this.reporter;
	}
	
	public void setReporter(Reporter reporter){
		this.reporter = reporter;
	}

	public void processNonTestClass(Class<?> klass) throws Exception {
		if (klass.isAnnotationPresent(DataMethodContainer.class)){
			dataMethodContainers.process(klass);
		} else if (klass.isAnnotationPresent(DataGenerator.class)){
			dataGenerators.process(klass);
		}

	}

	public Method getDataGeneratorMethod(String containerName, String dgName) throws Exception {
		return this.dataMethodContainers.getMethod(containerName, dgName);
	}

	public Method getDataGeneratorMethod(Class<?> containerClass, String dgName) throws Exception {
		return this.dataMethodContainers.getMethod(containerClass, dgName);
	}

	public DataSource getDataSourceFromDataGenName(String dataGenName) throws Exception {
		return this.dataGenerators.getDataSource(dataGenName);
	}

	public void setCentralExecState(CentralExecutionState execState) {
		this.execState = execState;
	}
	
	public CentralExecutionState getCentralExecState() {
		return this.execState;
	}

	public TestGroupsDB getTestGroupDB() {
		return this.groupsDB;
	}
	
	public CLIConfigurator getCliConfigurator(){
		return this.cliConfigurator;
	}

	public Session getSession() {
		return this.session;
	}

	public void setPickerOptions(Map<TestPickerProperty, String> options) {
		this.cliPickerOptions = options;
	}
	
}

class SessionCreator {
	private String sessionName = null;
	private Session session = null;

	public SessionCreator(ComponentIntegrator integrator, Map<TestPickerProperty, String> options, String sessionName) throws Exception{
		this.sessionName = sessionName;
		if (sessionName.trim().toUpperCase().equals("MSESSION")){
			ArjunaSingleton.INSTANCE.getTestGroupDB().createPickerConfigForCLIConfig(options);
			PickerTargetType pType = ArjunaSingleton.INSTANCE.getTestGroupDB().getTargetForMagicGroup();
			if (pType == null){
				session = new MSession();
			} else {
				session = new MSession(pType);
			}
		} else {
			String sessionsDir = integrator.value(ArjunaProperty.DIRECTORY_SESSIONS).asString();
			File sDir = new File(sessionsDir);
			boolean matchFound = false;
			String sFileName = null;
			if (!sDir.isDirectory()){
				Console.displayError("Sessions directory does not exist: " + sDir);
				Console.displayError("Exiting...");
				System.exit(1);
			}
			
			boolean fileExistsButConfExtUsedInSessionName = false;
			for (File f: sDir.listFiles()){
				if (f.isFile()){
					if (f.getName().toUpperCase().equals(sessionName.toUpperCase() + "." + "CONF")){
						matchFound = true;
						sFileName = f.getName();
						break;
					}
					
					if ((sessionName.toUpperCase().endsWith(".CONF")) && (f.getName().toUpperCase().equals(sessionName.toUpperCase()))){
						fileExistsButConfExtUsedInSessionName = true;
					}
				}
			}
			
			if (!matchFound){
				if (fileExistsButConfExtUsedInSessionName){
					Console.displayError("Provide session name without the conf extension.");
				} else {
					Console.displayError("No session template found for session name: "  + sessionName);
					Console.displayError(String.format("Ensure that >>%s.conf<< file is present in >>%s<< directory.", sessionName.toUpperCase().replace(".CONF", ""), sessionsDir));
					if (sessionName.toUpperCase().endsWith(".CONF")){
						Console.displayError("Also, provide session name without the conf extension.");
					}
				}
				Console.displayError("Exiting...");
				System.exit(1);
			}
			session = new UserDefinedSession(sessionName, sessionsDir + "/" + sFileName);
		}
		
	}
	
	public Session getSession() throws Exception {
		return this.session;
	}
}


