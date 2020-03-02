package com.alation.api.utils.article;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.alation.api.models.AlationArticle;
import com.alation.api.utils.ApiUtils;
import com.alation.api.utils.Constants;
import com.alation.api.utils.PropHelper;
import com.alation.api.utils.files.CSVStoreManager;

public class ArticleExtractor {
	private CSVFormat csvFileFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader();

	private static Logger logger = Logger.getLogger(ArticleExtractor.class);
	private List<String> columnNames = new ArrayList<>();

	private Map<String, List<String>> templates = new HashMap<>();
	private String[] columnHeaders;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ArticleExtractor articleExtract = new ArticleExtractor();

		// articleExtract.fetchCustomTemplates();
		Map<String, String> customTemplatesMap = new HashMap<>();
		customTemplatesMap.put("32", "BGP");
		try {
			articleExtract.getArticlesForCustomTemplate(customTemplatesMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void fetchCustomTemplates() {

		String templateName = "";
		CSVStoreManager csvStoreManager = null;
		CSVParser csvFileParser = null;
		Path newFilePath = null;
		JSONArray customTemplates = getCustomTemplates(templateName);
		System.out.println(customTemplates.size());
		for (int j = 0; j < customTemplates.size(); j++) {
			
			JSONObject customtemplate = (JSONObject) customTemplates.get(j);
			String o = customTemplates.get(j).toString();
			String templateId = customtemplate.get(Constants.ID).toString();
			String title = customtemplate.get(Constants.TITLE).toString();
			System.out.println("templateId" + templateId);
			try {

				String exportFilesLocation = createExportFilesFolder();
				newFilePath = ArticleUtils.tocreateExportFile(exportFilesLocation, title);
				String csvLocation = PropHelper.getHelper().getCsvLocation();
				Path filePath = Paths.get(csvLocation);

				Reader reader = new InputStreamReader(new FileInputStream(filePath.toString()), "UTF-8");
				csvFileParser = new CSVParser(reader, csvFileFormat);

				// Method to add the Response Header into Response file
				columnNames.add("ArticleID");
				columnNames.add("Title");
				columnHeaders = ArticleUtils.toAddResponseHeader(columnNames);

				csvStoreManager = new CSVStoreManager(newFilePath.toUri(), columnHeaders);

				System.out.println("newFilePath for export" + newFilePath);

			} catch (Exception e) {
				logger.info(e.getMessage());
			}

		}
		System.out.println("Run successfully completed");
	}

	public static JSONArray getCustomTemplates(String templateName) {
		JSONArray customTemplates = new JSONArray();
		try {
			HttpResponse response = CustomUtils.CustomTemplatesAPI(templateName);
			if (response.getStatusLine().getStatusCode() == 200) {
				logger.info("Fetching completed.");
				customTemplates = (JSONArray) new JSONParser()
						.parse(ApiUtils.convert(response.getEntity().getContent()));
				if (customTemplates.size() == 0) {
					logger.warn("There are no templates in Alation with name: " + templateName);
				}
			} else {
				logger.error("Error fetching the template(s)!!");
				logger.error(response.toString());
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return customTemplates;

	}

	protected static String createExportFilesFolder() throws Exception {
		String exportFilesLocation = "";
		try {
			String basePath = PropHelper.getHelper().getCsvLocation();

			File baseFile = new File(basePath);
			if (!baseFile.isDirectory()) {
				basePath = baseFile.getParent();
			}

			/*
			 * String userHome = "user.home"; String path =
			 * System.getProperty(userHome);
			 */
			exportFilesLocation = basePath + File.separator + Constants.Export;
			Path filePath = Paths.get(exportFilesLocation);
			// Check filepath is exist or not
			if (!Files.exists(filePath)) {
				Files.createDirectory(filePath);
			}
			return exportFilesLocation;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new Exception(
					"Could not create a directory with this name: " + exportFilesLocation + " in root folder", e);
		}
	}

	protected String getArticlesForCustomTemplate(Map<String, String> customTemplatesMap) throws Exception {
		Map<String, String> map = new HashMap<>(customTemplatesMap);
		List<String> customTemplatesId = new ArrayList<>();
		customTemplatesId.addAll(map.keySet());
		logger.info("customTemplatesId:: " + customTemplatesId);
		for (String templateId : customTemplatesId) {
			JSONArray articlesList = ArticleUtils.getArticlesListByCustomTemplateId(new ArrayList<>(customTemplatesId));
			JSONArray filteredArticles = filterArticles(articlesList);

			logger.info(filteredArticles);

		}
		return null;
	}

	@SuppressWarnings("unchecked")
	protected JSONArray filterArticles(JSONArray articlesList) {

		JSONArray filterArticles = new JSONArray();

		for (int i = 0; i < articlesList.size(); i++) {

			JSONObject article = (JSONObject) articlesList.get(i);

			JSONArray custom_fields = (JSONArray) article.get(Constants.CUST_FIELDS);

			if (custom_fields.size() == 0) {
				continue;
			} else {
				filterArticles.add(article);
			}

		}
		return filterArticles;
	}
}
