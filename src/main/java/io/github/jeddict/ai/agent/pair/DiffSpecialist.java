/**
 * Copyright 2025 the original author or authors from the Jeddict project (https://jeddict.github.io/).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.jeddict.ai.agent.pair;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.apache.commons.lang3.StringUtils;


/**
 * The Shakespeare interface defines an agent for fixing or enhancing text in
 * java strings.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Reviewing provided text with respect to the associated Java code.</li>
 *   <li>Correcting and improving text to ensure clarity, engagement, and polish.</li>
 * </ul>
 *
 * This interface extends {@link PairProgrammer}, thereby fitting into a broader
 * programming assistance context.
 */
/**
 * This interface extends the PairProgrammer interface and provides methods for
 * reviewing code changes and suggesting commit messages.
 * It includes an enumeration for different levels of code review, and constants
 * for system and user messages used in the review process.
 */
public interface DiffSpecialist extends PairProgrammer {

    enum CodeReviewLevel {
        BALANCED("balanced"),
        HIGH("high"),
        LOW("low");

        final String level;

        CodeReviewLevel(final String level) {
            this.level = level;
        }

        String prompt() {
            return switch(this) {
                case BALANCED -> "Provide a balanced review. Focus on important improvements but avoid nitpicking trivial details.";
                case LOW -> "Be minimal. Only report critical issues such as bugs, performance bottlenecks, or major violations of best practices.";
                case HIGH -> "Be exhaustive. Suggest all potential improvements, even minor ones. Include naming, formatting, comments, and any best practice observations.";
            };
        }

        static CodeReviewLevel of(String value) {
            for(CodeReviewLevel level: values()) {
                if (level.level.equals(value)) {
                    return level;
                }
            }

            throw new IllegalArgumentException("unknwn level '%s'".formatted(value));
        }
    }

    public static final String SYSTEM_MESSAGE = """
You are an experience developer specialized in in writing commit messages or
code review based on user request and provided git diff output.
Based on the provided diff and an optional reference comment, you can:
- create commit messages that reflect business or domain features rather than technical details like dependency updates or refactoring
- create the following versions of the commit message:
  - Very short
  - Short
  - Medium length
  - Long
  - Descriptive
- review the changes and provide feedback following the bewlow instructions:
  - base your review strictly on the provided Git diff
  - focus areas:
    - bugs, unsafe behavior, or runtime risks
    - readability, naming, and clarity
    - best practices or conventions
    - opportunities for simplification or performance improvement
  - anchor each suggestion to a specific hunk header from the diff
  - DO NOT infer or hallucinate line numbers not present in the diff
  - DO NOT reference line numbers or attempt to estimate exact start/end lines

{{format}}
""";

    public static final String USER_MESSAGE = "{{prompt}}\n{{diff}}\nDiff description: {{description}}";

    public static final String USER_MESSAGE_COMMENT = """
Provide various types of commit messages (very short, short, medium length, long, descriptive)
based on reference comment and changes below.
""";

    public static final String OUTPUT_REVIEW = """
Respond only with a YAML array of review suggestions. Each suggestion must include:
- file: the file name
- hunk: the Git diff hunk header (e.g., "@@ -10,7 +10,9 @@")
- type: one of "security", "warning", "info", or "suggestion"
  - "security" for vulnerabilities or high-risk flaws
  - "warning" for potential bugs or unsafe behavior
  - "info" for minor issues or readability
  - "suggestion" for non-critical improvements or refactoring
- title: a short title summarizing the issue
- description: a longer explanation or recommendation

Output raw YAML with no markdown, code block, or extra formatting.

Expected YAML format:

- file: src/com/example/MyService.java
  hunk: "@@ -42,6 +42,10 @@"
  type: warning
  title: "Possible null pointer exception"
  description: "The 'items' list might be null before iteration. Add a null check to avoid NPE."
""";

    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage(USER_MESSAGE)
    @Agent("Suggest commit messages for the given git diff")
    String review(
        @V("prompt") final String referenceMessage,
        @V("diff") final String diff,
        @V("description") final String diffDescription,
        @V("format") final String format
    );

    /**
     * Suggests commit messages based on the provided diff and reference message.
     *
     * @param diff The diff content to be analyzed for generating commit messages.
     * @param referenceMessage The reference message to be used as a basis for generating commit messages.
     *
     * @return A string containing the suggested commit messages.
     */
    default String suggestCommitMessages(
        final String diff,
        final String referenceMessage
    ) {
        //
        // NOTE for the reviewer: this functionality is invoked from GenerateCommitMessageAction,
        // which creates a new AssistantChatManager on which askQueryForProjectCommit
        // is called. This means that despite the original code was carrying on
        // pictures and history, those data are always be empty.
        // Therefore I have removed them from here.
        //
        LOG.finest(() -> "\nreferenceMessage: %s\ndiff:%s".formatted(StringUtils.abbreviate(referenceMessage, 80), StringUtils.abbreviate(diff, 80)));
        return review(
            USER_MESSAGE_COMMENT,
            StringUtils.defaultString(diff),
            StringUtils.defaultString(referenceMessage),
        "");
    }

    /**
     * Reviews the changes based on the provided diff, level, and feature context.
     *
     *  @param diff The string representation of changes to be reviewed.
     *  @param level The level of detail for the code review.
     *  @param featureContext The context of the feature being reviewed.
     *
     * @return A string containing the review of the changes.
     */
    default String reviewChanges(
        final String diff,
        final String level,
        final String featureContext
    ) {
        //
        // NOTE for the reviewer: this functionality is invoked from GenerateCommitMessageAction,
        // which creates a new AssistantChatManager on which askQueryForProjectCommit
        // is called. This means that despite the original code was carrying on
        // pictures and history, those data are always be empty.
        // Therefore I have removed them from here.
        //
        LOG.finest(() -> "\ndiff:%s".formatted(StringUtils.abbreviate(diff, 80)));
        return review(CodeReviewLevel.of(level).prompt(), diff, featureContext, OUTPUT_REVIEW);
    }
}
