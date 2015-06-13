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

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Aurelien Broszniowski
 */

public class OsTest {

    @Test
    public void testReleaseFileWithLinuxPrettyName() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        BufferedReader mockFile = mock(BufferedReader.class);
        when(mockFile.readLine()).thenReturn("NAME=Fedora", "PRETTY_NAME=\"Fedora 17 (Beefy Miracle)\"", "VERSION_ID=17", null);

        String name = "some name";
        String version = "4.1.4";
        String arch = "68000";
        Os.OsInfo osInfo = new Os().readPlatformName(name, version, arch, mockFile);
        Assert.assertThat(osInfo.getName(), is(equalTo(name)));
        Assert.assertThat(osInfo.getVersion(), is(equalTo(version)));
        Assert.assertThat(osInfo.getArch(), is(equalTo(arch)));
        Assert.assertThat(osInfo.getPlatformName(), is(equalTo("Fedora 17 (Beefy Miracle)")));
    }

    @Test
    public void testReleaseFileWithOneLine() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        BufferedReader mockFile = mock(BufferedReader.class);
        String line = "Fedora version 19";
        when(mockFile.readLine()).thenReturn(line, null);

        String name = "some name";
        String version = "4.1.4";
        String arch = "68000";
        Os.OsInfo osInfo = new Os().readPlatformName(name, version, arch, mockFile);
        Assert.assertThat(osInfo.getName(), is(equalTo(name)));
        Assert.assertThat(osInfo.getVersion(), is(equalTo(version)));
        Assert.assertThat(osInfo.getArch(), is(equalTo(arch)));
        Assert.assertThat(osInfo.getPlatformName(), is(equalTo(line)));
    }

    @Test
    public void testReleaseFileWithTwoLines() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        BufferedReader mockFile = mock(BufferedReader.class);
        when(mockFile.readLine()).thenReturn("Fedora version 19", "second line", null);

        String name = "some name";
        String version = "4.1.4";
        String arch = "68000";
        Os.OsInfo osInfo = new Os().readPlatformName(name, version, arch, mockFile);
        Assert.assertThat(osInfo.getName(), is(equalTo(name)));
        Assert.assertThat(osInfo.getVersion(), is(equalTo(version)));
        Assert.assertThat(osInfo.getArch(), is(equalTo(arch)));
        Assert.assertThat(osInfo.getPlatformName(), is(equalTo("Fedora version 19")));
    }

    @Test
    public void testLsbRelease() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        BufferedReader mockFile = mock(BufferedReader.class);
        when(mockFile.readLine()).thenReturn("DISTRIB_ID=Ubuntu", "DISTRIB_RELEASE=9.10", "DISTRIB_CODENAME=karmic",
                "DISTRIB_DESCRIPTION=\"Ubuntu 9.10\"", null);

        String name = "some name";
        String version = "4.1.4";
        String arch = "68000";
        Os.OsInfo osInfo = new Os().readPlatformNameFromLsb(name, version, arch, mockFile);
        Assert.assertThat(osInfo.getName(), is(equalTo(name)));
        Assert.assertThat(osInfo.getVersion(), is(equalTo(version)));
        Assert.assertThat(osInfo.getArch(), is(equalTo(arch)));
        Assert.assertThat(osInfo.getPlatformName(), is(equalTo("Ubuntu 9.10 (karmic)")));
    }

}