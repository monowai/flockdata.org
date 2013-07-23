/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.test.functional;

import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.helper.CompressionHelper;
import com.auditbucket.helper.CompressionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * User: Mike Holdsworth
 * Since: 18/07/13
 */
public class TestJson {
    @Test
    public void compressLotsOfBytes() throws Exception {
        String json = getBigJsonText(99);
        System.out.println("Pretty JSON          - " + json.getBytes().length);
        AuditLogInputBean log = new AuditLogInputBean("", "", null, json);
        System.out.println("JSON Node (unpretty) - " + log.getWhat().getBytes().length);

        CompressionResult result = CompressionHelper.compress(json);
        System.out.println("Compress Pretty      - " + result.length());
        result = CompressionHelper.compress(log.getWhat());
        System.out.println("Compressed JSON      - " + result.length());

        assertEquals(CompressionResult.Method.GZIP, result.getMethod());

        json = getBigJsonText(99);
        String uncompressed = CompressionHelper.decompress(result);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode compareTo = mapper.readTree(json);
        JsonNode other = mapper.readTree(uncompressed);
        assertTrue(compareTo.equals(other));
    }

    @Test
    public void simpleTextRemainsUncompressed() throws Exception {
        String json = "{\"colname\": \"tinytext.......................\"}";
        System.out.println("Before Comppression" + json.getBytes().length);

        CompressionResult result = CompressionHelper.compress(json);
        assertEquals(CompressionResult.Method.NONE, result.getMethod());
        System.out.println("Compressed " + result.length());

        String uncompressed = CompressionHelper.decompress(result);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode compareTo = mapper.readTree(json);
        JsonNode other = mapper.readTree(uncompressed);
        assertTrue(compareTo.equals(other));

    }

    public static String getBigJsonText(int i) {
        return "{\n" +
                "   \"trainprofiles\": [\n" +
                "        {\n" +
                "           \"name\":\"TP-" + i + "\",\n" +
                "           \"startDate\":\"20120918\",\n" +
                "           \"endDate\":\"20120924\",\n" +
                "           \"type\":\"M\",\n" +
                "           \"class\":\"UF\",\n" +
                "           \"locations\": [\n" +
                "                {\n" +
                "                   \"name\":\"PNTH\",\n" +
                "                   \"workstationDetails\": {\n" +
                "                       \"stationType\":\"ORIG\",\n" +
                "                       \"dayEnroute\": 416\n" +
                "                    }\n" +
                "                },\n" +
                "                {\n" +
                "                   \"name\":\"WHRRA\",\n" +
                "                   \"workstationDetails\": {\n" +
                "                       \"stationType\":\"DEST\",\n" +
                "                       \"dayEnroute\": 0\n" +
                "                    }\n" +
                "                }\n" +
                "            ],\n" +
                "           \"schedules\": [\n" +
                "                {\n" +
                "                   \"dayOfWeek\": {\n" +
                "                       \"day\":\"MON\"\n" +
                "                    },\n" +
                "                   \"locationTimings\": [\n" +
                "                        {\n" +
                "                           \"name\":\"PNTH\",\n" +
                "                           \"departTime\": 345\n" +
                "                        },\n" +
                "                        {\n" +
                "                           \"name\":\"WHRRA\",\n" +
                "                           \"arriveTime\": 234\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                {\n" +
                "                   \"dayOfWeek\": {\n" +
                "                       \"day\":\"TUE\"\n" +
                "                    },\n" +
                "                   \"locationTimings\": [\n" +
                "                        {\n" +
                "                           \"name\":\"PNTH\",\n" +
                "                           \"departTime\": 345\n" +
                "                        },\n" +
                "                        {\n" +
                "                           \"name\":\"WHRRA\",\n" +
                "                           \"arriveTime\": 234\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                {\n" +
                "                   \"dayOfWeek\": {\n" +
                "                       \"day\":\"WED\"\n" +
                "                    },\n" +
                "                   \"locationTimings\": [\n" +
                "                        {\n" +
                "                           \"name\":\"PNTH\",\n" +
                "                           \"departTime\": 345\n" +
                "                        },\n" +
                "                        {\n" +
                "                           \"name\":\"WHRRA\",\n" +
                "                           \"arriveTime\": 234\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                {\n" +
                "                   \"dayOfWeek\": {\n" +
                "                       \"day\":\"THU\"\n" +
                "                    },\n" +
                "                   \"locationTimings\": [\n" +
                "                        {\n" +
                "                           \"name\":\"PNTH\",\n" +
                "                           \"departTime\": 345\n" +
                "                        },\n" +
                "                        {\n" +
                "                           \"name\":\"WHRRA\",\n" +
                "                           \"arriveTime\": 234\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                {\n" +
                "                   \"dayOfWeek\": {\n" +
                "                       \"day\":\"FRI\"\n" +
                "                    },\n" +
                "                   \"locationTimings\": [\n" +
                "                        {\n" +
                "                           \"name\":\"PNTH\",\n" +
                "                           \"departTime\": 345\n" +
                "                        },\n" +
                "                        {\n" +
                "                           \"name\":\"WHRRA\",\n" +
                "                           \"arriveTime\": 234\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                {\n" +
                "                   \"dayOfWeek\": {\n" +
                "                       \"day\":\"SAT\"\n" +
                "                    },\n" +
                "                   \"locationTimings\": [\n" +
                "                        {\n" +
                "                           \"name\":\"PNTH\",\n" +
                "                           \"departTime\": 345\n" +
                "                        },\n" +
                "                        {\n" +
                "                           \"name\":\"WHRRA\",\n" +
                "                           \"arriveTime\": 234\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                {\n" +
                "                   \"dayOfWeek\": {\n" +
                "                       \"day\":\"SUN\"\n" +
                "                    },\n" +
                "                   \"locationTimings\": [\n" +
                "                        {\n" +
                "                           \"name\":\"PNTH\",\n" +
                "                           \"departTime\": 345\n" +
                "                        },\n" +
                "                        {\n" +
                "                           \"name\":\"WHRRA\",\n" +
                "                           \"arriveTime\": 234\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "           \"name\":\"B56\",\n" +
                "           \"startDate\":\"20080708\",\n" +
                "           \"endDate\":\"99999999\",\n" +
                "           \"type\":\"M\",\n" +
                "           \"class\":\"EX\",\n" +
                "           \"locations\": [\n" +
                "                {\n" +
                "                   \"name\":\"PNTH\",\n" +
                "                   \"workstationDetails\": {\n" +
                "                       \"stationType\":\"ORIG\",\n" +
                "                       \"dayEnroute\": 508\n" +
                "                    }\n" +
                "                },\n" +
                "                {\n" +
                "                   \"name\":\"TAUM\",\n" +
                "                   \"workstationDetails\": {\n" +
                "                       \"stationType\":\"DEST\",\n" +
                "                       \"dayEnroute\": 0\n" +
                "                    }\n" +
                "                }\n" +
                "            ],\n" +
                "           \"schedules\": [\n" +
                "                {\n" +
                "                   \"dayOfWeek\": {\n" +
                "                       \"day\":\"TUE\"\n" +
                "                    },\n" +
                "                   \"locationTimings\": [\n" +
                "                        {\n" +
                "                           \"name\":\"PNTH\",\n" +
                "                           \"departTime\": 345\n" +
                "                        },\n" +
                "                        {\n" +
                "                           \"name\":\"TAUM\",\n" +
                "                           \"arriveTime\": 234\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                {\n" +
                "                   \"dayOfWeek\": {\n" +
                "                       \"day\":\"WED\"\n" +
                "                    },\n" +
                "                   \"locationTimings\": [\n" +
                "                        {\n" +
                "                           \"name\":\"PNTH\",\n" +
                "                           \"departTime\": 345\n" +
                "                        },\n" +
                "                        {\n" +
                "                           \"name\":\"TAUM\",\n" +
                "                           \"arriveTime\": 234\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                {\n" +
                "                   \"dayOfWeek\": {\n" +
                "                       \"day\":\"THU\"\n" +
                "                    },\n" +
                "                   \"locationTimings\": [\n" +
                "                        {\n" +
                "                           \"name\":\"PNTH\",\n" +
                "                           \"departTime\": 345\n" +
                "                        },\n" +
                "                        {\n" +
                "                           \"name\":\"TAUM\",\n" +
                "                           \"arriveTime\": 234\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    ],\n" +
                "   \"pagination\": {\n" +
                "       \"total\": 9952,\n" +
                "       \"page\": 1,\n" +
                "       \"size\": 2,\n" +
                "       \"order\":\"train\"\n" +
                "    }\n" +
                "}\n" +
                " \n" +
                " ";
    }
}
