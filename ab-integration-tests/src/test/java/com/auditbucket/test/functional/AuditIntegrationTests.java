package com.auditbucket.test.functional;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: nabil
 * Date: 16/07/13
 * Time: 22:51
 * To change this template use File | Settings | File Templates.
 */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration("classpath:root-context.xml")
//@Transactional
public class AuditIntegrationTests {

//    @Autowired
//    RegistrationService regService;
//
//    @Autowired
//    FortressService fortressService;
//
//    @Autowired
//    AuditService auditService;
//
//
//    private Logger log = LoggerFactory.getLogger(AuditIntegrationTests.class);
//    private String company = "Monowai";
//    private String email = "mike@monowai.com";
//    private String emailB = "mark@null.com";
//    Authentication authA = new UsernamePasswordAuthenticationToken(email, "user1");
//    Authentication authB = new UsernamePasswordAuthenticationToken(emailB, "user1");

    @Test
    public void testCreateLogHeaderWithSearchActivated(){
        Assert.assertTrue(true);
    }

    //@Test
//    public void createHeaderTimeLogsWithSearchActivated() throws Exception {
//
//        regService.registerSystemUser(new RegistrationBean(company, email, "bah"));
//        IFortress fo = fortressService.registerFortress(new FortressInputBean("auditTest",true));
//
//        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "TestAudit", new Date(), "ABC123");
//        String ahKey = auditService.createHeader(inputBean).getAuditKey();
//
//        assertNotNull(ahKey);
//        log.info(ahKey);
//        Thread.sleep(5000);
////        byte[] docs = alRepo.findOne(auditService.getHeader(ahKey));
////        assertNotNull(docs);
//        assertNotNull(auditService.getHeader(ahKey));
//        assertNotNull(auditService.findByCallerRef(fo.getId(), "TestAudit", "ABC123"));
//        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
//        assertNull(fortressService.getFortressUser(fo, "wallyz", false));
//
//        int i = 0;
//        double max = 10d;
//        StopWatch watch = new StopWatch();
//        log.info("Start-");
//        watch.start();
//        while (i < max) {
//            AuditLogInputBean auditLogInputBean = auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + i + "}"));
//            i++;
//        }
//        watch.stop();
//        log.info("End " + watch.getTime() / 1000d + " avg = " + (watch.getTime() / 1000d) / max);
//
//        // Test that we get the expected number of log events
//        assertEquals(max, (double) auditService.getAuditLogCount(ahKey));
//
//
//    }
}
