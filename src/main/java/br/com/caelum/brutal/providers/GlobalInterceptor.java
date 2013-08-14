package br.com.caelum.brutal.providers;

import static java.util.Arrays.asList;

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.ocpsoft.prettytime.PrettyTime;

import br.com.caelum.brutal.ads.BrutalAds;
import br.com.caelum.brutal.auth.BannedUserException;
import br.com.caelum.brutal.controllers.AuthController;
import br.com.caelum.brutal.factory.MessageFactory;
import br.com.caelum.brutal.infra.MenuInfo;
import br.com.caelum.brutal.infra.NotFoundException;
import br.com.caelum.brutal.infra.SideBarInfo;
import br.com.caelum.brutal.util.BrutalDateFormat;
import br.com.caelum.brutauth.interceptors.CustomBrutauthRuleInterceptor;
import br.com.caelum.brutauth.interceptors.SimpleBrutauthRuleInterceptor;
import br.com.caelum.vraptor.InterceptionException;
import br.com.caelum.vraptor.Intercepts;
import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.core.InterceptorStack;
import br.com.caelum.vraptor.core.Localization;
import br.com.caelum.vraptor.environment.Environment;
import br.com.caelum.vraptor.interceptor.Interceptor;
import br.com.caelum.vraptor.ioc.Component;
import br.com.caelum.vraptor.plugin.hibernate4.extra.ParameterLoaderInterceptor;
import br.com.caelum.vraptor.resource.ResourceMethod;

@Component
@Intercepts(before={ParameterLoaderInterceptor.class, CustomBrutauthRuleInterceptor.class, SimpleBrutauthRuleInterceptor.class})
public class GlobalInterceptor implements Interceptor {
	
	private static final String SLASH_AT_END = "/$";
	private final Environment env;
	private final Result result;
	private final HttpServletRequest req;
	private final Localization localization;
	private static final Logger LOG = Logger.getLogger(GlobalInterceptor.class);
	private final MenuInfo menuInfo;
	private BrutalDateFormat brutalDateFormat;
	private final MessageFactory messageFactory;
	private final BrutalAds ads;
	private final SideBarInfo sideBarInfo;

	public GlobalInterceptor(Environment env, Result result, 
			HttpServletRequest req, Localization localization,  
			ServletContext servletContext, MenuInfo menuInfo, SideBarInfo sideBarInfo,
			BrutalDateFormat brutalDateFormat, MessageFactory messageFactory,
			BrutalAds ads) {
		this.env = env;
		this.result = result;
		this.req = req;
		this.localization = localization;
		this.menuInfo = menuInfo;
		this.sideBarInfo = sideBarInfo;
		this.brutalDateFormat = brutalDateFormat;
		this.messageFactory = messageFactory;
		this.ads = ads;
	}

	public void intercept(InterceptorStack stack, ResourceMethod method,
			Object resourceInstance) throws InterceptionException {
		menuInfo.include();
		sideBarInfo.include();
		
		result.include("env", env);
		result.include("prettyTimeFormatter", new PrettyTime(localization.getLocale()));
		result.include("literalFormatter", brutalDateFormat.getInstance("date.joda.pattern"));
		result.include("currentUrl", getCurrentUrl());
		result.include("contextPath", req.getContextPath());
		result.include("deployTimestamp", deployTimestamp());
		result.include("shouldShowAds", ads.shouldShowAds());
		result.on(NotFoundException.class).notFound();
		result.on(BannedUserException.class)
				.include("errors", asList(messageFactory.build("error", "user.errors.banned")))
				.redirectTo(AuthController.class).loginForm("");
		
		logHeaders();
		
		stack.next(method, resourceInstance);
	}

	private String deployTimestamp() {
		return System.getProperty("deploy.timestamp", "");
	}

	private String getCurrentUrl() {
		String host = req.getHeader("Host");
		String url;
		if (host == null) {
			url = req.getRequestURL().toString();
		} else {
			url = "http://" + host + req.getRequestURI(); 
		}
		if(url.endsWith("/")) url = url.split(SLASH_AT_END)[0];
		LOG.debug("setting url: " + url);
		return url;
	}

	private void logHeaders() {
		Enumeration<String> headerNames = req.getHeaderNames();
		LOG.debug("headers received from request");
		while (headerNames.hasMoreElements()) {
			String key = headerNames.nextElement();
			String value = req.getHeader(key);
			LOG.debug(key);
			LOG.debug(value);
		}
	}

	public boolean accepts(ResourceMethod method) {
		return true;
	}

}
