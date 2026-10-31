package com.elias.service.agent.impl;

import com.elias.common.context.UserContext;
import com.elias.agent.worker.AgentResultConsumer;
import com.elias.dto.agent.run.request.AgentRunStartRequest;
import com.elias.dto.agent.run.response.AgentRunQueueResponse;
import com.elias.dto.interview.request.StartInterviewRunRequest;
import com.elias.entity.agent.AgentSession;
import com.elias.entity.auth.User;
import com.elias.entity.resume.Resume;
import com.elias.entity.resume.ResumeSection;
import com.elias.mapper.agent.AgentSessionMapper;
import com.elias.mapper.auth.UserMapper;
import com.elias.mapper.resume.ResumeMapper;
import com.elias.mapper.resume.ResumeSectionMapper;
import com.elias.service.agent.AgentRunService;
import com.elias.service.interview.InterviewService;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证 run 与 SQLite 任务队列在同一事务内提交或回滚。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgentRunQueueTransactionIntegrationTest {

    private static final Path TEST_DIRECTORY = createTestDirectory();
    private static final Path TEST_DATABASE = TEST_DIRECTORY.resolve("ai_resume.db");

    @Autowired
    private AgentRunService agentRunService;

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ResumeMapper resumeMapper;

    @Autowired
    private ResumeSectionMapper resumeSectionMapper;

    @Autowired
    private AgentSessionMapper agentSessionMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 集成测试不启动常驻消费循环，避免清理临时数据库时仍有后台线程轮询。 */
    @MockBean
    private AgentResultConsumer agentResultConsumer;

    @Autowired
    private HikariDataSource dataSource;

    @DynamicPropertySource
    static void registerTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEST_DATABASE.toAbsolutePath()
                + "?busy_timeout=10000&journal_mode=WAL&foreign_keys=on");
        registry.add("jwt.secret", () -> "test-only-jwt-secret-with-more-than-thirty-two-bytes");
        registry.add("internal.agent.service-token", () -> "test-only-internal-agent-token");
        registry.add("python.agent.base-url", () -> "http://127.0.0.1:65535");
        registry.add("storage.root", () -> TEST_DIRECTORY.resolve("storage").toString());
    }

    @BeforeEach
    void clearUserContext() {
        UserContext.clear();
    }

    @AfterAll
    void closeTestDatabase() throws IOException {
        dataSource.close();
        Files.deleteIfExists(TEST_DATABASE.resolveSibling(TEST_DATABASE.getFileName() + "-wal"));
        Files.deleteIfExists(TEST_DATABASE.resolveSibling(TEST_DATABASE.getFileName() + "-shm"));
        Files.deleteIfExists(TEST_DATABASE);
        Files.deleteIfExists(TEST_DIRECTORY.resolve("storage"));
        Files.deleteIfExists(TEST_DIRECTORY);
    }

    @Test
    void resumeRunsAreIdempotentParallelAndAtomicWithQueueWrites() {
        Fixture fixture = createFixture("JD_CUSTOMIZE", true);
        UserContext.setUserId(String.valueOf(fixture.userId()));
        try {
            AgentRunStartRequest firstRequest = resumeRequest(fixture, "resume-request-1");
            AgentRunQueueResponse first = agentRunService.startRun(fixture.sessionId(), firstRequest);
            AgentRunQueueResponse duplicate = agentRunService.startRun(fixture.sessionId(), firstRequest);

            assertEquals(first.getRunId(), duplicate.getRunId());
            assertEquals(1, countRuns(fixture.sessionId()));
            assertEquals(1, countTasks("agent.resume.run"));

            AgentRunQueueResponse second = agentRunService.startRun(
                    fixture.sessionId(), resumeRequest(fixture, "resume-request-2"));
            assertNotEquals(first.getRunId(), second.getRunId());
            assertEquals(2, countRuns(fixture.sessionId()));
            assertEquals(2, countTasks("agent.resume.run"));

            createQueueFailureTrigger("fail_resume_queue_test", "agent.resume.run");
            assertThrows(RuntimeException.class, () -> agentRunService.startRun(
                    fixture.sessionId(), resumeRequest(fixture, "resume-request-failing")));
            assertEquals(2, countRuns(fixture.sessionId()));
            assertEquals(2, countTasks("agent.resume.run"));
        } finally {
            dropQueueFailureTrigger("fail_resume_queue_test");
            UserContext.clear();
        }
    }

    @Test
    void interviewRunAndQueueWriteRollbackTogether() {
        Fixture fixture = createFixture("INTERVIEW", false);
        UserContext.setUserId(String.valueOf(fixture.userId()));
        try {
            StartInterviewRunRequest request = StartInterviewRunRequest.builder()
                    .sessionId(fixture.sessionId())
                    .build();
            interviewService.startRun(request);
            assertEquals(1, countRuns(fixture.sessionId()));
            assertEquals(1, countTasks("agent.interview.run"));

            createQueueFailureTrigger("fail_interview_queue_test", "agent.interview.run");
            assertThrows(RuntimeException.class, () -> interviewService.startRun(request));
            assertEquals(1, countRuns(fixture.sessionId()));
            assertEquals(1, countTasks("agent.interview.run"));
        } finally {
            dropQueueFailureTrigger("fail_interview_queue_test");
            UserContext.clear();
        }
    }

    private Fixture createFixture(String sceneCode, boolean moduleBound) {
        String unique = UUID.randomUUID().toString().replace("-", "");
        User user = User.builder()
                .username("outbox-" + unique)
                .email(unique + "@example.test")
                .password("test-only")
                .enabled(true)
                .build();
        userMapper.insert(user);

        Resume resume = Resume.builder()
                .userId(user.getId())
                .title("Outbox test")
                .template("classic")
                .build();
        resumeMapper.insert(resume);

        ResumeSection section = ResumeSection.builder()
                .resumeId(resume.getId())
                .sectionCode("OUTBOX_TEST")
                .sectionTitle("Outbox test section")
                .sectionType("CUSTOM")
                .schemaType("TEXT")
                .contentJson("{}")
                .visible(true)
                .sortOrder(1)
                .build();
        resumeSectionMapper.insert(section);

        AgentSession session = AgentSession.builder()
                .userId(user.getId())
                .resumeId(resume.getId())
                .sceneCode(sceneCode)
                .targetSectionId(moduleBound ? section.getId() : null)
                .sessionTitle("Outbox test")
                .status("ACTIVE")
                .build();
        agentSessionMapper.insert(session);
        return new Fixture(user.getId(), resume.getId(), section.getId(), session.getId());
    }

    private AgentRunStartRequest resumeRequest(Fixture fixture, String clientRequestId) {
        AgentRunStartRequest request = new AgentRunStartRequest();
        request.setSceneCode("JD_CUSTOMIZE");
        request.setResumeId(fixture.resumeId());
        request.setTargetSectionId(fixture.sectionId());
        request.setUserInput("Test the queue transaction.");
        request.setJobDescription("Test role");
        request.setClientRequestId(clientRequestId);
        return request;
    }

    private long countRuns(Long sessionId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_agent_run WHERE session_id = ?", Long.class, sessionId);
    }

    private long countTasks(String stream) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_task WHERE stream = ?", Long.class, stream);
    }

    private void createQueueFailureTrigger(String triggerName, String stream) {
        jdbcTemplate.execute("CREATE TRIGGER " + triggerName
                + " BEFORE INSERT ON agent_task WHEN NEW.stream = '" + stream
                + "' BEGIN SELECT RAISE(ABORT, 'forced test queue failure'); END");
    }

    private void dropQueueFailureTrigger(String triggerName) {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + triggerName);
    }

    private record Fixture(Long userId, Long resumeId, Long sectionId, Long sessionId) {}

    private static Path createTestDirectory() {
        try {
            return Files.createTempDirectory("ai-resume-outbox-test-");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
