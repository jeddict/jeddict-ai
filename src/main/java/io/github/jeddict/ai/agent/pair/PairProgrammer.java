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


/**
 * THis is just a marker for PairProgrammer agents like JavadocSpecialist,
 * RestSpecialist, etc.
 *
 *
 */
public interface PairProgrammer {

    public static enum Specialist {
        ADVISOR(CodeAdvisor.class),
        REFACTOR(RefactorSpecialist.class),
        JAVADOC(JavadocSpecialist.class),
        REST(RestSpecialist.class);

        public final Class specialistClass;

        Specialist(final Class specialist) {
            this.specialistClass = specialist;
        }
    }


}
