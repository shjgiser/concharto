/*******************************************************************************
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License Version 
 * 1.1 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 * 
 * The Original Code is Concharto.
 * 
 * The Initial Developer of the Original Code is
 * Time Space Map, LLC
 * Portions created by the Initial Developer are Copyright (C) 2007 - 2009
 * the Initial Developer. All Rights Reserved.
 * 
 * Contributor(s):
 * Time Space Map, LLC
 * 
 * ***** END LICENSE BLOCK *****
 ******************************************************************************/
package com.tech4d.tsm.model.wiki;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;

import com.tech4d.tsm.model.BaseAuditableEntity;

@Entity
public class WikiText extends BaseAuditableEntity {
    public static final int SZ_TITLE = 512;
	private String text;
	String title;

	@Lob 
	@Column( columnDefinition = "mediumtext")
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

    @Column(length=SZ_TITLE)
    public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
}