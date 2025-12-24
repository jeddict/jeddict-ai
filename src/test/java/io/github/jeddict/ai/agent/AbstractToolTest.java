package io.github.jeddict.ai.agent;

import io.github.jeddict.ai.test.TestBase;
import io.github.jeddict.ai.test.DummyTool;
import static io.github.jeddict.ai.agent.AbstractTool.PROPERTY_MESSAGE;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

public class AbstractToolTest extends TestBase {

    @Test
    public void constructor_sets_instance_variables() {
        DummyTool tool = new DummyTool(projectDir);

        then(tool.basedir).isSameAs(projectDir);
        then(tool.basepath.toString()).isEqualTo(projectDir);

        tool = new DummyTool();
        then(tool.basedir).isEqualTo(new File(".").getAbsolutePath());
        then(tool.basepath.toString()).isEqualTo(new File(".").getAbsolutePath());
        then(tool.humanInTheMiddle()).isEmpty();

    }

    @Test
    public void basedir_can_not_be_null_or_blank() {
        for (String S: new String[] {null, "  ", "", "\n", " \t"})
       thenThrownBy(() -> { new DummyTool(null); })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("basedir can not be null or blank");
    }

    @Test
    public void fullPath_returns_the_full_path_of_given_relative_path() {
        DummyTool tool = new DummyTool(projectDir);

        then(tool.fullPath("relative")).isEqualTo(Paths.get(projectDir, "relative"));
    }

    @Test
    public void set_and_get_humanInTheMiddle() {
        DummyTool tool = new DummyTool();

        final UnaryOperator<String> hitm = (s) -> {return s;};

        tool.withHumanInTheMiddle(hitm);
        then(tool.humanInTheMiddle()).contains(hitm);

        tool.withHumanInTheMiddle(null);
        then(tool.humanInTheMiddle()).isEmpty();
    }

    @Test
    public void fires_property_change_event() {
        // given
        DummyTool tool = new DummyTool(projectDir);
        final List<PropertyChangeEvent> events = new ArrayList<>();
        PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                events.add(evt);
            }
        };
        tool.addPropertyChangeListener(listener);

        // when
        tool.progress("a message");

        // then
        then(events).hasSize(1);
        then(events.get(0).getPropertyName()).isEqualTo(PROPERTY_MESSAGE);
        then(events.get(0).getNewValue()).isEqualTo("a message");
    }

    @Test
    public void addPropertyChangeListener_does_not_accept_null() {
        // given
        DummyTool tool = new DummyTool(projectDir);

        // when & then
        thenThrownBy(() -> tool.addPropertyChangeListener(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("listener can not be null");
    }

    @Test
    public void removePropertyChangeListener_does_not_accept_null() {
        // given
        DummyTool tool = new DummyTool(projectDir);

        // when & then
        thenThrownBy(() -> tool.removePropertyChangeListener(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("listener can not be null");
    }

    @Test
    public void progress_also_logs_the_message() {
        // given
        DummyTool tool = new DummyTool(projectDir);
        final List<PropertyChangeEvent> events = new ArrayList<>();
        tool.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                events.add(evt);
            }
        });

        // when
        tool.progress("a message");

        // then
        then(events).hasSize(1);
        then(logHandler.getMessages()).contains("a message");
    }

}
