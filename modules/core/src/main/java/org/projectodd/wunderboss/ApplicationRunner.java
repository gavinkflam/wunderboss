/*
 * Copyright 2014 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectodd.wunderboss;

import org.jboss.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ApplicationRunner {

    public ApplicationRunner(String name) {
        this.name = name;
    }

    public void start(String[] args) throws Exception {
        loadProperties();
        extractJar();
        updateClassPath();

        WunderBoss.putOption("argv", args);
        WunderBoss.putOption("root", requiredProperty(properties, "root"));
        String language = requiredProperty(properties, "language");
        log.info("Initializing " + name + " as " + language);
        WunderBoss.findLanguage(language)
                .eval(requiredProperty(properties, "init"));
    }

    protected void loadProperties() throws Exception {
        String internalPath = "META-INF/app.properties";
        log.debugf("Looking for properties file at %s", internalPath);
        InputStream configStream = WunderBoss.classLoader().getResourceAsStream(internalPath);
        if (configStream != null) {
            log.debugf("Found properties file %s", internalPath);
            properties.load(configStream);
        }
        Properties externalProperties = new Properties();
        String externalPath = jarPath();
        if (externalPath.endsWith(".jar")) {
            externalPath = externalPath.replace(".jar", ".properties");
            log.debugf("Looking for properties file at %s", externalPath);
            File externalFile = new File(externalPath);
            if (externalFile.exists()) {
                log.debugf("Found properties file %s", externalPath);
                externalProperties.load(new FileInputStream(externalFile));
                properties.putAll(externalProperties);
            }
        }
    }

    public void stop() {
        if (extractRoot != null) {
            Utils.deleteRecursively(new File(extractRoot));
        }
    }

    protected void extractJar() throws Exception {
        if (!properties.containsKey("extract_paths")) {
            return;
        }
        String[] extractPaths = Utils.classpathStringToArray(properties.getProperty("extract_paths"));
        extractRoot = Files.createTempDirectory("wunderboss").toFile().getPath();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Utils.deleteRecursively(new File(extractRoot));
            }
        });

        ZipInputStream zipStream = new ZipInputStream(new FileInputStream(jarPath()));
        ZipEntry zipEntry = null;
        byte[] buffer = new byte[4096];
        while ((zipEntry = zipStream.getNextEntry()) != null) {
            String name = zipEntry.getName();
            // Only extract entries in the specified directories
            boolean match = false;
            for (String extractPath : extractPaths) {
                if (name.startsWith(extractPath)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                continue;
            }
            File file = new File(extractRoot + "/" + name);
            if (zipEntry.isDirectory()) {
                file.mkdirs();
            } else {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                FileOutputStream extractStream = new FileOutputStream(file);
                try {
                    int bytesRead = -1;
                    while ((bytesRead = zipStream.read(buffer)) != -1) {
                        extractStream.write(buffer, 0, bytesRead);
                    }
                } finally {
                    extractStream.close();
                }
            }
            zipStream.closeEntry();
        }
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            if (value.contains("${extract_root}")) {
                properties.setProperty(key, value.replace("${extract_root}", extractRoot));
            }
        }
        WunderBoss.putOption("extract-root", extractRoot);
    }

    protected void updateClassPath() throws Exception {
        if (properties.containsKey("classpath")) {
            classPathAdditions.addAll(Utils.classpathStringToFiles(properties.getProperty("classpath")));
        }

        for (File file : classPathAdditions) {
            try {
                WunderBoss.updateClassPath(file.toURI().toURL());
            } catch (MalformedURLException ignored) {}
        }
    }

    protected String requiredProperty(Properties properties, String key) {
        if (properties.containsKey(key)) {
            return properties.getProperty(key);
        } else {
            throw new IllegalArgumentException("Required option " + key + " not provided.");
        }
    }

    protected String jarPath() {
        String mainPath = ApplicationRunner.class.getName().replace(".", "/") + ".class";
        String mainUrl = ApplicationRunner.class.getClassLoader().getResource(mainPath).toString();
        int from = "jar:file:".length();
        int to = mainUrl.indexOf("!/");
        return mainUrl.substring(from, to);
    }


    protected String name;
    protected Properties properties = new Properties();
    protected String extractRoot;
    protected List<File> classPathAdditions = new ArrayList<>();

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss");
}