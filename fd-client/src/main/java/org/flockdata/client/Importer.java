/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.client;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.flockdata.data.ContentModel;
import org.flockdata.helper.FlockException;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.FileProcessor;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.FdIoInterface;
import org.flockdata.transform.json.ExtractProfileDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.flockdata.transform.model.PayloadTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StopWatch;

/**
 * General importer with support for Delimited and XML parsing.
 * <p>
 * Will send information to FlockData as either tags or track information over AMQP
 * <p>
 * For XML you should extend EntityInputBean or TagInputBean and implement XMLMappable or DelimitedMappable
 * to massage your data prior to dispatch to FD.
 * <p>
 * Parameters:
 * example command - assumes the server has a Tag model called Countries
 * import --auth.user=demo:123 --fd.client.delimiter=";" --fd.client.import="/fd-cow.txt" --fd.content.model="tag:countries"
 * <p>
 * -or- to parse the file using the local copy of the Content Model,
 * import --auth.user=demo:123 --fd.client.delimiter=";" --fd.client.import="/fd-cow.txt,/countries.json"
 *
 * @author mholdsworth
 * @see FdClientIo
 * @see PayloadTransformer
 * @see TagInputBean
 * @see org.flockdata.track.bean.EntityInputBean
 * @since 13/10/2013
 */
@Configuration
//@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
public class Importer {

    @Value("${auth.user:#{null}}")
    String authUser;
    @Value("${fd.client.delimiter:,}")
    String delimiter;
    @Value("${fd.content.model:#{null}}")
    String serverSideContentModel; // tag:{typeCode} or {fortress}:{doctype}
    private Logger logger = LoggerFactory.getLogger(Importer.class);
    private ClientConfiguration clientConfiguration;
    private FdIoInterface fdClientIo;
    private FileProcessor fileProcessor;

    public Importer() {
    }

    @Autowired
    public Importer(FileProcessor fileProcessor, FdIoInterface fdClientIo, ClientConfiguration clientConfiguration) {
        this.fileProcessor = fileProcessor;
        this.fdClientIo = fdClientIo;
        this.clientConfiguration = clientConfiguration;
    }

    @Autowired
    void setFileProcessor(FileProcessor fileProcessor) {
        this.fileProcessor = fileProcessor;
    }

    @Autowired
    void setClientConfiguration(ClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
    }

    @Autowired
    void setFdClientIo(FdIoInterface fdClientIo) {
        this.fdClientIo = fdClientIo;
    }

    public String runImport(Collection<String> files) {
        StopWatch watch = new StopWatch("Batch Import");
        int totalRows = 0;
        try {

            int batchSize = clientConfiguration.getBatchSize();

            int skipCount = clientConfiguration.getSkipCount();

            fdClientIo.validateConnectivity();
            watch.start();

            for (String thisFile : files) {
                List<String> items = Arrays.asList(thisFile.split("\\s*,\\s*"));

                int item = 0;
                String fileName = null;
                String fileModel = null;
                for (String itemArg : items) {
                    if (item == 0) {
                        fileName = itemArg;
                    } else if (item == 1) {
                        fileModel = itemArg;
                    }

                    item++;
                }
                ContentModel contentModel;
                ExtractProfile extractProfile;
                if (fileModel != null) {
                    // Reading model from file
                    contentModel = resolveContentModel(fileModel);
                    extractProfile = resolveExtractProfile(fileModel, contentModel);
                } else if (serverSideContentModel != null) {
                    contentModel = resolveContentModel(serverSideContentModel);
                    if (contentModel == null) {
                        logger.error(String.format("Unable to located the requested content model %s", serverSideContentModel));
                        System.exit(-1);
                    }

                    extractProfile = new ExtractProfileHandler(contentModel, delimiter);

                } else {
                    return ("No import parameters to work with");
                }

                SystemUserResultBean su = fdClientIo.me(); // Use the configured API as the default FU unless another is set
                if (su == null) {
                    if (!clientConfiguration.isAmqp()) {
                        throw new FlockException("Unable to connect to FlockData. Is the service running at [" + clientConfiguration.getServiceUrl() + "]?");
                    } else {
                        logger.warn("Http communications with FlockData are not working. Is the service running at [" + clientConfiguration.getServiceUrl() + "]?");
                    }
                }
                logger.debug("*** Calculated process args {}, {}, {}, {}", fileName, contentModel, batchSize, skipCount);
                logger.info("Processing [{}], model [{}]", fileName, fileModel);

                // ToDo: Figure out importProfile properties. We are currently reading them out of the content model
                if (extractProfile == null) {
                    ExtractProfileDeserializer.getImportProfile(fileModel, contentModel);
                }

                totalRows = totalRows + fileProcessor.processFile(extractProfile, fileName);
            }
            return String.format("Finished at {%s}", DateFormat.getDateTimeInstance().format(new Date()));


        } catch (Exception e) {
            return ("Import error " + e.getMessage());
//            System.exit(-1);
        }
    }

    private ExtractProfile resolveExtractProfile(String fileModel, ContentModel contentModel) {
        return fdClientIo.getExtractProfile(fileModel, contentModel);
    }

    // import --auth.user=mike:123 --fd.client.import="/fd-cow.txt" --fd.content.model=tag:countries
    public ContentModel resolveContentModel(String fileModel) throws IOException {
        return fdClientIo.getContentModel(fileModel);
    }


}