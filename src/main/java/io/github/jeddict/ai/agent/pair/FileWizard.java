package io.github.jeddict.ai.agent.pair;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.apache.commons.lang3.StringUtils;


/**
 * Used by the New/Other.../Other/Generate File Using AI
 *
 */
public interface FileWizard extends PairProgrammer {

    static final String SYSTEM_MESSAGE = """
You are a programmer assistant that can generate a new file with code or other
content based on the request and context provided by the user. Generate the file
content only without any description.
Take into account the following general rules: {{globalRules}}
Take into account the following project rules: {{projectRules}}
""";

    static final String USER_MESSAGE_DEFAULT = """
Generate the content of a new file given the below context information.
""";

    static final String USER_MESSAGE = """
{{prompt}}
filename: {{filename}}
context: {{context}}
content:
```
{{content}}
```
project info: {{project}}
""";

    @SystemMessage(SYSTEM_MESSAGE)
    @Agent("Generate content for a new file given the provided context")
    String _newFile_(
        @UserMessage       final String message,     // default or user provided prompt
        @V("context")      final String context,     // additional context provided by the user
        @V("filename")     final String filename,    // filename of the new file
        @V("content")      final String content,     // content of a template/reference/sample file
        @V("project")      final String project,     // project info (JDK, j2EE versions)
        @V("globalRules")  final String globalRules, // global rules
        @V("projectRules") final String projectRules // project rules
    );

    default String newFile(
        final String message,
        final String context,
        final String filename,
        final String content,
        final String project,
        final String globalRules,
        final String projectRules
    ) {
        LOG.finest(() -> "\n"
                + "message: " + StringUtils.abbreviate(message, 80) + "\n"
                + "context: " + StringUtils.abbreviate(context, 80) + "\n"
                + "filename: " + StringUtils.abbreviate(filename, 80) + "\n"
                + "content: " + StringUtils.abbreviate(content, 80) + "\n"
                + "project: " + StringUtils.abbreviate(project, 80) + "\n"
                + "globalRules: " + StringUtils.abbreviate(globalRules, 80) + "\n"
                + "projectRules: " + StringUtils.abbreviate(projectRules, 80) + "\n"
        );

        return _newFile_(
            USER_MESSAGE.replace("{{prompt}}", StringUtils.defaultIfBlank(message, USER_MESSAGE_DEFAULT)),
            StringUtils.defaultIfBlank(context, ""),
            StringUtils.defaultIfBlank(filename, ""),
            StringUtils.defaultIfBlank(content, ""),
            StringUtils.defaultIfBlank(project, ""),
            StringUtils.defaultIfBlank(globalRules, ""),
            StringUtils.defaultIfBlank(projectRules, "")
        );
    }

}
