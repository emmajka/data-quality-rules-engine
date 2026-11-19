package pl.emmajk.dqrules.example;

import pl.emmajk.dqrules.Greeter;

/** Placeholder app that proves the modules are wired. To be replaced by the example HTTP API. */
public final class HelloApp {

    /** Prints the greeting. */
    public static void main(String[] args) {
        System.out.println(new Greeter().greet());
    }
}
