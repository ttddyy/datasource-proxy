package net.ttddyy.dsproxy.listener.logging;

import net.ttddyy.dsproxy.TestUtils;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Tadaya Tsuyukubo
 */
public class LoggingListenerLogLevelTest {

    private DataSource jdbcDataSource;

    @BeforeEach
    public void setup() throws Exception {
        // real datasource
        jdbcDataSource = TestUtils.getDataSourceWithData();
    }

    @AfterEach
    public void teardown() throws Exception {
        TestUtils.shutdown(jdbcDataSource);
    }

    @ParameterizedTest
    @EnumSource(value = CommonsLogLevel.class)
    public void testLogLevel(CommonsLogLevel logLevel) throws Exception {

        InMemoryCommonsLog log = new InMemoryCommonsLog();
        log.setEnabledLogLevel(logLevel);

        CommonsQueryLoggingListener loggingListener = new CommonsQueryLoggingListener();
        loggingListener.setLog(log);
        loggingListener.setLogLevel(logLevel);

        ProxyDataSource proxyDataSource = ProxyDataSourceBuilder.create(jdbcDataSource).listener(loggingListener).build();


        Connection connection = proxyDataSource.getConnection();
        Statement statement = connection.createStatement();
        statement.executeQuery("select * from emp where id=1");
        statement.executeQuery("select * from emp where id=2");

        verifyMessage(log, logLevel, "select * from emp where id=1", "select * from emp where id=2");

    }

    private void verifyMessage(InMemoryCommonsLog log, CommonsLogLevel logLevel, String... queries) {

        for (CommonsLogLevel commonsLogLevel : CommonsLogLevel.values()) {
            List<String> messageList = log.getMessages(commonsLogLevel);
            if (commonsLogLevel == logLevel) {
                assertThat(messageList).hasSize(queries.length);
                for (int i = 0; i < queries.length; i++) {
                    final String query = queries[i];
                    assertThat(messageList.get(i)).contains(query);
                }
            } else {
                assertThat(messageList).isEmpty();
            }
        }

    }

}
