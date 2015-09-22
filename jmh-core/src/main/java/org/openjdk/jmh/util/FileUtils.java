/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jmh.util;

import java.io.*;
import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A utility class for File creation and manipulation.
 */
public class FileUtils {
    // Static access only
    private FileUtils() {

    }

    public static File tempFile(String suffix) throws IOException {
        File file = File.createTempFile("jmh", suffix);
        file.deleteOnExit();
        return file;
    }

    /**
     * Helper method for extracting a given resource to File
     *
     * @param name name of the resource
     * @return a File pointing to the extracted resource
     * @throws IOException if things go crazy
     */
    public static File extractFromResource(String name) throws IOException {
        InputStream fis = null;
        OutputStream fos = null;
        try {
            File temp = FileUtils.tempFile("extracted");
            fis = FileUtils.class.getResourceAsStream(name);
            fos = new FileOutputStream(temp);

            byte[] buf = new byte[8192];
            int read;
            while ((read = fis.read(buf)) != -1) {
                fos.write(buf, 0, read);
            }

            fos.close();

            return temp;
        } finally {
            FileUtils.safelyClose(fis);
            FileUtils.safelyClose(fos);
        }
    }

    /**
     * Create a temporary file (see {@link File#createTempFile(String, String)}) and fill it with the given lines.
     *
     * @param suffix file suffix {@link File#createTempFile(String, String)}
     * @param lines to be written
     *
     * @return the temporary file absolute path
     * @throws IOException on file creation error
     */
    public static String createTempFileWithLines(String suffix, Iterable<String> lines)
            throws IOException {
        File file = FileUtils.tempFile(suffix);
        PrintWriter pw = new PrintWriter(file);
        for (String l : lines) {
            pw.println(l);
        }
        pw.close();
        return file.getAbsolutePath();
    }

    public static Collection<String> tail(File file, int num) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            LinkedList<String> lines = new LinkedList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                if (lines.size() > num) {
                    lines.remove(0);
                }
            }
            return lines;
        } finally {
            FileUtils.safelyClose(fis);
        }
    }

    public static Collection<String> readAllLines(Reader src) throws IOException {
            BufferedReader reader = new BufferedReader(src);
            List<String> lines = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
    }

    public static Collection<String> readAllLines(File file) throws IOException {
        FileReader fr = null;
        try {
            fr = new FileReader(file);
            return readAllLines(fr);
        } finally {
            FileUtils.safelyClose(fr);
        }
    }

    public static Collection<File> getClasses(File root) {
        Collection<File> result = new ArrayList<File>();

        List<File> newDirs = new ArrayList<File>();
        newDirs.add(root);
        while (!newDirs.isEmpty()) {
            List<File> add = new ArrayList<File>();
            for (File dir : newDirs) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isDirectory()) {
                            add.add(f);
                        } else {
                            if (f.getName().endsWith(".class")) {
                                result.add(f);
                            }
                        }
                    }
                }
            }
            newDirs.clear();
            newDirs = add;
        }

        return result;
    }

    public static void copy(String src, String dst) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dst);

            byte[] buf = new byte[8192];
            int read;
            while ((read = fis.read(buf)) != -1) {
                fos.write(buf, 0, read);
            }

            fos.close();
        } finally {
            FileUtils.safelyClose(fis);
            FileUtils.safelyClose(fos);
        }
    }

    public static void safelyClose(OutputStream out) {
        if (out != null) {
            try {
                out.flush();
            } catch (IOException e) {
                // ignore
            }
            try {
                out.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static void safelyClose(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static void safelyClose(Channel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    public static void safelyClose(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    public static void touch(String f) throws IOException {
        File file = new File(f);
        try {
            if (file.createNewFile() || file.canWrite()) {
                return;
            }
        } catch (IOException e) {
            // fall-through
        }
        throw new IOException("The file is not writable: " + f);
    }
}
