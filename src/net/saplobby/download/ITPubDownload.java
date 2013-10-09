package net.saplobby.download;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class ITPubDownload {
    public static final String FILESEPARATOR = System.getProperty("file.separator");
    public static final String OK = "OK";
    public static final String FROMDATE = "FROMDATE";
    public static final String USERNAME = "USERNAME";
    public static final String PASSWORD = "PASSWORD";
    public static final String LOGIN_URL = "http://www.itpub.net/member.php?mod=logging&action=login&loginsubmit=yes&infloat=yes";
    public static final String BASE_DIR = "d:/work/itpubdownload";
    static Properties configs = new Properties();

    static {
        try {
            loadConfigs(BASE_DIR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final void main(String[] args) {
        new ITPubDownload().download();
    }

    private static Properties loadConfigs(String basedir) throws IOException {
        configs.load(new FileInputStream(new File(basedir + FILESEPARATOR + "config" + FILESEPARATOR + "config.txt")));
        System.out.println(configs);
        return configs;
    }

    private void download() {
        try {
            DefaultHttpClient httpClient = getHttpClient();
            login(configs, httpClient);
            String destination = getDestination(BASE_DIR);
            createDownloadDirectory(destination);
            visitThreads(configs, httpClient, destination);
            System.out.println("\r\nall downloading are finished");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void visitThreads(Properties configs, DefaultHttpClient httpClient, String destination) throws Exception {
        List<String> threads = LinkerExtractor.extratThreads(new SimpleDateFormat("yyyy-MM-dd").parse(configs.getProperty(FROMDATE)));
        for (String thread : threads) {
            visitThread(httpClient, destination, thread);
        }
    }

    private void visitThread(DefaultHttpClient httpClient, String destination, String thread) {
        System.out.println("scanning thread: " + thread);
        try {
            downloadAttachments(httpClient, destination, thread);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void downloadAttachments(DefaultHttpClient httpClient, String destination, String thread) throws Exception {
        Set<ItpubAttachmentLink> links = LinkerExtractor.extracLinks(thread);
        if (links == null || links.isEmpty()) {
            System.out.println("no link found. make sure you set the suffix filter correctly.");
        } else {
            for (ItpubAttachmentLink link : links) {
                System.out.println(link);
                download(httpClient, link, destination);
            }
        }
    }

    private DefaultHttpClient getHttpClient() {
        return new DefaultHttpClient();
    }

    private String getDestination(String basedir) {
        return basedir + FILESEPARATOR + "downloaded";
    }

    private void login(Properties configs, DefaultHttpClient httpclient) throws IOException {
        String username = configs.getProperty(USERNAME);
        String pwd = configs.getProperty(PASSWORD);

        HttpPost httpost = new HttpPost(LOGIN_URL);
        List<BasicNameValuePair> nvps = new ArrayList<BasicNameValuePair>();
        nvps.add(new BasicNameValuePair("username", username));
        nvps.add(new BasicNameValuePair("password", pwd));
        httpost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

        HttpResponse response = httpclient.execute(httpost);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            EntityUtils.consume(entity);
        }
    }

    public String download(HttpClient httpclient, ItpubAttachmentLink link, String destination) {
        String linkStr = link.getLink();
        linkStr = linkStr.replace("attachment.php?", "forum.php?mod=attachment&").replace("&amp;", "&");
        System.out.println("final link " + linkStr);

        HttpGet finalAttachmentGet = null;
        InputStream attInput = null;
        FileOutputStream attOutput = null;
        try {
            finalAttachmentGet = new HttpGet(linkStr);
            HttpResponse finalResponse = httpclient.execute(finalAttachmentGet);
            File attFile = new File(destination + FILESEPARATOR + link.getName());
            HttpEntity finalEntity = finalResponse.getEntity();
            attInput = finalEntity.getContent();
            if (!needOverwrite(finalAttachmentGet, attFile, finalEntity)) {
                return OK;
            }

            System.out.println("downloading \"" + link.getName() + "\" is started.");
            attOutput = new FileOutputStream(attFile);
            writeFile(attInput, finalEntity.getContentLength(), attOutput);
            return OK;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(attInput);
            close(attOutput);
            finalAttachmentGet.releaseConnection();
        }

        return "ERROR";
    }

    private void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
        }
    }

    private boolean needOverwrite(HttpGet finalAttachmentGet, File attFile, HttpEntity finalEntity) {
        System.out.println("file size " + finalEntity.getContentLength());
        if (attFile.exists()) {
            System.out.println("existing file size is " + attFile.length());

            if (finalEntity.getContentLength() > attFile.length()) {
                System.out.println("size of new file is bigger than the existing one, which will be overwritten.");
            } else {
                System.out.println("file existed, no need to download");
                finalAttachmentGet.releaseConnection();
                return false;
            }

            System.out.println("downloading will continue");
        }
        return true;
    }

    private void writeFile(InputStream attInput, float fileLength, FileOutputStream attOutput) throws IOException {
        byte[] array = new byte[102400];
        int write = 0;
        int finished = 0;

        while ((write = attInput.read(array)) != -1) {
            finished += write;
            attOutput.write(array, 0, write);
            DecimalFormat df = new DecimalFormat("#");
            System.out.print("\r" + df.format(finished / fileLength * 100.0F) + "% downloaded");
        }
    }

    private void createDownloadDirectory(String destination) {
        File downloadDirectory = new File(destination);
        if (!downloadDirectory.exists()) {
            downloadDirectory.mkdirs();
        }
    }
}
