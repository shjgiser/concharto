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
package org.tsm.concharto.web.util;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.tsm.concharto.model.user.User;


public class AuthFormValidatorHelper {
    private static final int MIN_PASSWORD_SIZE = 4;

	public static void validate(AuthForm authForm, Errors errors) {
    	validateEmail(authForm, errors);
    	validateUsernamePasswordFields(authForm, errors);
        ValidationHelper.rejectIfTooLong(errors, "username", User.SZ_USERNAME, "tooLong.signupForm");
    }
    
    public static void validateEmail(AuthForm authForm, Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "email", "empty.authForm.email");
    }

    public static void validateUsernamePasswordFields(AuthForm authForm, Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "username", "empty.authForm.username");
        validatePasswordFields(authForm, errors);
    }
    
    public static void validatePasswordFields(AuthForm authForm, Errors errors) {
    	ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "empty.authForm.password");
    	ValidationUtils.rejectIfEmptyOrWhitespace(errors, "passwordConfirm", "empty.authForm.passwordConfirm");
    	if (!StringUtils.isEmpty(authForm.getPassword()) && !StringUtils.isEmpty(authForm.getPasswordConfirm())) {
    		if (!authForm.getPassword().equals(authForm.getPasswordConfirm())) {
    			errors.rejectValue("password", "notSame.authForm.password");
    		}
        	if (authForm.getPassword().length() < MIN_PASSWORD_SIZE) {
        		errors.rejectValue("password", "tooSmall.authForm.password");
        	}
            ValidationHelper.rejectIfTooLong(errors, "password", User.SZ_PASSWORD, "tooLong.signupForm");
    	}
    }
    
}
