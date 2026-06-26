/*
 * Copyright 2026 the original author or authors from the LLMToolify project
 * (https://github.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://apache.org
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.netbeans.modules.gradle.api;

import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import org.netbeans.api.project.Project;

/**
 * Shadow test-classpath implementation used to bypass NetBeans'
 * platform initialization layer during un-attached test runs.
 */
public class NbGradleProject {

    private final Project project;
    private final Preferences dummyPreferences = new TestPreferences();

    private NbGradleProject(Project project) {
        this.project = project;
    }

    /**
     * Bypasses the default system lookup factories to return
     * our custom testing-wrapper token.
     */
    public static NbGradleProject get(Project project) {
        return new NbGradleProject(project);
    }

    /**
     * Fixes the internal NullPointerException by natively supplying
     * a safe dummy backing preferences instance to RunUtils.
     */
    public Preferences getPreferences(boolean shared) {
        return dummyPreferences;
    }

    public Object projectLookup(Class c) {
        return project.getLookup().lookup(c);
    }

    public static Preferences getPreferences(Project project, boolean shared) {
        return new NbGradleProject(project).getPreferences(shared);
    }
    
    /**
     * Dummy inline implementation of the Preferences interface to fulfill
     * RunUtils configuration checks without accessing system registry pathways.
     */
    private static class TestPreferences extends Preferences {
        @Override public void put(String key, String value) {}
        @Override public String get(String key, String def) { return def; }
        @Override public void remove(String key) {}
        @Override public void clear() throws BackingStoreException {}
        @Override public void putInt(String key, int value) {}
        @Override public int getInt(String key, int def) { return def; }
        @Override public void putLong(String key, long value) {}
        @Override public long getLong(String key, long def) { return def; }
        @Override public void putBoolean(String key, boolean value) {}
        @Override public boolean getBoolean(String key, boolean def) { return def; }
        @Override public void putFloat(String key, float value) {}
        @Override public float getFloat(String key, float def) { return def; }
        @Override public void putDouble(String key, double value) {}
        @Override public double getDouble(String key, double def) { return def; }
        @Override public void putByteArray(String key, byte[] value) {}
        @Override public byte[] getByteArray(String key, byte[] def) { return def; }
        @Override public String[] keys() throws BackingStoreException { return new String[0]; }
        @Override public String[] childrenNames() throws BackingStoreException { return new String[0]; }
        @Override public Preferences parent() { return null; }
        @Override public Preferences node(String pathName) { return this; }
        @Override public boolean nodeExists(String pathName) throws BackingStoreException { return true; }
        @Override public void removeNode() throws BackingStoreException {}
        @Override public String name() { return "test"; }
        @Override public String toString() { return "TestPreferences"; }
        @Override public void flush() throws BackingStoreException {}
        @Override public void sync() throws BackingStoreException {}
        @Override public void addPreferenceChangeListener(PreferenceChangeListener pcl) {}
        @Override public void removePreferenceChangeListener(PreferenceChangeListener pcl) {}
        @Override public void addNodeChangeListener(NodeChangeListener ncl) {}
        @Override public void removeNodeChangeListener(NodeChangeListener ncl) {}
        @Override public void exportNode(java.io.OutputStream os) throws java.io.IOException, BackingStoreException {}
        @Override public void exportSubtree(java.io.OutputStream os) throws java.io.IOException, BackingStoreException {}
        @Override public String absolutePath() { return "/"; }
        @Override public boolean isUserNode() { return true; }
    }
}
