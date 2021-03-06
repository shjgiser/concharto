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
package org.tsm.concharto.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.tsm.concharto.dao.WikiTextDao;
import org.tsm.concharto.model.wiki.WikiText;
import org.tsm.concharto.util.ContextUtil;


public class IntegrationTestWikiTextDao {
    private static final String WIKI_PAGE = "lllllls [dfsdf sdf] yo";
    private static final String WIKI_PAGE_TITLE = "User:jon";
    private static final String[] TITLES = {"page0", "page1", "page2"};
	private static WikiTextDao wikiTextDao;
    private static EventTesterDao eventTesterDao;

    @BeforeClass
    public static void setupClass() {
        ApplicationContext appCtx = ContextUtil.getCtx();
        wikiTextDao = (WikiTextDao) appCtx.getBean("wikiTextDao");
        eventTesterDao = (EventTesterDao) appCtx.getBean("eventTesterDao");
    }
    
    @Before public void setUp() {
        eventTesterDao.deleteAll();
    }
    
    @Test public void saveGetDelete() {
    	WikiText wikiText = new WikiText();
    	wikiText.setText(WIKI_PAGE);
    	wikiText.setTitle(WIKI_PAGE_TITLE);
    	Long id = createWikiText(WIKI_PAGE_TITLE, WIKI_PAGE);
    	assertNotNull(id);
    	wikiText = wikiTextDao.get(id);
    	assertEquals(WIKI_PAGE, wikiText.getText());
    	
    	wikiText = wikiTextDao.find(WIKI_PAGE_TITLE);
    	assertEquals(WIKI_PAGE_TITLE, wikiText.getTitle());
    	assertEquals(true, wikiTextDao.exists(WIKI_PAGE_TITLE));
    	wikiTextDao.delete(id);
    	wikiText = wikiTextDao.get(id);
    	assertNull(wikiText);
    	assertEquals(false, wikiTextDao.exists(WIKI_PAGE_TITLE));   	
    }
    
    @Test public void existsArray() {
    	for (int i=0; i<TITLES.length; i++) {
        	createWikiText(TITLES[i],WIKI_PAGE);
    	}
    	createWikiText(WIKI_PAGE_TITLE,WIKI_PAGE);
    	
    	assertEquals(TITLES.length, wikiTextDao.exists(TITLES).entrySet().size());
    }
    
    private Long createWikiText(String title, String text) {
    	WikiText wikiText = new WikiText();
    	wikiText.setText(text);
    	wikiText.setTitle(title);
    	Long id = (Long) wikiTextDao.save(wikiText);
    	return id;
    }
}
