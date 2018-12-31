package euphoria.psycho.library;

import android.text.TextUtils;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class EbookUtils {
    private static final String TAG = "EbookUtils";

    public static String changeExtension(String fileName, String extension) {
        if (TextUtils.isEmpty(fileName) || TextUtils.isEmpty(extension)) return fileName;
        String s = fileName;
        for (int i = fileName.length(); --i >= 0; ) {
            char ch = fileName.charAt(i);
            if (ch == '.') {
                s = s.substring(0, i);
                break;
            }
        }
        if (extension.charAt(0) != '.') {
            s += ".";
        }
        return s + extension;
    }

    public static void epub2txt(String fileName) {
        try {
            FileOutputStream outputStream = new FileOutputStream(changeExtension(fileName, ".txt"));
            ZipFile zipFile = new ZipFile(fileName);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<ZipEntry> zipEntries = new ArrayList<>();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                zipEntries.add(zipEntry);
                if (zipEntry.getName().endsWith(".opf")) {
                    Document document = Jsoup.parse(streamToString(zipFile.getInputStream(zipEntry)));
                    Elements elements = document.select("item");
                    Elements refElements = document.select("spine itemref");
                    for (Element refel : refElements) {
                        String id = refel.attr("idref");
                        for (Element e : elements) {
                            if (id.equals(e.attr("id"))) {
                                arrayList.add(e.attr("href"));
                                break;
                            }
                        }
                    }
//                    for (Element e : elements) {
//                        String atr = e.attr("href");
//                        if (e.attr("media-type").equals("application/xhtml+xml")&&!arrayList.contains(atr))
//                            arrayList.add(atr);
//                    }
                }
            }
            for (String l : arrayList) {
                Log.e(TAG, "Currently " + l);
                for (ZipEntry z : zipEntries) {
                    if (z.getName().endsWith(l)) {
                        Log.e(TAG, "Processing " + z.getName());
                        String content = streamToString(zipFile.getInputStream(z));
                        Document document = Jsoup.parse(content);
                        content = getPlainText(document);
                        outputStream.write(content.getBytes(Charset.forName("utf-8")));
                        outputStream.write("\n".getBytes(Charset.forName("utf-8")));
                        break;
                    }
                }
            }
            zipFile.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public static void generateHTML(String dir) {
        File directory = new File(dir);
        if (!directory.isDirectory()) return;
        File[] tocFiles = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isFile() && pathname.getName().endsWith(".ncx")) return true;
                return false;
            }
        });
        if (tocFiles == null || tocFiles.length == 0) return;
        Document document = null;
        try {
            document = Jsoup.parse(tocFiles[0], "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (document == null) return;
        Elements elements = document.select("navpoint");
        if (elements.size() == 0) return;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<ol>");
        for (Element element : elements) {

            stringBuilder.append(String.format("<li><a href=\"%s\">%s</a></li>",
                    element.select("content").get(0).attr("src"),
                    element.select("text").get(0).text()));
        }
        stringBuilder.append("</ol>");

        File targetTocFile = new File(dir, "目录.html");
        try {
            FileOutputStream outputStream = new FileOutputStream(targetTocFile);
            byte[] buffer = stringBuilder.toString().getBytes(Charset.forName("utf-8"));
            outputStream.write(buffer, 0, buffer.length);
            outputStream.close();
        } catch (Exception e) {

        }

    }

    private static String getPlainText(Element element) {
        FormattingVisitor formatter = new FormattingVisitor();
        new NodeTraversor(formatter).traverse(element); // walk the DOM, and call .head() and .tail() for each node
        return formatter.toString();
    }

    //    public static void renamPDFInDirectory(String fileName) {
//        File src = new File(fileName);
//
//        File[] files = src.listFiles(new FileFilter() {
//            @Override
//            public boolean accept(File file) {
//                if (file.isFile() && file.getName().endsWith(".pdf")) return true;
//                return false;
//            }
//        });
//
//        for (File f : files) {
//            renamePDF(f);
//        }
//    }
//    public static void renamePDF(File src) {
//        try {
//            PdfReader pdfReader = new PdfReader(src.getAbsolutePath());
//            HashMap<String, String> hashMap = pdfReader.getInfo();
//
//            if (hashMap.containsKey("Title")) {
//
//                String targetFileName = hashMap.get("Title").replaceAll("[:?\"'\\-\n\r\t]+", " ");
//
//                File targetFile = new File(src.getParentFile(), targetFileName + ".pdf");
//
//                if (!targetFile.exists()) {
//                    src.renameTo(targetFile);
//                }
//            } else {
//                File targetFile = new File(src.getParentFile(), src.getName().replaceAll("-", " "));
//
//                if (!targetFile.exists()) {
//                    src.renameTo(targetFile);
//                }
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
    private static String streamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        inputStream.close();
// StandardCharsets.UTF_8.name() > JDK 7
        return result.toString("UTF-8");
    }

    //    public static String pdf2txt(String fileName) {
//        StringBuilder sb = new StringBuilder();
//        PdfReader reader = null;
//        try {
//            reader = new PdfReader(fileName);
//
//            int n = reader.getNumberOfPages();
//            for (int i = 0; i < n; i++) {
//                sb.append(PdfTextExtractor.getTextFromPage(reader, i + 1, new SimpleTextExtractionStrategy()).trim()).append('\n');
//                //Extracting the content from the different pages
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        reader.close();
//
//        return sb.toString();
//    }
    public static void writeFile(String fileName, String content) {
        try {
            FileOutputStream os = new FileOutputStream(fileName);
            byte[] buffer = content.getBytes(Charset.forName("utf-8"));
            os.write(buffer);
            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // the formatting rules, implemented in a breadth-first DOM traverse
    private static class FormattingVisitor implements NodeVisitor {
        private StringBuilder accum = new StringBuilder(); // holds the accumulated text

        // appends text to the string builder with a simple word wrap method
        private void append(String text) {
            accum.append(text);
        }

        // hit when the node is first seen
        public void head(Node node, int depth) {
            String name = node.nodeName();
            if (node instanceof TextNode)
                append(((TextNode) node).text()); // TextNodes carry all user-readable text in the DOM.
            else if (name.equals("li"))
                append("\n * ");
            else if (name.equals("dt"))
                append("  ");
            else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr"))
                append("\n");
        }

        // hit when all of the node's children (if any) have been visited
        public void tail(Node node, int depth) {
            String name = node.nodeName();
            if (StringUtil.in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5"))
                append("\n");
//            else if (name.equals("a"))
//                append(String.format(" <%s>", node.absUrl("href")));
        }

        @Override
        public String toString() {
            return accum.toString();
        }
    }
}