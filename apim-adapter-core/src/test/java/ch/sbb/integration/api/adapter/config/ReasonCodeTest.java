package ch.sbb.integration.api.adapter.config;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ReasonCodeTest {

    private static final Logger LOG = LoggerFactory.getLogger(ReasonCodeTest.class);

    @Test
    public void testForMismatchedCodes() {
        Arrays.stream(ReasonCode.values()).forEach(reasonCode -> {
            assertTrue("ReasonCode name should start with APIM_<code> check: " + reasonCode.name(),
                    reasonCode.name().startsWith("APIM_" + reasonCode.getCode()));
        });
    }

    @Test
    public void testLogPatternWithoutStringFormat() {
        Arrays.stream(ReasonCode.values()).filter(reasonCode -> StringUtils.containsAny(reasonCode.pattern(), "%s", "%d", "%f")).findFirst().ifPresent(reasonCode -> fail("Log Pattern contains string format patterns: " + reasonCode.pattern()));
    }

    public void testPatternFormat() {
        assertTrue(ReasonCode.APIM_1007.pattern().contains("{}"));
        assertFalse(ReasonCode.APIM_1007.pattern().contains("%s"));

        assertFalse(ReasonCode.APIM_1007.stringFormat().contains("{}"));
        assertTrue(ReasonCode.APIM_1007.stringFormat().contains("%s"));
    }

    @Test
    public void testGaps() {
        AtomicInteger last = new AtomicInteger(1000);
        final String gaps = Arrays.stream(ReasonCode.values()).map(ReasonCode::getCode).sorted().map(i -> {
            final Integer gap = i - last.get();
            last.set(i);
            if (gap > 1 && gap < 100) { // 100 to skip bigger jumps (e.g. 1xxx to 2xxx)
                final int gapNr = i - 1;
                LOG.info("{}", gapNr);
                return gapNr;
            } else {
                return null;
            }
        }).filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(", "));
        if (!gaps.isEmpty()) {
            fail("Encountered reason code gaps (make use of deprecations instead of deletion): " + gaps);
        }
    }
}