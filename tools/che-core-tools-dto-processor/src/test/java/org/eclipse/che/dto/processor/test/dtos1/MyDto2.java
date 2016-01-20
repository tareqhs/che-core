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

import org.eclipse.che.dto.processor.test.dtos3.ExternalDto;
import org.eclipse.che.dto.shared.DTO;

@DTO
public interface MyDto2 extends MyDto1 {

    public MyDto1 getTheDto();

    public void setTheDto(MyDto1 v);

    public List<ExternalDto> getTheListOfExternalDto();

    public void setTheListOfExternalDto(List<ExternalDto> v);

    public Boolean getGg();

    public void setGg(Boolean v);

}
