/*
 * Copyright 2014 Aurélien Broszniowski
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
 * limitations under the License.
 */

/* Pulled from https://github.com/aurbroszniowski/os-platform-finder */

package org.graylog.collector.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is made to get the OS platform name, e.g.
 * <p/>
 * Red Hat Enterprise Linux Server release 5.6 (Tikanga)
 * <p/>
 * <p/>
 * It currently supports :
 * - Linux
 * - Windows
 * - Mac OS
 * - Solaris
 * <p/>
 * TODO : AIX
 *
 * @author Aurelien Broszniowski
 *         http://www.jsoft.biz
 */

public class Os {

    private OsInfo osInfo;

    public Os() {}

    private Os(final String name, final String version, final String arch) {
        if (name != null) {
            // Windows is quite easy to tackle with
            if (name.startsWith("Windows")) {
                this.osInfo = new OsInfo(name, version, arch, name);
            }
            // Mac requires a bit of work, but at least it's consistent
            else if (name.startsWith("Mac")) {
                initMacOsInfo(name, version, arch);
            } else if (name.startsWith("Darwin")) {
                initDarwinOsInfo(name, version, arch);
            }
            // Try to detect other POSIX compliant platforms, now the fun begins
            else for (String linuxName : linux) {
                    if (name.startsWith(linuxName)) {
                        initLinuxOsInfo(name, version, arch);
                    }
                }
        }
        if (this.osInfo == null)
            this.osInfo = new OsInfo(name, version, arch, name);
    }

    private static class SingletonHolder {
        static String name = System.getProperty("os.name");
        static String version = System.getProperty("os.version");
        static String arch = System.getProperty("os.arch");
        private final static Os instance = new Os(name, version, arch);

    }

    public static Os getOs() {
        return SingletonHolder.instance;
    }

    public String getName() {
        return osInfo.getName();
    }

    public String getArch() {
        return osInfo.getArch();
    }

    public String getVersion() {
        return osInfo.getVersion();
    }

    public String getPlatformName() {
        return osInfo.getPlatformName();
    }

    private static final Map<Double, String> macOs = new HashMap<Double, String>();
    private static final Map<Integer, String> darwin = new HashMap<Integer, String>();
    private static final List<String> linux = new ArrayList<String>();

    static {
        macOs.put(10.0, "Puma");
        macOs.put(10.1, "Cheetah");
        macOs.put(10.2, "Jaguar");
        macOs.put(10.3, "Panther");
        macOs.put(10.4, "Tiger");
        macOs.put(10.5, "Leopard");
        macOs.put(10.6, "Snow Leopard");
        macOs.put(10.7, "Snow Lion");
        macOs.put(10.8, "Mountain Lion");
        macOs.put(10.9, "Mavericks");
        macOs.put(10.10, "Yosemite");

        darwin.put(5, "Puma");
        darwin.put(6, "Jaguar");
        darwin.put(7, "Panther");
        darwin.put(8, "Tiger");
        darwin.put(9, "Leopard");
        darwin.put(10, "Snow Leopard");
        darwin.put(11, "Lion");
        darwin.put(12, "Mountain Lion");
        darwin.put(13, "Mavericks");
        darwin.put(14, "Yosemite");

        linux.addAll(Arrays.asList("Linux", "SunOS"));
    }

    private void initMacOsInfo(final String name, final String version, final String arch) {
        String[] versions = version.split("\\.");
        double numericVersion = Double.parseDouble(versions[0] + "." + versions[1]);
        if (numericVersion < 10)
            this.osInfo = new OsInfo(name, version, arch, "Mac OS " + version);
        else
            this.osInfo = new OsInfo(name, version, arch, "OS X " + macOs.get(numericVersion) + " (" + version + ")");
    }

    private void initDarwinOsInfo(final String name, final String version, final String arch) {
        String[] versions = version.split("\\.");
        int numericVersion = Integer.parseInt(versions[0]);
        this.osInfo = new OsInfo(name, version, arch, "OS X " + darwin.get(numericVersion) + " (" + version + ")");
    }

    private void initLinuxOsInfo(final String name, final String version, final String arch) {
        OsInfo osInfo;
        // The most likely is to have a LSB compliant distro
        osInfo = getPlatformNameFromLsbRelease(name, version, arch);

        // Generic Linux platform name
        if (osInfo == null)
            osInfo = getPlatformNameFromFile(name, version, arch, "/etc/system-release");

        File dir = new File("/etc/");
        if (dir.exists()) {
            // if generic 'system-release' file is not present, then try to find another one
            if (osInfo == null)
                osInfo = getPlatformNameFromFile(name, version, arch, getFileEndingWith(dir, "-release"));

            // if generic 'system-release' file is not present, then try to find '_version'
            if (osInfo == null)
                osInfo = getPlatformNameFromFile(name, version, arch, getFileEndingWith(dir, "_version"));

            // try with /etc/issue file
            if (osInfo == null)
                osInfo = getPlatformNameFromFile(name, version, arch, "/etc/issue");

        }

        // if nothing found yet, looks for the version info
        File fileVersion = new File("/proc/version");
        if (fileVersion.exists()) {
            if (osInfo == null)
                osInfo = getPlatformNameFromFile(name, version, arch, fileVersion.getAbsolutePath());
        }

        // if nothing found, well...
        if (osInfo == null)
            osInfo = new OsInfo(name, version, arch, name);

        this.osInfo = osInfo;
    }

    private String getFileEndingWith(final File dir, final String fileEndingWith) {
        File[] fileList = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.endsWith(fileEndingWith);
            }
        });
        if (fileList.length > 0)
            return fileList[0].getAbsolutePath();
        else
            return null;
    }

    private OsInfo getPlatformNameFromFile(final String name, final String version, final String arch, final String filename) {
        if (filename == null)
            return null;
        File f = new File(filename);
        if (f.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(filename));
                return readPlatformName(name, version, arch, br);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    OsInfo readPlatformName(final String name, final String version, final String arch, final BufferedReader br) throws IOException {
        String line;
        String lineToReturn = null;
        int lineNb = 0;
        while ((line = br.readLine()) != null) {
            if (lineNb++ == 0) {
                lineToReturn = line;
            }
            if (line.startsWith("PRETTY_NAME")) return new OsInfo(name, version, arch, line.substring(13, line.length() - 1));
        }
        return new OsInfo(name, version, arch, lineToReturn);
    }

    private OsInfo getPlatformNameFromLsbRelease(final String name, final String version, final String arch) {
        String fileName = "/etc/lsb-release";
        File f = new File(fileName);
        if (f.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(fileName));
                return readPlatformNameFromLsb(name, version, arch, br);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    OsInfo readPlatformNameFromLsb(final String name, final String version, final String arch, final BufferedReader br) throws IOException {
        String distribDescription = null;
        String distribCodename = null;

        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("DISTRIB_DESCRIPTION"))
                distribDescription = line.replace("DISTRIB_DESCRIPTION=", "").replace("\"", "");
            if (line.startsWith("DISTRIB_CODENAME")) distribCodename = line.replace("DISTRIB_CODENAME=", "");
        }
        if (distribDescription != null && distribCodename != null) {
            return new OsInfo(name, version, arch, distribDescription + " (" + distribCodename + ")");
        }
        return null;
    }

    class OsInfo {
        private String name;
        private String arch;
        private String version;
        private String platformName;

        private OsInfo(final String name, final String version, final String arch, final String platformName) {
            this.name = name;
            this.arch = arch;
            this.version = version;
            this.platformName = platformName;
        }

        public String getName() {
            return name;
        }

        public String getArch() {
            return arch;
        }

        public String getVersion() {
            return version;
        }

        public String getPlatformName() {
            return platformName;
        }
    }

}