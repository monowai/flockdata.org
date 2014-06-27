package com.auditbucket.company.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.stormpath.spring.security.provider.StormpathUserDetails;

@Controller
public class HelloController {
	private static final Logger logger = LoggerFactory
			.getLogger(HelloController.class);

	@RequestMapping(value = "/hello/{message}", method = RequestMethod.GET)
	public ResponseEntity<String> sayHello(
			@PathVariable(value = "message") String message,
			@AuthenticationPrincipal StormpathUserDetails customUser) {
		logger.info("Authentication principal - " + customUser);
		return new ResponseEntity<String>("Hello " + message, HttpStatus.OK);
	}

}
