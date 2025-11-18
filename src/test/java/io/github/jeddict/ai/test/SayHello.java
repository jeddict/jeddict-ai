//
// DO NOT CHANGE THIS CLASS. IT IS USED IN MANY TEST CASES AND POSITION OF TEXT
// IS RELEVANT AND IMPORTANT.
//
package io.github.jeddict.ai.test;

import jakarta.ws.rs.Path;

@Path("/greetings")  // to test create rest endpoint hint
public class SayHello {
    public void sayHello(String name) {
        if (name != null) {
            System.out.println("Hello, " + name + "!");
        } else {
            System.out.println("What's your name?");
        }
    }

    class InnerClass { } /* to test inner class suggestions */
}
