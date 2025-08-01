///////////////////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code and other text files for adherence to a set of rules.
// Copyright (C) 2001-2025 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
///////////////////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle;

import static com.google.common.truth.Truth.assertWithMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serial;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.AbstractAutomaticBean.OutputStreamOptions;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AutomaticBean;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import com.puppycrawl.tools.checkstyle.api.Violation;
import com.puppycrawl.tools.checkstyle.internal.utils.CloseAndFlushTestByteArrayOutputStream;
import com.puppycrawl.tools.checkstyle.internal.utils.TestUtil;

public class SarifLoggerTest extends AbstractModuleTestSupport {

    /**
     * Output stream to hold the test results. The IntelliJ IDEA issues the AutoCloseableResource
     * warning here, so it needs to be suppressed. The {@code ByteArrayOutputStream} does not hold
     * any resources that need to be released.
     */
    private final CloseAndFlushTestByteArrayOutputStream outStream =
        new CloseAndFlushTestByteArrayOutputStream();

    @Override
    protected String getPackageLocation() {
        return "com/puppycrawl/tools/checkstyle/sariflogger";
    }

    /**
     * Executes the common logging steps for SARIF logging tests.
     * This method mimic of natural execution at is done by Checker.
     *
     * <p>
     * It starts file auditing, adds an error, and completes the audit process.
     * </p>
     *
     * @param instance  the current instance of {@code SarifLoggerTest}, used for context
     * @param logger    the {@code SarifLogger} instance to log errors
     * @param fileName  the name of the file being audited
     * @param violation the {@code Violation} object containing error details
     */
    public final void executeLogger(
        SarifLoggerTest instance, SarifLogger logger, String fileName, Violation violation) {
        final AuditEvent event = new AuditEvent(this, "Test.java", violation);
        logger.fileStarted(event);
        logger.addError(event);
        logger.fileFinished(event);
        logger.auditFinished(null);
    }

    @Test
    public void testEscape() {
        final String[][] encodings = {
            {"\"", "\\\""},
            {"\\", "\\\\"},
            {"\b", "\\b"},
            {"\f", "\\f"},
            {"\n", "\\n"},
            {"\r", "\\r"},
            {"\t", "\\t"},
            {"/", "\\/"},
            {"\u0010", "\\u0010"},
            {"\u001E", "\\u001E"},
            {"\u001F", "\\u001F"},
            {" ", " "},
            {"bar1234", "bar1234"},
        };
        for (String[] encoding : encodings) {
            final String encoded = SarifLogger.escape(encoding[0]);
            assertWithMessage("\"" + encoding[0] + "\"")
                .that(encoded)
                .isEqualTo(encoding[1]);
        }
    }

    @Test
    public void testSingleError() throws Exception {
        final String inputFile = "InputSarifLoggerSingleError.java";
        final String expectedReportFile = "ExpectedSarifLoggerSingleError.sarif";
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.CLOSE);

        verifyWithInlineConfigParserAndLogger(
                getPath(inputFile), getPath(expectedReportFile), logger, outStream);
    }

    @Test
    public void testAddErrorAtColumn1() throws Exception {
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.CLOSE);
        final String inputFile = "InputSarifLoggerSingleErrorColumn1.java";
        final String expectedOutput = "ExpectedSarifLoggerSingleErrorColumn1.sarif";
        verifyWithInlineConfigParserAndLogger(getPath(inputFile),
                getPath(expectedOutput), logger, outStream);
    }

    @Test
    public void testAddErrorAtColumn0() throws Exception {
        final String inputFile = "InputSarifLoggerErrorColumn0.java";
        final String expectedReportFile = "ExpectedSarifLoggerSingleErrorColumn0.sarif";
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.CLOSE);

        verifyWithInlineConfigParserAndLogger(
                getPath(inputFile), getPath(expectedReportFile), logger, outStream);
    }

    @Test
    public void testAddErrorWithWarningLevel() throws Exception {
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.CLOSE);
        final String inputFile = "InputSarifLoggerSingleWarning.java";
        final String expectedOutput = "ExpectedSarifLoggerSingleWarning.sarif";
        verifyWithInlineConfigParserAndLogger(getPath(inputFile),
                getPath(expectedOutput), logger, outStream);
    }

    @Test
    public void testAddErrors() throws IOException {
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.CLOSE);
        logger.auditStarted(null);
        final Violation violation =
                new Violation(1, 1,
                        "messages.properties", "ruleId", null, SeverityLevel.INFO, null,
                        getClass(), "found an error");
        final AuditEvent ev = new AuditEvent(this, "Test.java", violation);
        final Violation violation2 =
                new Violation(1, 1,
                        "messages.properties", "ruleId2", null, SeverityLevel.IGNORE, null,
                        getClass(), "found another error");
        final AuditEvent ev2 = new AuditEvent(this, "Test.java", violation2);
        logger.fileStarted(ev);
        logger.addError(ev);
        logger.fileFinished(ev);
        logger.fileStarted(ev2);
        logger.addError(ev2);
        logger.fileFinished(ev2);
        logger.auditFinished(null);
        verifyContent(getPath("ExpectedSarifLoggerDoubleError.sarif"), outStream);
    }

    @Test
    public void testAddException() throws IOException {
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.CLOSE);
        logger.auditStarted(null);
        final Violation message =
                new Violation(1, 1,
                        "messages.properties", "null", null, null,
                        getClass(), "found an error");
        final AuditEvent ev = new AuditEvent(this, null, message);
        logger.fileStarted(ev);
        logger.addException(ev, new TestException("msg", new RuntimeException("msg")));
        logger.fileFinished(ev);
        logger.auditFinished(null);
        verifyContent(getPath("ExpectedSarifLoggerSingleException.sarif"), outStream);
    }

    @Test
    public void testAddExceptions() throws IOException {
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.CLOSE);
        logger.auditStarted(null);
        final Violation violation =
                new Violation(1, 1,
                        "messages.properties", "null", null, null,
                        getClass(), "found an error");
        final AuditEvent ev = new AuditEvent(this, null, violation);
        final Violation violation2 =
                new Violation(1, 1,
                        "messages.properties", "null", null, null,
                        getClass(), "found an error");
        final AuditEvent ev2 = new AuditEvent(this, "Test.java", violation2);
        logger.fileStarted(ev);
        logger.addException(ev, new TestException("msg", new RuntimeException("msg")));
        logger.fileFinished(ev);
        logger.fileStarted(ev2);
        logger.addException(ev2, new TestException("msg2", new RuntimeException("msg2")));
        logger.fileFinished(ev);
        logger.auditFinished(null);
        verifyContent(getPath("ExpectedSarifLoggerDoubleException.sarif"), outStream);
    }

    @Test
    public void testLineOnly() throws IOException {
        final SarifLogger logger = new SarifLogger(outStream,
            OutputStreamOptions.CLOSE);
        logger.auditStarted(null);
        final Violation violation =
            new Violation(1, 0,
                "messages.properties", "ruleId", null, null,
                getClass(), "found an error");
        final AuditEvent ev = new AuditEvent(this, "Test.java", violation);
        logger.fileStarted(ev);
        logger.addError(ev);
        logger.fileFinished(ev);
        logger.auditFinished(null);
        verifyContent(getPath("ExpectedSarifLoggerLineOnly.sarif"), outStream);
    }

    @Test
    public void testEmpty() throws IOException {
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.CLOSE);
        logger.auditStarted(null);
        final Violation violation =
                new Violation(1, 1,
                        "messages.properties", "null", null, null,
                        getClass(), "found an error");
        final AuditEvent ev = new AuditEvent(this, null, violation);
        logger.fileStarted(ev);
        logger.fileFinished(ev);
        logger.auditFinished(null);
        verifyContent(getPath("ExpectedSarifLoggerEmpty.sarif"), outStream);
    }

    @Test
    public void testAddErrorWithSpaceInPath() throws IOException {
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.CLOSE);
        logger.auditStarted(null);
        final Violation violation =
                new Violation(1, 1,
                        "messages.properties", "ruleId", null, SeverityLevel.ERROR, null,
                        getClass(), "found an error");
        final AuditEvent ev = new AuditEvent(this, "/home/someuser/Code/Test 2.java", violation);
        logger.fileStarted(ev);
        logger.addError(ev);
        logger.fileFinished(ev);
        logger.auditFinished(null);
        verifyContent(getPath("ExpectedSarifLoggerSpaceInPath.sarif"), outStream);
    }

    @Test
    public void testAddErrorWithAbsoluteLinuxPath() throws IOException {
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.CLOSE);
        logger.auditStarted(null);
        final Violation violation =
                new Violation(1, 1,
                        "messages.properties", "ruleId", null, SeverityLevel.ERROR, null,
                        getClass(), "found an error");
        final AuditEvent ev = new AuditEvent(this, "/home/someuser/Code/Test.java", violation);
        logger.fileStarted(ev);
        logger.addError(ev);
        logger.fileFinished(ev);
        logger.auditFinished(null);
        verifyContent(getPath("ExpectedSarifLoggerAbsoluteLinuxPath.sarif"), outStream);
    }

    @Test
    public void testAddErrorWithRelativeLinuxPath() throws IOException {
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.CLOSE);
        logger.auditStarted(null);
        final Violation violation =
                new Violation(1, 1,
                        "messages.properties", "ruleId", null, SeverityLevel.ERROR, null,
                        getClass(), "found an error");
        final AuditEvent ev = new AuditEvent(this, "./Test.java", violation);
        logger.fileStarted(ev);
        logger.addError(ev);
        logger.fileFinished(ev);
        logger.auditFinished(null);
        verifyContent(getPath("ExpectedSarifLoggerRelativeLinuxPath.sarif"), outStream);
    }

    @Test
    public void testAddErrorWithAbsoluteWindowsPath() throws IOException {
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.CLOSE);
        logger.auditStarted(null);
        final Violation violation =
                new Violation(1, 1,
                        "messages.properties", "ruleId", null, SeverityLevel.ERROR, null,
                        getClass(), "found an error");
        final AuditEvent ev =
                new AuditEvent(this, "C:\\Users\\SomeUser\\Code\\Test.java", violation);
        logger.fileStarted(ev);
        logger.addError(ev);
        logger.fileFinished(ev);
        logger.auditFinished(null);
        verifyContent(getPath("ExpectedSarifLoggerAbsoluteWindowsPath.sarif"), outStream);
    }

    @Test
    public void testAddErrorWithRelativeWindowsPath() throws IOException {
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.CLOSE);
        logger.auditStarted(null);
        final Violation violation =
                new Violation(1, 1,
                        "messages.properties", "ruleId", null, SeverityLevel.ERROR, null,
                        getClass(), "found an error");
        final AuditEvent ev = new AuditEvent(this, ".\\Test.java", violation);
        logger.fileStarted(ev);
        logger.addError(ev);
        logger.fileFinished(ev);
        logger.auditFinished(null);
        verifyContent(getPath("ExpectedSarifLoggerRelativeWindowsPath.sarif"), outStream);
    }

    /**
     * We keep this test for 100% coverage. Until #12873.
     */
    @Test
    public void testCtorWithTwoParametersCloseStreamOptions() throws IOException {
        final OutputStream infoStream = new ByteArrayOutputStream();
        final SarifLogger logger = new SarifLogger(infoStream,
                AutomaticBean.OutputStreamOptions.CLOSE);
        final boolean closeStream = TestUtil.getInternalState(logger, "closeStream");

        assertWithMessage("closeStream should be true")
                .that(closeStream)
                .isTrue();
    }

    /**
     * We keep this test for 100% coverage. Until #12873.
     */
    @Test
    public void testCtorWithTwoParametersNoneStreamOptions() throws IOException {
        final OutputStream infoStream = new ByteArrayOutputStream();
        final SarifLogger logger = new SarifLogger(infoStream,
                AutomaticBean.OutputStreamOptions.NONE);
        final boolean closeStream = TestUtil.getInternalState(logger, "closeStream");

        assertWithMessage("closeStream should be false")
                .that(closeStream)
                .isFalse();
    }

    @Test
    public void testNullOutputStreamOptions() {
        try {
            final SarifLogger logger = new SarifLogger(outStream, (OutputStreamOptions) null);
            // assert required to calm down eclipse's 'The allocated object is never used' violation
            assertWithMessage("Null instance")
                .that(logger)
                .isNotNull();
            assertWithMessage("Exception was expected").fail();
        }
        catch (IllegalArgumentException | IOException exception) {
            assertWithMessage("Invalid error message")
                .that(exception.getMessage())
                .isEqualTo("Parameter outputStreamOptions can not be null");
        }
    }

    @Test
    public void testCloseStream() throws IOException {
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.CLOSE);
        logger.auditStarted(null);
        logger.auditFinished(null);

        assertWithMessage("Invalid close count")
            .that(outStream.getCloseCount())
            .isEqualTo(1);

        verifyContent(getPath("ExpectedSarifLoggerEmpty.sarif"), outStream);
    }

    @Test
    public void testNoCloseStream() throws IOException {
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.NONE);
        logger.auditStarted(null);
        logger.auditFinished(null);

        assertWithMessage("Invalid close count")
            .that(outStream.getCloseCount())
            .isEqualTo(0);
        assertWithMessage("Invalid flush count")
            .that(outStream.getFlushCount())
            .isEqualTo(1);

        outStream.close();
        verifyContent(getPath("ExpectedSarifLoggerEmpty.sarif"), outStream);
    }

    @Test
    public void testFinishLocalSetup() throws IOException {
        final SarifLogger logger = new SarifLogger(outStream,
                OutputStreamOptions.CLOSE);
        logger.finishLocalSetup();
        logger.auditStarted(null);
        logger.auditFinished(null);
        assertWithMessage("instance should not be null")
            .that(logger)
            .isNotNull();
    }

    @Test
    public void testReadResourceWithInvalidName() {
        try {
            SarifLogger.readResource("random");
            assertWithMessage("Exception expected").fail();
        }
        catch (IOException exception) {
            assertWithMessage("Exception message must match")
                .that(exception.getMessage())
                .isEqualTo("Cannot find the resource random");
        }
    }

    private static void verifyContent(
            String expectedOutputFile,
            ByteArrayOutputStream actualOutputStream) throws IOException {
        final String expectedContent = readFile(expectedOutputFile);
        final String actualContent =
                toLfLineEnding(actualOutputStream.toString(StandardCharsets.UTF_8));
        assertWithMessage("sarif content should match")
            .that(actualContent)
            .isEqualTo(expectedContent);
    }

    private static final class TestException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;

        private TestException(String msg, Throwable cause) {
            super(msg, cause);
        }

        @Override
        public void printStackTrace(PrintWriter printWriter) {
            printWriter.print("stackTrace\nexample");
        }
    }
}
