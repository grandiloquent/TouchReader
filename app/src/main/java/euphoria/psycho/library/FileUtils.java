package euphoria.psycho.library;

import android.os.Environment;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileUtils {

    public static String getFileNameWithoutExtension(String path) {
        return path.replaceFirst("[.][^.]+$", "");
    }

    public static String cutLastSegmentOfPath(String path) {
        if (path.length() - path.replace("/", "").length() <= 1)
            return "/";
        String newPath = path.substring(0, path.lastIndexOf("/"));
        // We don't need to list the content of /storage/emulated
        if (newPath.equals("/storage/emulated"))
            newPath = "/storage";
        return newPath;
    }

    public static ArrayList<File> getFileListByDirPath(String path, FileFilter filter) {
        File directory = new File(path);
        ArrayList<File> result = new ArrayList<>();
        File[] files = directory.listFiles(filter);

        if (files == null) {
            return new ArrayList<>();
        }
        for (File f : files) {
            result.add(f);
        }

        Collections.sort(result, new FileComparator());
        return result;
    }

    public static void fileCombine(String destinationFileName, String[] files) {
        try {
            FileOutputStream out = new FileOutputStream(destinationFileName);
            byte[] buf = new byte[1024];
            for (String file : files) {
                InputStream in = new FileInputStream(file);
                int b = 0;
                while ((b = in.read(buf)) >= 0) {
                    out.write(buf, 0, b);
                    out.flush();
                }
                out.write("\r\n".getBytes(Charset.forName("UTF-8")));
                out.flush();
                closeSilently(in);
            }
            closeSilently(out);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private static String getPattern(String fileName) {
        if (fileName.contains(" Pt.")) {
            fileName = fileName.split("Pt\\.")[0];
        }
        if (fileName.contains(" Ch.")) {
            fileName = fileName.split("Ch\\.")[0];
        }
        fileName = fileName.replaceAll("[0-9]+$", "");

        return fileName.trim();
    }

    public static void fileCombineInDirectories(String dir) {

        File patternDirectory = new File(dir);
        File parent = patternDirectory.getParentFile();
        if (parent == null) return;
        final String pattern = getPattern(patternDirectory.getName());//patternDirectory.getName().replaceAll("((Pt|Ch)\\.\\s+)*[0-9]+$", "");
        File dst = new File(new File(Environment.getExternalStorageDirectory(), ".readings"), ".readings");
        dst.mkdir();

        String destinationFileName = new File(dst, pattern + ".txt").getAbsolutePath();

        File[] directories = parent.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory() && file.getName().startsWith(pattern)) return true;
                return false;
            }
        });
        Arrays.sort(directories, new Comparator<File>() {
            @Override
            public int compare(File file, File t1) {
                return file.getName().compareTo(t1.getName());
            }
        });
        FileOutputStream out = null;
        byte[] buf = new byte[1024];
        try {
            out = new FileOutputStream(destinationFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        for (File directory : directories) {

            File[] txtFiles = directory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    if (file.isFile() && file.getName().endsWith(".txt")) return true;
                    return false;
                }
            });
            Arrays.sort(txtFiles, new Comparator<File>() {
                @Override
                public int compare(File file, File t1) {
                    return file.getName().compareTo(t1.getName());
                }
            });
            for (File file : txtFiles) {
                InputStream in = null;
                try {
                    in = new FileInputStream(file);

                    int b = 0;
                    while ((b = in.read(buf)) >= 0) {
                        out.write(buf, 0, b);
                        out.flush();
                    }
                    out.write("\r\n".getBytes(Charset.forName("UTF-8")));
                    out.flush();
                    closeSilently(in);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        }
        closeSilently(out);

        File destinationDirectory = new File(new File(Environment.getExternalStorageDirectory(), ".readings"), ".combined");

        destinationDirectory.mkdir();

        for (File file : directories) {
            file.renameTo(new File(destinationDirectory, file.getName()));
        }
    }

    public static void closeSilently(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
