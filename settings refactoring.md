## Goal
We want to review the PreferencesManager class and refactor it considering the following
requirements and guidelines. Take the deliverables mentioned below as the expected
outcome, which in short at this stage is to have a unit test that covers all
PreferencesManager functionalities.

## Requirements
- Turn the object into a JSONObject as back-bone hashmap based data structure
- extend the JSONObject with specific methods to retrieve underlying well known properties
- keep the same public interface for now so that we do not have to change existing code
- preferably use of simple JSON frameworks, we can use automatic mapping if we
  can easily cover all scenarios

## Expected deliverable
- A test class that covers all functionality currently provided by PreferencesManager
- The unit test class shall successfully pass all tests

## Guidelines
- Follow a TDD workflow:
  - Do not write production code if we do not have a test that fails
  - Address one feature or piece of a feature at a time and write in a test the
    expected behaviour of production code
  - check the test fails
  - write the simplest code possible that makes the test pass and verify the test
    executes successfully
  - iterate until the feature is done
- Use snake convention for test names (not for classes or production code)
- Use AssertJ as assertions framework
- Use AssertJ BDDAssertions' then(), not asssertThat()
- Maven tool is in/opt/apache-maven-latest/bin
- Use JDK HOME /usr/lib/jvm/java-21-openjdk-amd64/bin/java

## References
src/main/java/io/github/jeddict/ai/settings/PreferencesManager.java
src/test/java/io/github/jeddict/ai/settings/PreferencesManagerTest.java
src/test/resources/settings/jeddict.json

