package com.auditbucket
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._
import assertions._

class Senario1 extends Simulation {
	
	val httpConf = httpConfig
			.baseURL("http://localhost:8080")
			.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
			.acceptEncodingHeader("gzip,deflate,sdch")
			.acceptLanguageHeader("fr-FR,fr;q=0.8,en-US;q=0.6,en;q=0.4")
			.userAgentHeader("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36")

			
	val headers_1 = Map(
			"Cache-Control" -> """max-age=0""",
			"Content-Type" -> """application/x-www-form-urlencoded""",
			"Origin" -> """http://localhost:9966"""
	)

	val scn = scenario("Stress Test AB")
		.exec(http("Create Account")
					.post("/account")
					.headers(headers_1)
			)
		.pause(749 milliseconds)
		.exec(http("Update Account")
        					.put("/account")
        					.headers(headers_1)
        			)

	setUp(scn.users(200).protocolConfig(httpConf))
}