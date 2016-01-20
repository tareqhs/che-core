/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.dto.processor.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Collections;

import org.eclipse.che.dto.processor.test.dtos1.MyDto1;
import org.eclipse.che.dto.processor.test.dtos1.MyDto2;
import org.eclipse.che.dto.processor.test.dtos1.MyEnum;
import org.eclipse.che.dto.processor.test.dtos2.MyDto3;
import org.eclipse.che.dto.server.DtoFactory;
import org.junit.Test;

public class DtoGeneratorTest {

    @Test
    public void testAllDtosGenerated() {
        assertNotNull(DtoFactory.getInstance().createDto(MyDto1.class));
        assertNotNull(DtoFactory.getInstance().createDto(MyDto2.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotAllowedDtosNotGenerated() {
        DtoFactory.getInstance().createDto(MyDto3.class);
    }

    @Test
    public void testInitialValues() {
        MyDto1 dto1 = DtoFactory.getInstance().createDto(MyDto1.class);
        assertNull(dto1.getTheAny());
        assertNull(dto1.getTheEnum());
        assertEquals(Collections.emptyList(), dto1.getTheListOfInteger());
        assertEquals(Collections.emptyList(), dto1.getTheListOfMap());
        assertEquals(Collections.emptyMap(), dto1.getTheMap());
        assertEquals(0, dto1.getThePrimShort());
        assertNull(dto1.getTheString());
    }

    @Test
    public void testSetters() {
        MyDto1 dto1 = DtoFactory.getInstance().createDto(MyDto1.class);
        dto1.setTheString("abc");
        assertEquals("abc", dto1.getTheString());
        dto1.setTheEnum(MyEnum.BB);
        assertEquals(MyEnum.BB, dto1.getTheEnum());
    }

    @Test
    public void testWithSetters() {
        MyDto1 dto1 = DtoFactory.getInstance().createDto(MyDto1.class);
        MyDto1 dto2 = dto1.withThePrimShort((short) 10);
        assertSame(dto1, dto2);
        assertEquals(10, dto1.getThePrimShort());
    }

    @Test
    public void testEquals() {
        MyDto1 dto1 = DtoFactory.getInstance().createDto(MyDto1.class);
    }
}
