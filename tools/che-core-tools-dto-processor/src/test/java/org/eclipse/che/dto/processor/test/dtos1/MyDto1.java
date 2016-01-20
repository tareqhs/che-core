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
package org.eclipse.che.dto.processor.test.dtos1;

import java.util.List;
import java.util.Map;

import org.eclipse.che.dto.shared.DTO;

@DTO
public interface MyDto1 {

    public String getTheString();

    public void setTheString(String n);

    public short getThePrimShort();

    public void setThePrimShort(short n);

    public MyDto1 withThePrimShort(short n);

    public Map<String, Integer> getTheMap();

    public void setTheMap(Map<String, Integer> x);

    public List<Map<String, Integer>> getTheListOfMap();

    public void setTheListOfMap(List<Map<String, Integer>> nnnn);

    public List<Integer> getTheListOfInteger();

    public void setTheListOfInteger(List<Integer> nnnn);

    public MyEnum getTheEnum();

    public void setTheEnum(MyEnum v);

    public Object getTheAny();

    public void setTheAny(Object v);

}
