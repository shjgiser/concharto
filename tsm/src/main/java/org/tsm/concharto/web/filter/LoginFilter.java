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
package org.tsm.concharto.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.util.WebUtils;
import org.tsm.concharto.auth.AuthConstants;
import org.tsm.concharto.auth.AuthHelper;
import org.tsm.concharto.auth.ThreadLocalUserContext;
import org.tsm.concharto.auth.UserContext;
import org.tsm.concharto.dao.UserDao;
import org.tsm.concharto.model.user.User;
import org.tsm.concharto.web.util.SessionHelper;


/**
 * Authorization filter.  We don't want to rely on the servlet container to do this because we
 * will have to get fancy in the future.
 * TODO make a unit test for this!
 * @author frank
 *
 */
public class LoginFilter implements Filter{
	public static final String SESSION_HOSTPREFIX = "hostprefix";
	private static final String REDIRECT_NOTAUTHORIZED = "/notauthorized.htm";
    private static final String REDIRECT_LOGIN = "/login.htm";
    //TODO search requires authentication only during the private pilot
    private static final Log log = LogFactory.getLog(LoginFilter.class);
    private UserDao userDao;
    private SessionHelper sessionHelper;

    public void init(FilterConfig filterConfig) throws ServletException {
//    	this.filterConfig = filterConfig; 
    	ServletContext ctx = filterConfig.getServletContext();
        WebApplicationContext webAppContext = WebApplicationContextUtils.getWebApplicationContext(ctx);
        userDao = (UserDao) webAppContext.getBean("userDao");
        sessionHelper = (SessionHelper) webAppContext.getBean("sessionHelper");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        //Has the user signed in?
    	if (handleRememberMeCookie(httpRequest)) {
    		//redirect to get rid of the jsessionid crap on the URL string
    		//TODO - Ugh! this is the only way I know how to get rid of jsessionid.  
    		//NOTE this doesn't work when you have a server port other than 80 (e.g. test server).  Not sure why.
    		//There is probably another way
    		//NOTE: this doesn't work for URLs that will be redirected (e.g. our "link to here" urls
    		//that contain get strings.  This all incoming links with remember me cookies must not redirect.  UGH!
    		if (StringUtils.isEmpty(httpRequest.getQueryString())) {
        		httpResponse.sendRedirect(httpResponse.encodeRedirectURL(httpRequest.getRequestURL().toString()));
    		} else {
    			String url = httpRequest.getRequestURL().toString();
    			url += "?" + httpRequest.getQueryString();
    			httpResponse.sendRedirect(url);
    		}
    		if (AuthHelper.isUserInSession(httpRequest)) {
        		log.info("user " + 
        				httpRequest.getSession().getAttribute(AuthConstants.SESSION_AUTH_USERNAME) + 
        				" signed in via cookie");
    		}
    	} 
    	
        //Does this page require authentication
        if (requiresAuthentication(httpRequest)) {
            if (!isAuthenticated(httpRequest)) {
                httpResponse.sendRedirect(httpResponse.encodeRedirectURL(httpRequest.getContextPath() + 
                		REDIRECT_LOGIN));
            }  
            //ok, is the user authorized for this URL
            else if (!isAuthorized(httpRequest)) {
                httpResponse.sendRedirect(httpResponse.encodeRedirectURL(httpRequest.getContextPath() + 
                		REDIRECT_NOTAUTHORIZED));
            }
        }
        //setup the user context for those who can't get user and role data from
        //the session (e.g. audit interceptor)
        HttpSession session = httpRequest.getSession();
        UserContext userContext = new UserContext();
        userContext.setUsername((String) session.getAttribute(AuthConstants.SESSION_AUTH_USERNAME));
        userContext.setRoles((String) session.getAttribute(AuthConstants.SESSION_AUTH_ROLES));
        ThreadLocalUserContext.setUserContext(userContext);
        chain.doFilter(request, response);
    }

    /**
     * This implements is a crude 2 level URL based authorization scheme.
     * For each of the role patterns ensure that the appropriate role exists.
     * Examples:
     * <pre>
     *  /admin/canDelete/deleteUser.htm requres 'admin' and 'canDelete'
     *  /admin/findUsers.htm requires 'admin'
     *  /admin/canBlock/blockUser.htm requires 'admin' and 'canBlock'
     * </pre>
     * @param httpRequest request
     * @return true if the user is authorized for this resource
     */
    private boolean isAuthorized(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession();
        String[] parts = getUrlParts(httpRequest);
        if (!requiresAuthorization(parts)) {
            return true;
        }
        if (null != session.getAttribute(AuthConstants.SESSION_AUTH_ROLES)) {
            String roles = (String) session.getAttribute(AuthConstants.SESSION_AUTH_ROLES);
            if (parts != null) {
                if (parts.length == 2) {
                    //one level only e.g. URL was admin/findUsers.htm
                    if (StringUtils.contains(roles, parts[0])) {
                        //e.g. "admin canDelete canMove" contains "admin"
                        return true;
                    }
                } else if (parts.length >= 3) {
                    //two levels e.g. URL was admin/canDelete/deleteUsers.htm
                    if (StringUtils.contains(roles, parts[0]) && StringUtils.contains(roles, parts[1])) {
                        //e.g. "admin canDelete canMove" contains "admin" and "canDelete"
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Returns the first part of the URL path: "/member/settings.htm?id=1234" returns "member"
     * @param httpRequest
     * @return first part of the URL path
     */
    private String[] getUrlParts(HttpServletRequest httpRequest) {
        String uri = httpRequest.getRequestURI();
        //should like like '/admin/canDelete' or '/admin/findUsers'
        String path = StringUtils.substringBetween(uri, httpRequest.getContextPath(),".htm");
        String[] parts = StringUtils.split(path, '/');
        return parts;
    }

    private boolean isAuthenticated(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession();
        if (log.isDebugEnabled()) {
            log.debug("auth login filter");
        }
        
        //TODO reliance on session may be a problem for scalability
        if (!AuthHelper.isUserInSession(httpRequest)) {
            //save the target so we can get there after authentication
            StringBuffer redirect = new StringBuffer(httpRequest.getRequestURI());

        	//no cookie set, we need to go to the login screen
            if (!StringUtils.isEmpty(httpRequest.getQueryString())) {
                redirect.append('?').append(httpRequest.getQueryString());
            }
            session.setAttribute(AuthConstants.SESSION_AUTH_TARGET_URI, redirect.toString() );
            return false;
        } else {
            return true;
        }
    }

    private boolean requiresAuthentication(HttpServletRequest httpRequest) {

        for (String pattern : AuthConstants.PATTERN_REQUIRES_AUTHENTICATION) {
            if (StringUtils.contains(httpRequest.getRequestURI(), pattern)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean requiresAuthorization(String[] parts) {
    	//iterate through n-1
    	for (int part_i=0; part_i<parts.length-1; part_i++) {
            for (String pattern : AuthConstants.PATTERN_REQUIRES_AUTHORIZATION) {
                if (StringUtils.contains(parts[part_i], pattern)) {
                    return true;
                }
            }
    	}
        return false;
    }

    public void destroy() {
        // TODO Auto-generated method stub
        
    }

    /**
     * Returns true if the user is not in the session but the remember me cookie is correct
     * Also puts the user in the session.  
     * @param request
     * @return 
     */
	private boolean handleRememberMeCookie(HttpServletRequest request) {
		if (!AuthHelper.isUserInSession(request)) {
			Cookie cookie = WebUtils.getCookie(request, AuthHelper.COOKIE_REMEMBER_ME);
			if (null != cookie) {
				//if the cookie is there, get the user for that cookie
				User user = userDao.getUserFromRememberMeKey(cookie.getValue());
				if (user != null) {
					//ok, they are authenticated
					sessionHelper.setUserInSession(request, user);
					return true;
				}
			}
			//The user is anonymous
			sessionHelper.setUserInSession(request, null);
		}
		return false;
	}

}
