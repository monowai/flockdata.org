package com.auditbucket.demo

import java.util.concurrent.atomic.AtomicLong
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._
import assertions._

class AccountOperationsSimulation extends Simulation {

  val rnd = new scala.util.Random
  val counter = new ABCounter()

  val chooseRandomNodes = exec((session) => {
    session.setAttribute("params", counter.incr())
  })

  val chooseRandomStatus = exec((session) => {
    session.setAttribute("status", rnd.nextInt(1000))
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


  val scn = scenario("Account Operations Scenario")
    .during(2 minutes) {
    exec(chooseRandomNodes)
      .exec(chooseRandomStatus)
      .exec(http("Create Account")
      .post("http://localhost:9090/account/save")
      .headers(headers_4)
      .body( """{
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
      .body( """{
                                                        "accountNumber":"%s",
                                                        "iban":"DDDEEEFF44444",
                                                        "status":"%s"
                                                      }""".format("${params}", "${status}")).asJSON
      .check(status.is(200))
    )
    //.pause(2 seconds)
  }


  setUp(scn.users(20).ramp(100).protocolConfig(httpConf))

  class ABCounter(value: AtomicLong) {
    def this() = this(new AtomicLong(System.currentTimeMillis()))

    /**
     * Increment the counter by one.
     */
    def incr(): Long = value.incrementAndGet

    /**
     * Increment the counter by `n`, atomically.
     */
    def incr(n: Int): Long = value.addAndGet(n)

    /**
     * Get the current value.
     */
    def apply(): Long = value.get()

    /**
     * Set a new value, wiping the old one.
     */
    def update(n: Long) = value.set(n)

    /**
     * Clear the counter back to zero.
     */
    def reset() = update(0L)

    override def toString() = "Counter(%d)".format(value.get())
  }

}




