package ch.sbb.integration.api.adapter.service.configuration;

import org.junit.Before;
import org.junit.Test;

import static ch.sbb.integration.api.adapter.model.ConfigType.METRIC;
import static ch.sbb.integration.api.adapter.model.ConfigType.PLAN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class EmergencyModeStateTest {

    private EmergencyModeState testee;

    @Before
    public void setup() {
        testee = new EmergencyModeState();
    }

    @Test
    public void changeMetricToFalse() {
        //Act
        testee.setConfigurationSuccessfulLoaded(METRIC, false);

        //Assert
        assertThat(testee.isEmergencyMode(), is(true));
    }

    @Test
    public void changeMetricAndPlanToFalse() {
        //Act
        testee.setConfigurationSuccessfulLoaded(METRIC, false);
        testee.setConfigurationSuccessfulLoaded(PLAN, false);

        //Assert
        assertThat(testee.isEmergencyMode(), is(true));
    }

    @Test
    public void changeMetricAndPlanToFalseAndThenBothToTrue() {
        //Act
        testee.setConfigurationSuccessfulLoaded(METRIC, false);
        testee.setConfigurationSuccessfulLoaded(PLAN, false);

        testee.setConfigurationSuccessfulLoaded(METRIC, true);
        testee.setConfigurationSuccessfulLoaded(PLAN, true);

        //Assert
        assertThat(testee.isEmergencyMode(), is(false));
    }

    @Test
    public void changeMetricAndPlanToFalseAndThenMetricToTrue() {
        //Act
        testee.setConfigurationSuccessfulLoaded(METRIC, false);
        testee.setConfigurationSuccessfulLoaded(PLAN, false);

        testee.setConfigurationSuccessfulLoaded(METRIC, true);

        //Assert
        assertThat(testee.isEmergencyMode(), is(true));
    }

    @Test
    public void changeMetriToFalseAndThenBackToThenPlanToFalseAndThenBack() {
        //Act 1
        testee.setConfigurationSuccessfulLoaded(METRIC, false);

        //Assert 1
        assertThat(testee.isEmergencyMode(), is(true));

        //Act 2
        testee.setConfigurationSuccessfulLoaded(METRIC, true);

        //Assert 2
        assertThat(testee.isEmergencyMode(), is(false));

        //Act 3
        testee.setConfigurationSuccessfulLoaded(PLAN, false);

        //Assert 3
        assertThat(testee.isEmergencyMode(), is(true));

        //Act 4
        testee.setConfigurationSuccessfulLoaded(PLAN, true);

        //Assert 4
        assertThat(testee.isEmergencyMode(), is(false));
    }


}