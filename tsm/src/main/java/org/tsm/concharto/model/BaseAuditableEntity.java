/*******************************************************************************
 * Copyright 2009 Time Space Map, LLC
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.tsm.concharto.model;

import java.util.Date;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class BaseAuditableEntity extends BaseEntity implements Auditable {
    private Date lastModified;
    private Date created;
    private Long version;

    /* (non-Javadoc)
     * @see org.tsm.concharto.model.Auditable#getCreated()
     */
    public Date getCreated() {
        return created;
    }

    /* (non-Javadoc)
     * @see org.tsm.concharto.model.Auditable#setCreated(java.util.Date)
     */
    public void setCreated(Date created) {
        this.created = created;
    }

    /* (non-Javadoc)
     * @see org.tsm.concharto.model.Auditable#getLastModified()
     */
    public Date getLastModified() {
        return lastModified;
    }

    /* (non-Javadoc)
     * @see org.tsm.concharto.model.Auditable#setLastModified(java.util.Date)
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
    
}
