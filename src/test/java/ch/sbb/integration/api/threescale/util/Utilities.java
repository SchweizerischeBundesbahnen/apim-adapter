package ch.sbb.integration.api.threescale.util;

import ch.sbb.integration.api.threescale.service.ThreeScaleAdapterServiceTest;

import java.io.InputStream;
import java.util.Scanner;

/**
 * Created by u217269 on 26.02.2018.
 */
public class Utilities {

    public static void tryToSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String readFile(String fileName) {
        InputStream resourceAsStream = ThreeScaleAdapterServiceTest.class.getResourceAsStream(fileName);
        try (Scanner scanner = new Scanner(resourceAsStream, "utf-8")) {
            return scanner.useDelimiter("\\Z").next();
        }
    }

}
