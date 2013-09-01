package com.auditbucket.demo 
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._
import assertions._

class AccountOperationsSimulation extends Simulation {

    val rnd = new scala.util.Random
    val chooseRandomNodes = exec((session) => {
        session.setAttribute("params", rnd.nextInt(100000))
      })

      val chooseRandomStatus = exec((session) => {
          session.setAttribute("status", rnd.nextInt(100000))
        })



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


	val scn = scenario("Account Operations Senario")
	.during(5 minutes){
	     exec(chooseRandomNodes)
	     .exec(chooseRandomStatus)
	     .exec(http("Create Account")
        					.post("http://localhost:9090/account/save")
        					.headers(headers_4)
        					.body("""{
                                       "accountNumber":"%s",
                                       "iban":"DDDEEEFF44444",
                                       "status":"STARTED"
                                     }""".format("${params}")).asJSON
                            .check(status.is(200))
        			)
        		.pause(2 seconds)
        		.exec(http("Update Account")
                         					.post("http://localhost:9090/account/update")
                         					.headers(headers_4)
                         					.body("""{
                                                        "accountNumber":"%s",
                                                        "iban":"DDDEEEFF44444",
                                                        "status":"%s"
                                                      }""".format("${params}","${status}")).asJSON
                                             .check(status.is(200))
                         			)
                         		.pause(2 seconds)
	}


	setUp(scn.users(20).ramp(100).protocolConfig(httpConf))
}