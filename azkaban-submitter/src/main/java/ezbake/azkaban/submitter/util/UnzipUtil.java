/*   Copyright (C) 2013-2014 Computer Sciences Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package ezbake.azkaban.submitter.util;

import com.google.common.base.Optional;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;

/**
 * Created by eperry on 12/10/13.
 */
public class UnzipUtil {
    private static final Logger log = LoggerFactory.getLogger(UnzipUtil.class);

    public static File unzip(File folder, ByteBuffer buf) throws IOException {
        File outputFolder = new File(folder, Long.toString(System.currentTimeMillis()));
        log.debug("Unzipping into " + outputFolder.getAbsoluteFile().getAbsolutePath());

        if (outputFolder.exists()) {
            throw new IOException(String.format("Folder already exists at %s, please attempt submission again", outputFolder.getAbsoluteFile().getAbsolutePath()));
        } else {
            boolean folderCreated = outputFolder.mkdir();
            if (!folderCreated) {
                throw new IOException(String.format("Folder for submission could not be created at %s", outputFolder.getAbsoluteFile().getAbsolutePath()));
            }
        }

        TarInputStream tar = new TarInputStream(new GZIPInputStream(new ByteArrayInputStream(buf.array())));
        TarEntry entry = tar.getNextEntry();

        while (entry != null) {
            String fileName = entry.getName();
            File newFile = new File(outputFolder, fileName);

            //create all non exists folders
            //else you will hit FileNotFoundException for compressed folder
            new File(newFile.getParent()).mkdirs();

            if (!entry.isDirectory()) {
                FileOutputStream fos = new FileOutputStream(newFile);
                tar.copyEntryContents(fos);
                fos.close();
            }
            entry = tar.getNextEntry();
        }

        tar.close();
        return outputFolder;
    }

    public static Optional<String> getConfDirectory(File unzipped) {
        Optional<String> result = Optional.absent();
        File confDir = findSubDir(unzipped, "config");
        if (confDir != null) {
            result = Optional.of(confDir.getAbsolutePath());
        }
        return result;
    }

    public static Optional<String> getSSLPath(File confDir) {
        Optional<String> result = Optional.absent();
        File sslDir = findSubDir(confDir, "ssl");
        if (sslDir != null) {
            // The SSL certs/keys are inside config/ssl/<security ID>
            // It should be the only subdirectory within the ssl dir
            File[] sslDirContents = sslDir.listFiles();
            if (sslDirContents != null && sslDirContents.length > 0 && sslDirContents[0].isDirectory()) {
                result = Optional.of(sslDirContents[0].getAbsolutePath());
            }
        }
        return result;
    }

    public static Optional<String> getJarPath(File unzipped) {
        Optional<String> jarPath = Optional.absent();
        File[] libDirFiles = findSubDir(unzipped, "lib").listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(".jar");
            }
        });
        if (libDirFiles.length > 0) {
            jarPath = Optional.of(libDirFiles[0].getAbsolutePath());
        }
        return jarPath;
    }

    private static File findSubDir(File parent, final String dirName) {
        File[] contents = parent.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.equals(dirName);
            }
        });
        if (contents.length > 0) {
            return contents[0];
        } else {
            // Perform recursive check on subdirectories
            File[] nonFilteredContents = parent.listFiles();
            if (nonFilteredContents != null) {
                for (File sub : nonFilteredContents) {
                    if (sub.isDirectory()) {
                        File result = findSubDir(sub, dirName);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }
        return null;
    }
}
