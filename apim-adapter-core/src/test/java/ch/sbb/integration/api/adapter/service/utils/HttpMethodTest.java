package ch.sbb.integration.api.adapter.service.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HttpMethodTest {

    @Test
    public void parse() {
        assertNull(HttpMethod.parse(null));
        assertNull(HttpMethod.parse(""));
        assertNull(HttpMethod.parse(" "));
        assertNull(HttpMethod.parse("GEX"));

        assertEquals(HttpMethod.GET, HttpMethod.parse("GET"));
        assertEquals(HttpMethod.GET, HttpMethod.parse("get"));
        assertEquals(HttpMethod.GET, HttpMethod.parse(" get "));
        assertEquals(HttpMethod.OPTIONS, HttpMethod.parse("OPTIONS "));
        assertEquals(HttpMethod.OPTIONS, HttpMethod.parse("Options"));
    }
}