package ch.sbb.integration.api.adapter.util;

import ch.sbb.integration.api.adapter.service.ApimAdapterServiceTest;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
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
        InputStream resourceAsStream = ApimAdapterServiceTest.class.getResourceAsStream(fileName);
        try (Scanner scanner = new Scanner(resourceAsStream, "utf-8")) {
            return scanner.useDelimiter("\\Z").next();
        }
    }

    public static String loadTextFromResource(String path) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(path);

        if(url == null) {
            throw new IllegalArgumentException("Could not find path: " + path + ", in resources");
        }

        StringWriter writer = new StringWriter();
        IOUtils.copy(url.openStream(), writer, Charset.forName("UTF8"));
        return writer.toString();
    }

}
