package com.cxiq.drools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.compiler.lang.DrlDumper;
import org.drools.compiler.lang.descr.ImportDescr;
import org.drools.compiler.lang.descr.PackageDescr;
import org.drools.compiler.lang.descr.RuleDescr;
import org.drools.definition.KnowledgePackage;
import org.drools.io.Resource;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CXIQDroolsService {

	@Qualifier("cxiq")
	@Autowired
	DataSource cxiqDatasource;

	static final Logger LOGGER = Logger.getLogger(CXIQDroolsService.class.getName());

	public String getGroup(String tenantId, String brand, int npsScore, String primary_emotion, String content,
			boolean emotion_happiness) {
		// load up the knowledge base
		KnowledgeBase kbase;
		String groupId = "";
		try {

			kbase = readKnowledgeBase(tenantId, brand);
			StatefulKnowledgeSession kSession = kbase.newStatefulKnowledgeSession();
			Group group = new Group();
			group.setNps_score(npsScore);
			// group.setPrimary_emotion(primary_emotion);
			group.setContent(content);
			// group.setEmotion_happiness(emotion_happiness);
			kSession.insert(group);
			kSession.fireAllRules();
			groupId = group.getGroupId();
			kSession.dispose();

		} catch (Exception e) {
			LOGGER.error("Error while executing rule: " + e.getMessage());
		}

		return groupId;

	}

	public String getGroup(String tenantId, String brand, Group group) {
		// load up the knowledge base
		KnowledgeBase kbase;
		String groupId = "";
		try {
			kbase = readKnowledgeBaseFromDB(tenantId, brand);
			StatefulKnowledgeSession kSession = kbase.newStatefulKnowledgeSession();
			// Group group = new Group(npsScore);
			// group.setNpsScore(npsScore);
			// group.setEmotion(emotion);
			kSession.insert(group);
			kSession.fireAllRules();
			groupId = group.getGroupId();
			kSession.dispose();

		} catch (Exception e) {
			LOGGER.error("Error while executing rule: " + e.getMessage());
		}

		return groupId;

	}

	private KnowledgeBase readKnowledgeBaseFromDB(String tenantId, String brand) {

		Connection connection = null;
		Statement stmt = null;
		String query = "";
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();

		try {
			connection = cxiqDatasource.getConnection();
			stmt = connection.createStatement();
			query = " select * from  " + tenantId + ".brand_groups_map where brand='" + brand + "' ";
			ResultSet rs = stmt.executeQuery(query);

			while (rs.next()) {

				String drl = rs.getString("drl");
				String groupId = rs.getString("group_id");
				Resource myResource = ResourceFactory.newReaderResource((Reader) new StringReader(drl));
				kbuilder.add(myResource, ResourceType.DRL);

				// Check the builder for errors
				if (kbuilder.hasErrors()) {
					LOGGER.error("Unable to compile drl for group -  " + groupId);
				}

				// get the compiled packages (which are serializable)
				Collection<KnowledgePackage> pkgs = kbuilder.getKnowledgePackages();

				// add the packages to a knowledgebase (deploy the knowledge packages).
				kbase.addKnowledgePackages(pkgs);

				if (LOGGER.isDebugEnabled())
					LOGGER.debug(" DRL content added to knowledgebase successfully in group - " + groupId);
			}

		} catch (Exception e) {
			LOGGER.error("In readKnowledgeBaseFromDB method catch block due to " + e);

		} finally {
			if (connection != null)
				try {
					connection.close();
				} catch (SQLException e) {
					LOGGER.error(e);
				}
		}

		return kbase;
	}

	private ArrayList<String> loadDRLFilesFromFileSystem(String drlFilesFolderPath) {
		ArrayList<String> filePaths = new ArrayList<String>();
		try {
			@SuppressWarnings("resource")
			Stream<Path> paths = Files.walk(Paths.get(drlFilesFolderPath));
			Iterator<Path> pathsIterator = paths.iterator();
			while (pathsIterator.hasNext()) {
				Path path = pathsIterator.next();
				filePaths.add(path.getFileName().toString());
			}

		} catch (IOException e) {
			LOGGER.error("Error while loading the DRL files from location:" + drlFilesFolderPath);
		}

		return filePaths;
	}

	private KnowledgeBase readKnowledgeBase(String tenantId, String brand) throws Exception {

		String drlFilesFolderPath = this.getClass().getClassLoader().getResource("").getPath() + "rules/" + tenantId
				+ "/" + brand;
		drlFilesFolderPath = drlFilesFolderPath.replaceAll("activiti-rest", "nps-rest");
		ArrayList<String> drlFilePaths = loadDRLFilesFromFileSystem(drlFilesFolderPath);
		KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		for (String drlFilePath : drlFilePaths) {
			if (drlFilePath != null && drlFilePath.contains(".drl")) {
				drlFilePath = drlFilesFolderPath + File.separator + drlFilePath;
				builder.add(ResourceFactory.newFileResource(drlFilePath), ResourceType.DRL);
				if (LOGGER.isDebugEnabled())
					LOGGER.debug(" DRL file added to knowledgebase successfully - " + drlFilePath);

			}
		}

		if (builder.hasErrors()) {
			LOGGER.error("Some DRL file contains invalid rules : " + builder.getErrors());
		}
		KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
		kbase.addKnowledgePackages(builder.getKnowledgePackages());
		return kbase;
	}

	public boolean createDRLFile(String tenantId, String brand, String rule, String groupId) {

		// --- Generate DRL file content
		PackageDescr pkg = new PackageDescr();
		pkg.setName("com.cxiq.drools");

		ImportDescr importEntry1 = new ImportDescr();
		importEntry1.setTarget("com.cxiq.drools.Group");
		pkg.addImport(importEntry1);

		RuleDescr ruleEntry = new RuleDescr();
		ruleEntry.setName(groupId);
		pkg.addRule(ruleEntry);
		String drl = new DrlDumper().dump(pkg);
		// condition = " when grp : Group( "+rule+" ) then grp.setGroup('"+groupId+"');
		// end ";
		JSONObject jsonObj = null;
		String condition = "";
		try {
			jsonObj = new JSONObject(rule);

			if (jsonObj != null) {
				condition = (String) jsonObj.get("rule");
			}

		} catch (JSONException e) {
			LOGGER.error("Error in parsing json " + e);
		}

		drl = drl.replaceAll("when", "when  group : Group( " + condition + " )");
		drl = drl.replaceAll("then", "then  group.setGroupId('" + groupId + "');");
		try {
			// create DRL file
			String drlFilesFolderPath = this.getClass().getClassLoader().getResource("").getPath() + "rules/" + tenantId
					+ "/" + brand + "/";
			String filePath = drlFilesFolderPath + groupId + ".drl";
			File file = new File(filePath);
			file.createNewFile();
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(drl);
			// close connection
			bw.close();
			if (LOGGER.isDebugEnabled())
				LOGGER.debug(" DRL file generated successfully - " + filePath);

		} catch (Exception e) {
			LOGGER.error("DRL file generation failed  :" + e.getMessage());
			return false;
		}

		return true;
	}

	public String createDRL(String tenantId, String brand, String rule, String groupId) {

		// --- Generate DRL file content
		PackageDescr pkg = new PackageDescr();
		pkg.setName("com.cxiq.drools");

		ImportDescr importEntry1 = new ImportDescr();
		importEntry1.setTarget("com.cxiq.drools.Group");
		pkg.addImport(importEntry1);

		RuleDescr ruleEntry = new RuleDescr();
		ruleEntry.setName(groupId);
		pkg.addRule(ruleEntry);
		String drl = new DrlDumper().dump(pkg);
		// condition = " when grp : Group( "+rule+" ) then grp.setGroup('"+groupId+"');
		// end ";
		JSONObject jsonObj = null;
		String condition = "";
		try {
			jsonObj = new JSONObject(rule);

			if (jsonObj != null) {
				condition = (String) jsonObj.get("rule");
			}

		} catch (JSONException e) {
			LOGGER.error("Error in parsing json " + e);
		}

		drl = drl.replaceAll("when", "when  group : Group( " + condition + " )");
		drl = drl.replaceAll("then", "then  group.setGroupId(\"" + groupId + "\");");
		/*
		 * try { // create DRL file String
		 * drlFilesFolderPath=this.getClass().getClassLoader().getResource("").getPath()
		 * +"rules/"+tenantId+"/"+brand+"/"; String
		 * filePath=drlFilesFolderPath+groupId+".drl"; File file = new File(filePath);
		 * file.createNewFile(); FileWriter fw = new FileWriter(file.getAbsoluteFile());
		 * BufferedWriter bw = new BufferedWriter(fw); bw.write(drl); // close
		 * connection bw.close(); if (LOGGER.isDebugEnabled())
		 * LOGGER.debug(" DRL file generated successfully - "+filePath);
		 * 
		 * } catch(Exception e) {
		 * LOGGER.error("DRL file generation failed  :"+e.getMessage()); return false; }
		 */

		return drl;
	}

	public boolean deleteDRLFile(String tenantId, String brand, String groupId) {
		boolean result = false;
		String drlFilesFolderPath = this.getClass().getClassLoader().getResource("").getPath() + "rules/" + tenantId
				+ "/" + brand + "/";
		String filePath = drlFilesFolderPath + groupId + ".drl";
		try {
			if (Files.deleteIfExists(Paths.get(filePath)))
				result = true;
		} catch (IOException e) {
			LOGGER.error("Unable to delete file - " + filePath + ":" + e.getMessage());
			return false;
		}
		return result;
	}
}
