package ch.sbb.integration.api.adapter.model;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class MappingRuleTest {


	public static final String ID = "id";
	public static final String METRIC_ID = "metricId";
	public static final String METHOD = "get";

	@Before
	public void setup() {
	}

	@Test
	public void whenNoWildcardAndNoDollar_thenNoWildcardAndNoFixEnd() {
		//Act
		MappingRule mappingRule = new MappingRule(ID, METRIC_ID, "/url/pattern/", METHOD);

		//Assert
		String transferedPattern = mappingRule.getPattern().pattern();
		assertThat(transferedPattern, is("/url/pattern/.*"));
	}

	@Test
	public void whenNoWildcardAndDollar_thenNoWildcardAndFixEnd() {
		//Act
		MappingRule mappingRule = new MappingRule(ID, METRIC_ID, "/url/{pattern}/$", METHOD);

		//Assert
		String transferedPattern = mappingRule.getPattern().pattern();
		assertThat(transferedPattern, is("/url/.+/"));

		assertTrue("/url/something/".matches(transferedPattern));
		assertFalse("/url/foo/bar".matches(transferedPattern));
	}

	@Test
	public void whenUrlWildcard_thenUrlWildcard() {
		//Act
		MappingRule mappingRule = new MappingRule(ID, METRIC_ID, "/url/{pattern}/", METHOD);

		//Assert
		String transferedPattern = mappingRule.getPattern().pattern();
		assertThat(transferedPattern, is("/url/.+/.*"));

		assertTrue("/url/something/".matches(transferedPattern));
		assertTrue("/url/foo/bar".matches(transferedPattern));
	}

	@Test
	public void whenUrlEndWildcard_thenUrlEndWildcard() {
		//Act
		MappingRule mappingRule = new MappingRule(ID, METRIC_ID, "/url/{pattern}", METHOD);

		//Assert
		String transferedPattern = mappingRule.getPattern().pattern();
		assertThat(transferedPattern, is("/url/.+.*"));

        assertTrue("/url/foo".matches(transferedPattern));
        assertTrue("/url/foo/bar".matches(transferedPattern));
        assertFalse("/url/".matches(transferedPattern));
	}


	@Test
	public void whenUrlFilenameWildcard_thenUrlFilenameWildcard() {
		//Act
		MappingRule mappingRule = new MappingRule(ID, METRIC_ID, "/url/{pattern}.json", METHOD);

		//Assert
		String transferedPattern = mappingRule.getPattern().pattern();
		assertThat(transferedPattern, is("/url/.+\\.json.*"));

		assertTrue("/url/some-file.json".matches(transferedPattern));
		assertTrue("/url/some-file.json?filterby=name".matches(transferedPattern));
	}

	@Test
	public void whenPathParamWildcard_thenPathParamWildcard() {
		//Act
		MappingRule mappingRule = new MappingRule(ID, METRIC_ID, "/url/resource?id={pattern}&name={secondPattern}", METHOD);

		//Assert
		String transferedPattern = mappingRule.getPattern().pattern();
		assertThat(transferedPattern, is("/url/resource\\?id\\=.+&name\\=.+.*"));

		assertTrue("/url/resource?id=123&name=sbb".matches(transferedPattern));
	}

}