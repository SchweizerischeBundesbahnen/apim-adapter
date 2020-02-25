package ch.sbb.integration.api.adapter.model.reporting;

import org.junit.Test;

import java.util.Map;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class HitsTest {

    private final static ResponseSummary RESPONSE_SUMMARY_1 = new ResponseSummary("CLIENT", SC_OK, "GETS");
    private final static ResponseSummary RESPONSE_SUMMARY_2 = new ResponseSummary("CLIENT", SC_OK, "POSTS");

    Hits testee = new Hits();

    @Test
    public void extractEmptyUnreportedHits() {
        //Act
        Map<ResponseSummary, Long> entries = testee.extractUnreportedHits();

        //Assert
        assertTrue(entries.isEmpty());
    }

    @Test
    public void extracUnreportedHits() {
        //Arrange
        testee.addUnreportHits(RESPONSE_SUMMARY_1, 1L);
        testee.addUnreportHits(RESPONSE_SUMMARY_2, 1L);
        testee.addUnreportHits(RESPONSE_SUMMARY_1, 2L);

        //Act 1
        Map<ResponseSummary, Long> entries = testee.extractUnreportedHits();


        //Assert 1
        assertThat(entries.size(), is(2));

        assertThat(entries.get(RESPONSE_SUMMARY_1), is(3L));
        assertThat(entries.get(RESPONSE_SUMMARY_2), is(1L));

        //Act 2
        entries = testee.extractUnreportedHits();


        //Assert 2
        assertThat(entries.size(), is(0));
    }

}