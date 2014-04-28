package com.auditbucket.client;

import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 4:34 PM
 */
public class TrackMapper extends MetaInputBean implements DelimitedMappable {
    public TrackMapper(ImportParams importParams) {
        setDocumentType(importParams.getDocumentType());
        setFortress(importParams.getFortress());
        setFortressUser(importParams.getFortressUser());

    }

    @Override
    public Importer.importer getImporter() {
        return Importer.importer.CSV;
    }

    @Override
    public AbRestClient.type getABType() {
        return AbRestClient.type.TRACK;
    }

    @Override
    public String setData(String[] headerRow, String[] line, StaticDataResolver staticDataResolver) throws JsonProcessingException {
        int col = 0;
        for (String column : headerRow) {
            if ( column.startsWith("$") && !(line[col]==null || line[col].equals(""))){
                String callerRef = getCallerRef();
                if (callerRef== null)
                    callerRef=line[col];
                else
                    callerRef = callerRef+"."+line[col];

                setCallerRef(callerRef);
            }else if ( column.startsWith("@")){
                boolean mustExist = column.startsWith("@!");

                String val = line[col];
                if ( val!=null && !val.equals(""))
                    setTag( new TagInputBean(line[col]).setMustExist(mustExist));
            } else if ( column.startsWith("*")){
                setName(line[col]);
            }
            col ++;
        }
        return AbRestClient.convertToJson(headerRow, line);
    }

    @Override
    public boolean hasHeader() {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public static DelimitedMappable newInstance(ImportParams importParams) {
        return new TrackMapper(importParams);
    }

    @Override
    public char getDelimiter() {
        return ',';  //To change body of implemented methods use File | Settings | File Templates.
    }


}
