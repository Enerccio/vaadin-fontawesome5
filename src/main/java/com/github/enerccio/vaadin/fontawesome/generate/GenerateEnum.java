package com.github.enerccio.vaadin.fontawesome.generate;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class GenerateEnum {

	private static class SourceMap {
		private String srcUrl;
		private String srcFile;
		private String element;
	}

	private static List<SourceMap> sources = new ArrayList<>();
	static {
		SourceMap fa = new SourceMap();
		fa.srcUrl = "https://fontawesome.com/cheatsheet/free/solid";
		fa.srcFile = "fontawesome-addon/src/main/java/com/github/enerccio/vaadin/fontawesome/FontAwesome.java";
		fa.element = "solid";
		sources.add(fa);

		fa = new SourceMap();
		fa.srcUrl = "https://fontawesome.com/cheatsheet/free/regular";
		fa.srcFile = "fontawesome-addon/src/main/java/com/github/enerccio/vaadin/fontawesome/FontAwesomeRegular.java";
		fa.element = "regular";
		sources.add(fa);

		fa = new SourceMap();
		fa.srcUrl = "https://fontawesome.com/cheatsheet/free/brands";
		fa.srcFile = "fontawesome-addon/src/main/java/com/github/enerccio/vaadin/fontawesome/FontAwesomeBrands.java";
		fa.element = "brands";
		sources.add(fa);
	}

	private static final String ENUM_ENTRY_REGEX = "\\t[A-Z0-9_]+\\(\\\"fa-.+\\),";
	
	public static void main(String[] args) {
		for (SourceMap sa : sources) {
			writeFile(getIcons(sa.srcUrl, sa.element), sa.srcFile);
		}
		System.out.println("Done.");
	}
	
	private static Map<String, String> getIcons(String sourceUrl, String element) {
		Map<String, String> icons = new LinkedHashMap<String, String>();
		try {
			WebClient webClient = new WebClient(BrowserVersion.FIREFOX);
			webClient.setAjaxController(new NicelyResynchronizingAjaxController());
			webClient.getOptions().setThrowExceptionOnScriptError(false);
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
			HtmlPage page = webClient.getPage(sourceUrl);
			JavaScriptJobManager manager = page.getEnclosingWindow().getJobManager();
			while (manager.getJobCount() > 1) {
				Thread.sleep(1000);
			}

			Document html = Jsoup.parse(page.asXml());

			Element cheatSheet = html.getElementById(element);

			// Loop through icons, get class and hex, add to map
			String cssClass, hex;
			for (Element e : cheatSheet.getElementsByClass("icon")) { // Cheatsheet has only 1 .row element
				Element inner = e.getAllElements().get(1);

				Element nameSpan = inner.getAllElements().get(3);
				String name = nameSpan.ownText();

				Element codeSpan = inner.getAllElements().get(4);
				String type = codeSpan.ownText();

				cssClass = name;
				hex = type.trim();
				icons.put(cssClass, hex);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("\tRead " + icons.size() + " icons.");
		return icons;
	}
	
	private static void writeFile(Map<String, String> icons, String enumFile) {
		System.out.println("Updating " + enumFile + "....");
		File infile = new File(enumFile);
		File outfile = new File(enumFile + ".tmp");
		BufferedReader reader = null;
		BufferedWriter writer = null;
		try {
			reader = new BufferedReader(new FileReader(infile));
			writer = new BufferedWriter(new FileWriter(outfile));
			String inline = null;
			int current = 0;
			int enumstart = 0;
			while ((inline = reader.readLine()) != null) {
				current++;
				if (enumstart > 0) {
					// Check if we're still in the enum
					if (!inline.matches(ENUM_ENTRY_REGEX)) {
						System.out.println("\tIcons end on line " + current + ". "
								+ "Replaced " + (current - enumstart + 1) + " icons.");
						enumstart = 0;
					}
				} else if (inline.contains("public enum FontAwesome")) {
					// Enum entries start on next line
					enumstart = current + 1;
					System.out.println("\tIcons start on line " + enumstart + ".");
					writer.write(inline + "\n");
					
					// Write the new enum entries
					writeEnumLines(writer, icons);
				} else {
					// Copy the rest of the file
					writer.write(inline + "\n");
				}
			}
			
			writer.close();
			reader.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void writeEnumLines(BufferedWriter writer, Map<String, String> icons) throws IOException {
		String outline = null;
		int outlines = 0;
		int count = icons.size();
		for (Entry<String, String> icon : icons.entrySet()) {
			outline = "\t" + icon.getKey().toUpperCase().replace("FA-",  "").replace("-", "_").replace("500PX", "_500PX");
			outline += "(\"" + icon.getKey() + "\", ";
			outline += "0x" + icon.getValue();
			outline += ")" + (++outlines == count ? ";" : ",") + "\n";
			
			writer.write(outline);
		}
	}
}
