import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

/*
 * Chapter download handling
 */
public class fetchChapters {
	public static boolean error = false;
	private static final String NL = System.getProperty("line.separator");
	private static final String textEncoding = "UTF-8";
	private static String tocFileName = "Table Of Contents";
	public static List<String> chapterFileNames = new ArrayList<String>();
	public static List<String> chapterUrl = new ArrayList<String>();
	public static List<Integer> failedChapters = new ArrayList<Integer>();
	public static long startTime;
	public static long endTime;

	/**
	 * Opens novel's table of contents page, retrieves chapter all links and
	 * processes them with saveChapters().
	 */
	public static void getAllChapterLinks(String url, String saveLocation, String host, String fileType,
			boolean chapterNumeration, boolean invertOrder)
			throws IllegalArgumentException, FileNotFoundException, IOException {
		startTime = System.nanoTime();
		Novel currentNovel = new Novel(host, url);
		String titleReplacement = "";
		ArrayList<String> chaptersNames = new ArrayList<String>();
		int chapterNumber = 0;
		int chapterAmount = 0;
		NovelGrabberGUI.autoAppendText("Connecting...");
		Document doc = Jsoup.connect(currentNovel.getUrl()).get();
		tocFileName = (doc.title().replaceAll("[^\\w]+", "-").replace(currentNovel.getTitleHostName(),
				titleReplacement)) + " Table of Contents";
		Element content = doc.select(currentNovel.getChapterLinkContainer()).first();
		Elements chapterItem = content.select(currentNovel.getChapterLinkSelecter());
		Elements links = chapterItem.select("a[href]");
		for (Element chapterLink : links) {
			chapterAmount++;
			chaptersNames.add(chapterLink.text());
		}
		if (invertOrder == true) {
			Collections.reverse(chaptersNames);
		}
		NovelGrabberGUI.setMaxProgress("auto", chapterAmount);
		for (Element chapterLink : links) {
			chapterNumber++;
			saveChapters(chapterLink.attr("abs:href"), saveLocation, host, chapterNumber, fileType, chapterNumeration,
					chaptersNames.get(chapterNumber - 1));
		}
		NovelGrabberGUI.autoAppendText("Finished! " + (chapterNumber - failedChapters.size()) + " of " + chapterNumber
				+ " chapters successfully grabbed.");
		if (!failedChapters.isEmpty()) {
			NovelGrabberGUI.autoAppendText("Failed to grab the following chapters:");
			for (Integer num : failedChapters) {
				NovelGrabberGUI.autoAppendText("Chapter " + num);
			}
		}
	}

	/**
	 * Opens novel's table of contents page, retrieves chapter links from selected
	 * chapter range and processes them with saveChapters()
	 */
	public static void getChapterRangeLinks(String url, String saveLocation, String host, int firstChapter,
			int lastChapter, String fileType, boolean chapterNumeration, boolean invertOrder)
			throws IllegalArgumentException, FileNotFoundException, IOException {
		startTime = System.nanoTime();
		error = false;
		Novel currentNovel = new Novel(host, url);
		ArrayList<String> chapters = new ArrayList<String>();
		ArrayList<String> chaptersNames = new ArrayList<String>();
		String titleReplacement = "";
		int chapterNumber = 0;
		NovelGrabberGUI.autoAppendText("Connecting...");
		Document doc = Jsoup.connect(currentNovel.getUrl()).get();
		tocFileName = "Table-of-Contents-"
				+ (doc.title().replaceAll("[^\\w]+", "-").replace(currentNovel.getTitleHostName(), titleReplacement)
						+ "-Chapter-" + firstChapter + "-" + lastChapter);
		Elements content = doc.select(currentNovel.getChapterLinkContainer());
		Elements chapterItem = content.select(currentNovel.getChapterLinkSelecter());
		Elements links = chapterItem.select("a[href]");
		for (Element chapterLink : links) {
			chapters.add(chapterLink.attr("abs:href"));
			chaptersNames.add(chapterLink.text());

		}
		if (lastChapter > chapters.size()) {
			NovelGrabberGUI.manAppendText("Novel does not have that many chapters.");
			error = true;
			return;
		} else {
			if (invertOrder == true) {
				Collections.reverse(chapters);
				Collections.reverse(chaptersNames);
			}
			NovelGrabberGUI.setMaxProgress("auto", (lastChapter - firstChapter) + 1);
			for (int i = firstChapter - 1; i <= lastChapter - 1; i++) {
				chapterNumber++;
				saveChapters(chapters.get(i), saveLocation, host, chapterNumber, fileType, chapterNumeration,
						chaptersNames.get(i));
			}
			endTime = System.nanoTime();
			long elapsedTime = TimeUnit.SECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
			NovelGrabberGUI.autoAppendText("Finished! " + (chapterNumber - failedChapters.size()) + " of "
					+ chapterNumber + " chapters successfully grabbed in " + elapsedTime + " seconds.");
			if (!failedChapters.isEmpty()) {
				NovelGrabberGUI.autoAppendText("Failed to grab the following chapters:");
				for (Integer num : failedChapters) {
					NovelGrabberGUI.autoAppendText("Chapter " + num);
				}
			}
		}
	}

	/**
	 * Opens chapter link and tries to save it's content at provided destination
	 * directory
	 */
	public static void saveChapters(String url, String saveLocation, String host, int chapterNumber, String fileType,
			boolean chapterNumeration, String fileName)
			throws IllegalArgumentException, FileNotFoundException, IOException {
		Novel currentNovel = new Novel(host, url);
		Document doc = Jsoup.connect(url).get();
		// Chapter numeration in filename
		if (chapterNumeration == false) {
			fileName = fileName.replaceAll("[^\\w]+", "-") + fileType;
		} else {
			fileName = "Ch-" + chapterNumber + "-" + fileName.replaceAll("[^\\w]+", "-") + fileType;
		}
		try {
			Element content = doc.select(currentNovel.getChapterContainer()).first();
			Elements p = content.select(currentNovel.getSentenceSelecter());
			// Check if sentence selector is empty and skip this chapter if so
			if (p.isEmpty()) {
				NovelGrabberGUI.autoAppendText(
						"[ERROR] Could not detect sentence wrapper for chapter " + chapterNumber + "(" + url + ")");
				failedChapters.add(chapterNumber);
				return;
			}
			// Create and save contents of chapter in file
			File dir = new File(saveLocation);
			if (!dir.exists())
				dir.mkdirs();
			if (fileType == ".txt") {
				try (PrintStream out = new PrintStream(saveLocation + File.separator + fileName, textEncoding)) {
					for (Element x : p) {
						out.println(x.text() + NL);
					}
				}
			} else {
				try (PrintStream out = new PrintStream(saveLocation + File.separator + fileName, textEncoding)) {
					out.print("<!DOCTYPE html>" + NL + "<html lang=\"en\">" + NL + "<head>" + NL
							+ "<meta charset=\"utf-8\" />" + NL + "</head>" + NL + "<body>" + NL);
					for (Element x : p) {
						out.print("<p>" + x.text() + "</p>" + NL);
					}
					out.print("</body>" + NL + "</html>");
				}
			}
			chapterFileNames.add(fileName);
			NovelGrabberGUI.autoAppendText(fileName + " saved.");
			NovelGrabberGUI.updateProgress("auto", 1);
		} catch (Exception noSelectors) {
			NovelGrabberGUI.autoAppendText("Could not detect selectors on: " + url);
		}

	}

	/**
	 * Opens chapter link and tries to save it's content in current directory
	 */
	public static void saveChapter(String url, String host)
			throws IllegalArgumentException, FileNotFoundException, IOException {
		Novel currentNovel = new Novel(host, url);
		NovelGrabberGUI.autoAppendText("Connecting...");
		Document doc = Jsoup.connect(url).get();
		String fileName = doc.title().replaceAll("[^\\w]+", "-") + ".html";
		try {
			Element content = doc.select(currentNovel.getChapterContainer()).first();
			Elements p = content.select(currentNovel.getSentenceSelecter());
			try (PrintStream out = new PrintStream(fileName, textEncoding)) {
				out.print("<!DOCTYPE html>" + NL + "<html lang=\"en\">" + NL + "<head>" + NL
						+ "<meta charset=\"utf-8\" />" + NL + "</head>" + NL + "<body>" + NL);
				for (Element x : p) {
					out.print("<p>" + x.text() + "</p>" + NL);
				}
				out.print("</body>" + NL + "</html>");
			}
			NovelGrabberGUI.autoAppendText(fileName + " saved.");
		} catch (Exception noSelectors) {
			NovelGrabberGUI.autoAppendText("Could not detect selectors on: " + url);
		}
	}

	public static void retrieveChapterLinks(String url)
			throws IllegalArgumentException, FileNotFoundException, IOException {
		NovelGrabberGUI.manAppendText("Retrieving links from: " + url);
		Document doc = Jsoup.connect(url).get();
		Elements links = doc.select("a[href]");
		String currChapterLink = null;
		int numberOfLinks = 0;
		for (Element chapterLink : links) {
			currChapterLink = chapterLink.attr("abs:href");
			if (currChapterLink.startsWith("http") && !chapterLink.text().isEmpty()) {
				chapterUrl.add(currChapterLink);
				NovelGrabberGUI.listModelChapterLinks.addElement(chapterLink.text());
				numberOfLinks++;
			}
		}
		if (!chapterUrl.isEmpty()) {
			NovelGrabberGUI.manAppendText(numberOfLinks + " links retrieved.");
		}

	}

	public static void manSaveChapters(String saveLocation, String fileType, boolean chapterNumeration,
			String chapterContainer, String sentenceSelecter, boolean invertOrder)
			throws IllegalArgumentException, FileNotFoundException, IOException {
		startTime = System.nanoTime();
		String fileName = null;
		int chapterNumber = 0;
		NovelGrabberGUI.setMaxProgress("manual", chapterUrl.size());
		if (invertOrder == true) {
			Collections.reverse(chapterUrl);
		}
		for (String chapter : chapterUrl) {
			chapterNumber++;
			Document doc = Jsoup.connect(chapter).get();
			if (chapterNumeration == false) {
				if (invertOrder == true) {
					fileName = NovelGrabberGUI.listModelChapterLinks
							.get(NovelGrabberGUI.listModelChapterLinks.getSize() - chapterNumber).toString()
							.replaceAll("[^\\w]+", "-") + fileType;
				} else {
					fileName = NovelGrabberGUI.listModelChapterLinks.get(chapterNumber - 1).toString()
							.replaceAll("[^\\w]+", "-") + fileType;
				}

			} else {
				if (invertOrder == true) {
					fileName = "Ch-" + chapterNumber + "-"
							+ NovelGrabberGUI.listModelChapterLinks
									.get(NovelGrabberGUI.listModelChapterLinks.getSize() - chapterNumber).toString()
									.replaceAll("[^\\w]+", "-")
							+ fileType;
				} else {
					fileName = "Ch-" + chapterNumber + "-" + NovelGrabberGUI.listModelChapterLinks
							.get(chapterNumber - 1).toString().replaceAll("[^\\w]+", "-") + fileType;
				}

			}
			try {
				Element content = doc.select(chapterContainer).first();
				Elements p = content.select(sentenceSelecter);
				if (p.isEmpty()) {
					NovelGrabberGUI.manAppendText("[ERROR] Could not detect sentence wrapper for chapter "
							+ chapterNumber + "(" + chapter + ")");
					failedChapters.add(chapterNumber);
				} else {
					File dir = new File(saveLocation);
					if (!dir.exists())
						dir.mkdirs();
					if (fileType == ".txt") {
						try (PrintStream out = new PrintStream(saveLocation + File.separator + fileName,
								textEncoding)) {
							for (Element x : p) {
								out.println(x.text() + NL);
							}
						}
					} else {
						try (PrintStream out = new PrintStream(saveLocation + File.separator + fileName,
								textEncoding)) {
							out.print("<!DOCTYPE html>" + NL + "<html lang=\"en\">" + NL + "<head>" + NL
									+ "<meta charset=\"UTF-8\" />" + NL + "</head>" + NL + "<body>" + NL);
							for (Element x : p) {
								out.print("<p>" + x.text() + "</p>" + NL);
							}
							out.print("</body>" + NL + "</html>");
						}
					}
					chapterFileNames.add(fileName);
					NovelGrabberGUI.manAppendText(fileName + " saved.");
					NovelGrabberGUI.updateProgress("manual", 1);
				}

			} catch (Exception noSelectors) {
				NovelGrabberGUI.manAppendText(
						"[ERROR] Could not detect sentence wrapper for chapter " + chapterNumber + "(" + chapter + ")");
				failedChapters.add(chapterNumber);
			}
		}
		endTime = System.nanoTime();
		long elapsedTime = TimeUnit.SECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
		NovelGrabberGUI.manAppendText("Finished! " + (chapterNumber - failedChapters.size()) + " of " + chapterNumber
				+ " chapters successfully grabbed in " + elapsedTime + " seconds.");
		if (!failedChapters.isEmpty()) {
			NovelGrabberGUI.manAppendText("Failed to grab the following chapters:");
			for (Integer num : failedChapters) {
				NovelGrabberGUI.manAppendText("Chapter " + num);
			}
		}
		if (invertOrder == true) {
			Collections.reverse(chapterUrl);
		}
	}

	public static void createToc(String saveLocation) throws FileNotFoundException, UnsupportedEncodingException {
		if (!chapterFileNames.isEmpty()) {
			String fileName = tocFileName + ".html";
			try (PrintStream out = new PrintStream(saveLocation + File.separator + fileName, textEncoding)) {
				out.print("<!DOCTYPE html>" + NL + "<html lang=\"en\">" + NL + "<head>" + NL
						+ "<meta charset=\"UTF-8\" />" + NL + "</head>" + NL + "<body>" + NL
						+ "<h1>Table of Contents</h1>" + NL + "<p style=\"text-indent:0pt\">" + NL);
				for (String chapterFileName : chapterFileNames) {
					out.print("<a href=\"" + chapterFileName + "\">" + chapterFileName.replace(".html", "")
							+ "</a><br/>" + NL);
				}
				out.print("</p>" + NL + "</body>" + NL + "</html>" + NL);
			}
			NovelGrabberGUI.manAppendText(fileName + " created.");
		}

	}
}
