package org.springframework.batch.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StepScopeAnnotatedListenerIntegrationTests {

	@Autowired
	JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	public void test() {
		JobExecution jobExecution = jobLauncherTestUtils.launchStep("step-under-test");

		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	public static class StatefulItemReader implements ItemReader<String> {

		private List<String> list;

		@BeforeStep
		public void initializeState(StepExecution stepExecution) {
			this.list = new ArrayList<String>();
		}

		@AfterStep
		public ExitStatus exploitState(StepExecution stepExecution) {
			System.out.println("******************************");
			System.out.println(" READING RESULTS : " + list.size());

			return stepExecution.getExitStatus();
		}

		@Override
		public String read() throws Exception {
			this.list.add("some stateful reading information");
			if (list.size() < 10) {
				return "value " + list.size();
			}
			return null;
		}
	}

	@Configuration
	@EnableBatchProcessing
	public static class TestConfig {
		@Autowired
		private JobBuilderFactory jobBuilder;
		@Autowired
		private StepBuilderFactory stepBuilder;

		@Bean
		JobLauncherTestUtils jobLauncherTestUtils() {
			return new JobLauncherTestUtils();
		}

		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder embeddedDatabaseBuilder = new EmbeddedDatabaseBuilder();
			return embeddedDatabaseBuilder.addScript("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql")
					.addScript("classpath:org/springframework/batch/core/schema-hsqldb.sql")
					.setType(EmbeddedDatabaseType.HSQL)
					.build();
		}

		@Bean
		public Job jobUnderTest() {
			return jobBuilder.get("job-under-test")
					.start(stepUnderTest())
					.build();
		}

		@Bean
		public Step stepUnderTest() {
			return stepBuilder.get("step-under-test")
					.<String, String>chunk(1)
					.reader(reader())
					.processor(processor())
					.writer(writer())
					.build();
		}

		@Bean
		@StepScope
		public StatefulItemReader reader() {
			return new StatefulItemReader();
		}

		@Bean
		public ItemProcessor<String, String> processor() {
			return new ItemProcessor<String, String>() {

				@Override
				public String process(String item) throws Exception {
					return item;
				}
			};
		}

		@Bean
		public ItemWriter<String> writer() {
			return new ItemWriter<String>() {

				@Override
				public void write(List<? extends String> items)
						throws Exception {
				}
			};
		}
	}
}
