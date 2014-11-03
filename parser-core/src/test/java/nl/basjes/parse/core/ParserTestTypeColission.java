/*
 * Apache HTTPD logparsing made easy
 * Copyright (C) 2013 Niels Basjes
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.basjes.parse.core;

import ch.qos.logback.classic.Level;
import nl.basjes.parse.core.exceptions.DisectionFailure;
import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ParserTestTypeColission {

    public static void setLoggingLevel(Level level) {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(level);
    }

    public static class TestDisector extends Disector {
        private String inputType;
        private String outputType;
        private String outputName;
        private String salt; // Each value that comes in is appended with this "salt"

        public TestDisector(String inputType, String outputType, String outputName, String salt) {
            this.inputType = inputType;
            this.outputType = outputType;
            this.outputName = outputName;
            this.salt = salt;
        }

        public void init(String inputtype, String outputtype, String outputname, String saltt) {
            this.inputType = inputtype;
            this.outputType = outputtype;
            this.outputName = outputname;
            this.salt = saltt;
        }
        protected void initializeNewInstance(Disector newInstance) {
            ((TestDisector)newInstance).init(inputType, outputType, outputName, salt);
        }


        @Override
        public void disect(Parsable<?> parsable, String inputname)
            throws DisectionFailure {
            final ParsedField field = parsable.getParsableField(inputType, inputname);
            parsable.addDisection(inputname, outputType, outputName, field.getValue()+salt);
        }

        @Override
        public String getInputType() {
            return inputType;
        }

        @Override
        public List<String> getPossibleOutput() {
            List<String> result = new ArrayList<>();
            result.add(outputType + ":" + outputName);
            return result;
        }

        @Override
        public EnumSet<Casts> prepareForDisect(String inputname, String outputname) {
            return Casts.STRING_ONLY;
        }

        @Override
        public void prepareForRun() {
        }
    }

    public static class TestDisectorOne extends TestDisector {
        public TestDisectorOne() {
            super("INPUTTYPE", "SOMETYPE", "output", "+1");
        }
    }

    public static class TestDisectorTwo extends TestDisector {
        public TestDisectorTwo() {
            super("INPUTTYPE", "OTHERTYPE", "output", "+2");
        }
    }

    public static class TestDisectorSubOne extends TestDisector {
        public TestDisectorSubOne() {
            super("SOMETYPE", "SOMESUBTYPE", "output", "+S1");
        }
    }

    public static class TestDisectorSubTwo extends TestDisector {
        public TestDisectorSubTwo() {
            super("OTHERTYPE", "OTHERSUBTYPE", "output", "+S2");
        }
    }

    public static class TestDisectorSubSubOne extends TestDisector {
        public TestDisectorSubSubOne() {
            super("SOMESUBTYPE", "SOMESUBSUBTYPE", "output", "+SS1");
        }
    }

    public static class TestDisectorSubSubTwo extends TestDisector {
        public TestDisectorSubSubTwo() {
            super("OTHERSUBTYPE", "OTHERSUBSUBTYPE", "output", "+SS2");
        }
    }

    public static class TestParser<RECORD> extends Parser<RECORD> {
        public TestParser(final Class<RECORD> clazz) {
            super(clazz);
            addDisector(new TestDisectorOne());
            addDisector(new TestDisectorTwo());
            addDisector(new TestDisectorSubOne());
            addDisector(new TestDisectorSubTwo());
            addDisector(new TestDisectorSubSubOne());
            addDisector(new TestDisectorSubSubTwo());
            setRootType("INPUTTYPE");
        }
    }

    public static class TestRecord {
        private String output1 = "xxx";

        @Field("SOMETYPE:output")
        public void setValue1(String name, String value) {
            output1 = name + ":" + value;
        }

        private String output2 = "xxx";

        @Field("OTHERTYPE:output")
        public void setValue2(String name, String value) {
            output2 = name + ":" + value;
        }

        private String output3 = "xxx";

        @Field("SOMESUBSUBTYPE:output.output.output")
        public void setValue3(String name, String value) {
            output3 = name + ":" + value;
        }

        private String output4 = "xxx";

        @Field("OTHERSUBSUBTYPE:output.output.output")
        public void setValue4(String name, String value) {
            output4 = name + ":" + value;
        }

    }

    @Test
    public void testParseString() throws Exception {
        setLoggingLevel(Level.ALL);
        Parser<TestRecord> parser = new TestParser<>(TestRecord.class);

        TestRecord output = new TestRecord();
        parser.parse(output, "Something");
        assertEquals("SOMETYPE:output:Something+1", output.output1);
        assertEquals("OTHERTYPE:output:Something+2", output.output2);
        assertEquals("SOMESUBSUBTYPE:output.output.output:Something+1+S1+SS1", output.output3);
        assertEquals("OTHERSUBSUBTYPE:output.output.output:Something+2+S2+SS2", output.output4);
    }

}
