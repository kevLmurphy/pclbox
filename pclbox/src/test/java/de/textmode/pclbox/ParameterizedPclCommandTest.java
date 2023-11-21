package de.textmode.pclbox;

/*
 * Copyright 2017 Michael Knigge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * JUnit-Tests for {@link ParameterizedPclCommand}.
 */
public final class ParameterizedPclCommandTest extends TestCase {

    private static final ParameterizedPclCommand SIMPLEX_AT_1 = new ParameterizedPclCommand(1, '&', 'l', "0", 'S');
    private static final ParameterizedPclCommand SIMPLEX_AT_2 = new ParameterizedPclCommand(2, '&', 'l', "0", 'S');
    private static final ParameterizedPclCommand DUPLEX_AT_1 = new ParameterizedPclCommand(1, '&', 'l', "1", 'S');
    private static final ParameterizedPclCommand DUPLEX_AT_2 = new ParameterizedPclCommand(2, '&', 'l', "1", 'S');

    private static final ParameterizedPclCommand END_RASTER_GRAPHICS_1 =
            new ParameterizedPclCommand(0, '*', 'r', "0", 'B');

    private static final ParameterizedPclCommand END_RASTER_GRAPHICS_2 =
            new ParameterizedPclCommand(0, '*', 'r', "0", 'C');

    private static final ParameterizedPclCommand DISABLE_UNDERLINE =
            new ParameterizedPclCommand(0, '&', 'd', "0", '@');

    private static final ParameterizedPclCommand UEL_AT_0 = new ParameterizedPclCommand(0, '%', 0x00, "-12345", 'X');

    private static final ParameterizedPclCommand TRANSPARENT_DATA_0 =
            new ParameterizedPclCommand(0, '&', 'p', "0", 'X', new byte[0]);

    private static final ParameterizedPclCommand TRANSPARENT_DATA_1 =
            new ParameterizedPclCommand(0, '&', 'p', "1", 'X', new byte[] { 'A' });

    private static final ParameterizedPclCommand TRANSPARENT_DATA_2 =
            new ParameterizedPclCommand(0, '&', 'p', "2", 'X', new byte[] { 'A', 'B' });

    private static final ParameterizedPclCommand RASTER_DATA_BY_PLANE =
            new ParameterizedPclCommand(0, '*', 'b', "2", 'V', new byte[] { 0x00, 0x01 });

    private static final ParameterizedPclCommand FONT_HEADER =
            new ParameterizedPclCommand(0, ')', 's', "4", 'W', new byte[] { 0x11, 0x22, 0x33, 0x44 });

    /**
     * Checks the method getDataSection.
     */
    public void testGetDataSecion() {
        assertNull(SIMPLEX_AT_1.getDataSection());
        assertNull(UEL_AT_0.getDataSection());

        assertTrue(Arrays.equals(new byte[0], TRANSPARENT_DATA_0.getDataSection()));
        assertTrue(Arrays.equals(new byte[] { 'A' }, TRANSPARENT_DATA_1.getDataSection()));
        assertTrue(Arrays.equals(new byte[] { 'A', 'B' }, TRANSPARENT_DATA_2.getDataSection()));
        assertTrue(Arrays.equals(new byte[] { 0x00, 0x01 }, RASTER_DATA_BY_PLANE.getDataSection()));
        assertTrue(Arrays.equals(new byte[] { 0x11, 0x22, 0x33, 0x44 }, FONT_HEADER.getDataSection()));
    }

    /**
     * Checks the method hashCode.
     */
    public void testHashCode() {
        assertEquals(40, SIMPLEX_AT_1.hashCode());
        assertEquals(43, SIMPLEX_AT_2.hashCode());
        assertEquals(41, DUPLEX_AT_1.hashCode());
        assertEquals(42, DUPLEX_AT_2.hashCode());
        assertEquals(63, TRANSPARENT_DATA_0.hashCode());
        assertEquals(95, TRANSPARENT_DATA_1.hashCode());
        assertEquals(3038, TRANSPARENT_DATA_2.hashCode());
    }

    /**
     * Checks the method equals.
     */
    public void testEquals() {
        assertFalse(SIMPLEX_AT_1.equals("DUMMY")); // equals with object of other class

        assertFalse(SIMPLEX_AT_1.equals(SIMPLEX_AT_2)); // same command, different offset
        assertFalse(SIMPLEX_AT_1.equals(DUPLEX_AT_1)); // different command, same offset
        assertTrue(SIMPLEX_AT_1.equals(new ParameterizedPclCommand(1, '&', 'l', "0", 'S')));

        assertFalse(TRANSPARENT_DATA_0.equals(TRANSPARENT_DATA_1));
        assertFalse(TRANSPARENT_DATA_1.equals(TRANSPARENT_DATA_2));
        assertTrue(TRANSPARENT_DATA_1.equals(new ParameterizedPclCommand(0, '&', 'p', "1", 'X', new byte[] { 'A' })));

        assertFalse(SIMPLEX_AT_1.equals(new ParameterizedPclCommand(1, '*', 'l', "0", 'S')));
        assertFalse(SIMPLEX_AT_1.equals(new ParameterizedPclCommand(1, '&', 'm', "0", 'S')));
        assertFalse(SIMPLEX_AT_1.equals(new ParameterizedPclCommand(1, '&', 'l', "0", 'T')));
    }

    /**
     * Checks the method getTextualDescription.
     */
    public void testGetTextualDescription() {
        assertEquals("Transparent Print Data", TRANSPARENT_DATA_0.getTextualDescription());
        assertEquals("Simplex/Duplex Print", DUPLEX_AT_1.getTextualDescription());

        assertEquals(
                "Unknown PCL-Command *tZ",
                new ParameterizedPclCommand(1, '*', 't', "0", 'Z').getTextualDescription());
    }

    /**
     * Checks the method toString.
     */
    public void testToString() {
        assertEquals("<esc>&l0S@1", SIMPLEX_AT_1.toString());
        assertEquals("<esc>&l0S@2", SIMPLEX_AT_2.toString());
        assertEquals("<esc>&l1S@1", DUPLEX_AT_1.toString());
        assertEquals("<esc>&l1S@2", DUPLEX_AT_2.toString());
        assertEquals("<esc>%-12345X@0", UEL_AT_0.toString());
        assertEquals("<esc>&p0X@0", TRANSPARENT_DATA_0.toString());
        assertEquals("<esc>&p1X@0", TRANSPARENT_DATA_1.toString());
        assertEquals("<esc>&p2X@0", TRANSPARENT_DATA_2.toString());
    }

    /**
     * Checks the method toCommandString.
     */
    public void testToCommandString() {
        assertEquals("&lS", SIMPLEX_AT_1.toCommandString());
        assertEquals("&lS", DUPLEX_AT_1.toCommandString());
        assertEquals("%X", UEL_AT_0.toCommandString());
        assertEquals("&pX", TRANSPARENT_DATA_0.toCommandString());
    }

    /**
     * Checks the method toDisplayString.
     */
    public void testToDisplayString() {
        assertEquals("&l0S", SIMPLEX_AT_1.toDisplayString());
        assertEquals("&l1S", DUPLEX_AT_1.toDisplayString());
        assertEquals("%-12345X", UEL_AT_0.toDisplayString());
        assertEquals("&p0X", TRANSPARENT_DATA_0.toDisplayString());
        assertEquals("&p1X", TRANSPARENT_DATA_1.toDisplayString());
        assertEquals("&p2X", TRANSPARENT_DATA_2.toDisplayString());

        assertEquals("*rB", END_RASTER_GRAPHICS_1.toDisplayString());
        assertEquals("*rC", END_RASTER_GRAPHICS_2.toDisplayString());
        assertEquals("&d@", DISABLE_UNDERLINE.toDisplayString());
    }

    /**
     * Checks the method toByteArray and writeTo.
     */
    public void testToBinary() throws Exception {
        final ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        SIMPLEX_AT_1.writeTo(baos1);
        assertTrue(Arrays.equals(SIMPLEX_AT_1.toByteArray(), baos1.toByteArray()));

        final ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        DUPLEX_AT_1.writeTo(baos2);
        assertTrue(Arrays.equals(DUPLEX_AT_1.toByteArray(), baos2.toByteArray()));

        final ByteArrayOutputStream baos3 = new ByteArrayOutputStream();
        UEL_AT_0.writeTo(baos3);
        assertTrue(Arrays.equals(UEL_AT_0.toByteArray(), baos3.toByteArray()));

        final ByteArrayOutputStream baos4 = new ByteArrayOutputStream();
        TRANSPARENT_DATA_0.writeTo(baos4);
        assertTrue(Arrays.equals(TRANSPARENT_DATA_0.toByteArray(), baos4.toByteArray()));

        final ByteArrayOutputStream baos5 = new ByteArrayOutputStream();
        TRANSPARENT_DATA_1.writeTo(baos5);
        assertTrue(Arrays.equals(TRANSPARENT_DATA_1.toByteArray(), baos5.toByteArray()));

        final ByteArrayOutputStream baos6 = new ByteArrayOutputStream();
        TRANSPARENT_DATA_2.writeTo(baos6);
        assertTrue(Arrays.equals(TRANSPARENT_DATA_2.toByteArray(), baos6.toByteArray()));
    }

    /**
     * Checks that the properitary command "Data Container" (by Oce) is handled as expected.
     */
    public void testProperitaryCommandDataContainer() throws Exception {
        final Pcl5Command pc = new ParameterizedPclCommand(123, '&', 'p', "<ABC>", 'A');

        assertEquals("&pA", pc.toCommandString());
        assertEquals("ImageStream Data Container", pc.getTextualDescription());
        assertEquals("<esc>&p<ABC>A@123", pc.toString());
        assertEquals("&p<ABC>A", pc.toDisplayString());

        final byte[] pclExpected = new byte[] { 27, '&', 'p', '<', 'A', 'B', 'C', '>', 'A'};
        final byte[] pclActual = pc.toByteArray();
        assertTrue(Arrays.equals(pclExpected, pclActual));

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pc.writeTo(baos);
        assertTrue(Arrays.equals(pclExpected, baos.toByteArray()));

    }
}
