package net.saplobby.download;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import static net.saplobby.download.ITPubDownload.*;
import static org.jsoup.Connection.Response;

public class LinkerExtractor {

    public static final int TIMEOUT_MILLIS = 1000000000;
    public static final String HTTP_WWW_ITPUB_NET = "http://www.itpub.net/";

    public static List<String> extratThreads(Date start) throws Exception {
        List<String> threads = new ArrayList<String>();

        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            if (!extractPage(threads, i, start)) {
                break;
            }
        }

        return threads;
    }

    private static boolean extractPage(List<String> threads, int page, Date start) throws Exception {
        try {
            Elements linkElements = getDocument("http://www.itpub.net/forum.php?mod=forumdisplay&fid=61&filter=author&orderby=dateline&page=" + page).select("a[href^=forum.php?mod=viewthread]");
            for (Element element : linkElements) {
                String publishDateString = element.nextSibling().nextSibling().nextSibling().nextSibling().childNode(2).toString().trim().split(" ")[0];
                Date publishDate = new SimpleDateFormat("yyyy-MM-dd").parse(publishDateString);
                if (publishDate.before(start)) {
                    if (page != 1) {
                        return false;
                    } else {
                        continue;
                    }
                }
                threads.add(getHref(element));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private static Document getDocument(String url) throws IOException {
        Response res = Jsoup.connect(LOGIN_URL).data("username", configs.getProperty(USERNAME), "password", configs.getProperty(PASSWORD)).method(Connection.Method.POST).execute();
        return Jsoup.connect(url).timeout(TIMEOUT_MILLIS).cookies(res.cookies()).get();
    }

    public static Set<ItpubAttachmentLink> extracLinks(String url) throws Exception {
        Set<ItpubAttachmentLink> links = new TreeSet<ItpubAttachmentLink>();
        try {
            Elements linkElements = getDocument(url).select("a[href^=forum.php?mod=attachment]");
            if ((linkElements != null) && (linkElements.size() > 0)) {
                for (Element element : linkElements) {
                    String linkHref = getHref(element);
                    links.add(new ItpubAttachmentLink(linkHref, element.text()));
                }
            } else {
                System.out.println("no link found");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return links;
    }

    private static String getHref(Element element) {
        return HTTP_WWW_ITPUB_NET + element.attr("href");
    }

}
