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
package org.tsm.concharto.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Transactional;
import org.tsm.concharto.audit.AuditChangeUtil;
import org.tsm.concharto.audit.AuditFieldChangeFormatter;
import org.tsm.concharto.dao.AuditEntryDao;
import org.tsm.concharto.dao.EventDao;
import org.tsm.concharto.model.Auditable;
import org.tsm.concharto.model.audit.AuditEntry;


@Transactional
public class RevertEventServiceHib implements RevertEventService {
    private SessionFactory sessionFactory;
    private AuditEntryDao auditEntryDao;
    private EventDao eventDao;
    private Set<AuditFieldChangeFormatter> auditFormatters = new HashSet<AuditFieldChangeFormatter>();

    public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setAuditEntryDao(AuditEntryDao auditEntryDao) {
		this.auditEntryDao = auditEntryDao;
	}

	public void setEventDao(EventDao eventDao) {
		this.eventDao = eventDao;
	}


    /**
     * A list of class names of classes that implement AuditFieldChangeFormatter.  We use
     * these to create the audit field change record
     * @param auditFieldChangeFormatters AuditFieldChangeFormatter 
     * @throws InstantiationException e
     * @throws IllegalAccessException e
     * @throws ClassNotFoundException e
     */
    public void setAuditFieldChangeFormatters(Set<String> auditFieldChangeFormatters) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        for (String className : auditFieldChangeFormatters) {
            auditFormatters.add((AuditFieldChangeFormatter) Class.forName(className).newInstance());
        }
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

	public Auditable revertToRevision(Class<?> clazz, Integer revision, Long eventId) {
		Auditable auditable = eventDao.findById(clazz, eventId);		
		Long count = auditEntryDao.getAuditEntriesCount(auditable);
		//only get the audit records necessary for reverting
		List<AuditEntry> auditEntries = 
			auditEntryDao.getAuditEntries(auditable, 0, (int) (count-revision-1));
		return revertToRevision(auditable, auditEntries);
	}

	private Auditable revertToRevision(Auditable auditable, List<AuditEntry> auditEntries) {

		//1. get the diff (change list)
		Map<Integer, String> changes = AuditChangeUtil.getChanges(auditEntries);
		
		if (changes.size() != 0 ) {
			//2. get the original event.  TODO assumes all auditEntries are for the same entity.  
			//could be a problem in the future.
			AuditFieldChangeFormatter auditFieldChangeFormatter = getFormatter(auditable);
			auditable = auditFieldChangeFormatter.revertEntity(auditable, changes);
			
			//3. save the object
			eventDao.saveAuditable(auditable);
			//flush, otherwise the audit record won't get written to the database.  
			// TODO Perhaps we can find it somewhere in the session instead of retrieving it in step 4?
			this.sessionFactory.getCurrentSession().flush();
			
			//4. TODO KLUDGE - I don't know how to modify the AuditInterceptor to realize that 
			// this particular save is actually a "revert", but we want the audit record to reflect that....
			// so let's change the last audit record from "ACTION_UPDATE" to "ACTION_REVERT"
			List<AuditEntry> updated = auditEntryDao.getAuditEntries(auditable, 0, 1);
			AuditEntry mostRecent = updated.get(0);
			mostRecent.setAction(AuditEntry.ACTION_REVERT);
			auditEntryDao.update(mostRecent);
		}
	
		return auditable;
	}

	private AuditFieldChangeFormatter getFormatter(Auditable auditable) {
        for (AuditFieldChangeFormatter formatter : auditFormatters) {
            if (formatter.supports(auditable.getClass())) {
                return formatter;
            }
        }
        return null;
    }

}
