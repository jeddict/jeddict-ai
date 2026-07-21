/*
 * Copyright 2026 the original author or authors from the LLMTooliy project
 * (https://stefanofornari.github.io/llm-toolify).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.jeddict.ai.test;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import org.netbeans.modules.gradle.api.execute.GradleExecConfiguration;
import org.netbeans.spi.project.ProjectConfigurationProvider;

/**
 * A test-only dummy implementation of NetBeans' configuration SPI.
 * This guarantees the Gradle engine sees a Gradle config structure
 * instead of accidentally resolving a Maven config fallback.
 */
class DummyGradleConfigurationProvider implements ProjectConfigurationProvider<GradleExecConfiguration> {

    // Create a dummy token instance representing a default configuration
    private final GradleExecConfiguration defaultConfiguration = new GradleExecConfiguration("DEFAULT");

    @Override
    public Collection<GradleExecConfiguration> getConfigurations() {
        return Collections.singletonList(defaultConfiguration);
    }

    @Override
    public GradleExecConfiguration getActiveConfiguration() {
        return defaultConfiguration;
    }

    @Override
    public void setActiveConfiguration(GradleExecConfiguration configuration) {
        // No-op inside our test framework
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener jpcl) {}

    @Override
    public void removePropertyChangeListener(PropertyChangeListener jpcl) {}

    @Override
    public boolean hasCustomizer() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void customize() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean configurationsAffectAction(String string) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
