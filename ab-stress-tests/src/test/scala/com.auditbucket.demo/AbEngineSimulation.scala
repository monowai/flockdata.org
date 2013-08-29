package com.auditbucket.demo 
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._
import assertions._

class AbEngineSimulation extends Simulation {

    val rnd = new scala.util.Random

	val httpConf = httpConfig
			.baseURL("http://localhost:7474")
			.acceptHeader("application/json, text/javascript, */*")
			.acceptEncodingHeader("gzip,deflate,sdch")
			.acceptLanguageHeader("en-US,en;q=0.8,fr;q=0.6")
			.authorizationHeader("Basic bWlrZToxMjM=")
			.userAgentHeader("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36")


	val headers_4 = Map(
			"Accept" -> """*/*""",
			"Cache-Control" -> """no-cache""",
			"Content-Type" -> """application/json""",
			"Origin" -> """chrome-extension://fdmmgilgnpjigdojojpjoooidkmcomcm"""
	)


	val scn = scenario("Ab Engine Audit Senario")
	.during(5 minutes){
	    exec(http("Create Header")
        					.post("http://localhost:9081/ab-engine/audit/header/new")
        					.headers(headers_4)
        					.body("""{
                                       "fortress":"SearchNA",
                                       "fortressUser": "mike",
                                       "documentType":"Training",
                                       "when":"2012-12-12",
                                       "tagValues": { "TagA": "tenzing", "TagB": "tagValB"}
                                     }""").asJSON
        	    )
        		.pause(2 seconds)
	}


	setUp(scn.users(20).ramp(100).protocolConfig(httpConf))
}