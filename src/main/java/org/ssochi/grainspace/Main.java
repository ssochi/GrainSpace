package org.ssochi.grainspace;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int len = 2048,light = 10000;
        if (args.length >= 2){
            len = Integer.parseInt(args[0]);
            light = Integer.parseInt(args[1]);
        }

        GrainSpace space = new GrainSpace(len,light);
        space.run();
    }
}
