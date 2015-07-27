/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.core.model.machine;

import org.eclipse.che.api.core.model.machine.Recipe;

/**
 * @author gazarenkov
 */
public interface MachineConfig {

    /**
     * Display name
     * @return
     */
    String getName();

    /**
     * From where to create this MAchine
     * (Recipe/Snapshot)
     * @return
     */
    Source getSource();

    //String getRecipeUrl();

    /**
     * Is workspace bound to machine or not
     */
    boolean isDev();

    /**
     * Id of workspace this machine belongs to
     */
    String getWorkspaceId();

    /**
     * Channel of websocket where machine logs should be put
     */
    String getOutputChannel();

    /**
     * Machine type (i.e. "docker")
     */
    String getType();

}
