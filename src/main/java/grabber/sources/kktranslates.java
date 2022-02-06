package grabber.sources;

import grabber.Chapter;
import grabber.GrabberUtils;
import grabber.Novel;
import grabber.NovelMetadata;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class kktranslates implements Source {
    private final String name = "KK Translates";
    private final String url = "https://kktranslates.home.blog/";
    private final boolean canHeadless = false;
    private Novel novel;
    private Document toc;

    public kktranslates(Novel novel) {
            this.novel = novel;
        }

    public kktranslates() {
        }

    public String getName() {
        return name;
    }

    public boolean canHeadless() {
        return canHeadless;
    }

    public String toString() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public List<Chapter> getChapterList() {
        List<Chapter> chapterList = new ArrayList();
        try {
            toc = Jsoup.connect(novel.novelLink).cookies(novel.cookies).get();
            Elements chapterLinks = toc.select(".wp-block-table > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(2) > td:nth-child(1) a");
            for (Element chapterLink : chapterLinks) {
                chapterList.add(new Chapter(chapterLink.text(), chapterLink.attr("abs:href")));
            }
        } catch (HttpStatusException httpEr) {
            GrabberUtils.err(novel.window, GrabberUtils.getHTMLErrMsg(httpEr));
        } catch (IOException e) {
            GrabberUtils.err(novel.window, "Could not connect to webpage!", e);
        } catch (NullPointerException e) {
            GrabberUtils.err(novel.window, "Could not find expected selectors. Correct novel link?", e);
        }
        return chapterList;
    }

    public Element getChapterContent(Chapter chapter) {
            Element chapterBody = null;
            try {
                Document doc = Jsoup.connect(chapter.chapterURL).cookies(novel.cookies).get();
                chapterBody = doc.select(".entry-content").first();
            } catch (HttpStatusException httpEr) {
                GrabberUtils.err(novel.window, GrabberUtils.getHTMLErrMsg(httpEr));
            } catch (IOException e) {
                GrabberUtils.err(novel.window, "Could not connect to webpage!", e);
            }
            return chapterBody;
    }

    public NovelMetadata getMetadata() {
        NovelMetadata metadata = new NovelMetadata();

        if (toc != null) {
            metadata.setTitle(toc.select("h2").first().text());
            metadata.setBufferedCover(toc.selectFirst("meta[property=og:image]").attr("content"));

        }

        return metadata;
    }

    public List<String> getBlacklistedTags() {
        List blacklistedTags = new ArrayList();
        blacklistedTags.add(".has-white-color");
        blacklistedTags.add("p[style=font-size:12px;text-align:center]");
        blacklistedTags.add(".has-text-align-center");
        blacklistedTags.add(".sharedaddy");
        return blacklistedTags;
    }
}