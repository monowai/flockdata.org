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
import java.util.Collection;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.flockdata.helper.FlockException;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.FileProcessor;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.FdIoInterface;
import org.flockdata.transform.model.ExtractProfile;
import org.flockdata.transform.model.PayloadTransformer;
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
@Slf4j
public class Importer {

  @Value("${auth.user:#{null}}")
  String authUser;
  @Value("${fd.client.delimiter:,}")
  String delimiter;
  @Value("${fd.content.model:#{null}}")
  String serverSideContentModel;
  private ClientConfiguration clientConfiguration;
  private FdIoInterface fdClientIo;
  private FileProcessor fileProcessor;

  public Importer() {
  }

  @Autowired
  public Importer(FileProcessor fileProcessor,
                  FdIoInterface fdClientIo,
                  ClientConfiguration clientConfiguration) {
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
      verifyConnectivity();

      watch.start();

      for (String thisFile : files) {
        String[] items = thisFile.split("\\s*,\\s*");

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

        ExtractProfile extractProfile;
        if (fileModel != null) {
          extractProfile = resolveExtractProfile(fileModel);
        } else {
          return ("No import parameters to work with");
        }

        // Use the configured API as the default FU unless another is set

        log.debug("*** Calculated process args {}, {}, {}, {}", fileName, extractProfile.getContentModel(),
            clientConfiguration.batchSize(), clientConfiguration.skipCount());
        log.info("Processing [{}], model [{}]", fileName, fileModel);

        totalRows = totalRows + fileProcessor.processFile(extractProfile, fileName);
      }
      return String.format("Finished at {%s}",
          DateFormat.getDateTimeInstance().format(new Date()));


    } catch (Exception e) {
      return ("Import error " + e.getMessage());
    }
  }

  public void verifyConnectivity() throws FlockException {
    SystemUserResultBean su = fdClientIo.me();
    if (su == null) {
      if (!clientConfiguration.amqp()) {
        throw new FlockException("Unable to connect to FlockData. Is the " +
            "service running at [" + clientConfiguration.getServiceUrl() + "]?");
      } else {
        log.warn("Http communications with FlockData are not working. Is the " +
            "service running at [" + clientConfiguration.getServiceUrl() + "]?");
      }
    }
    fdClientIo.validateConnectivity();
  }

  // import --auth.user=mike:123 --fd.client.import="/fd-cow.txt" --fd.content.model=tag:countries
  private ExtractProfile resolveExtractProfile(String fileModel) throws IOException {
    return fdClientIo.getExtractProfile(fileModel, fdClientIo.getContentModel(fileModel));
  }


}