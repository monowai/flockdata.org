Overview
========
fd-client is an Apache licensed client side library used to talk to FlockData. It contains classes that can be injected into your Java code and commands that can be executed from the command line
    
Configuration is via YAML and can be overridden by passing arguments on the command line.

### Commands
Commands are being added on a regular basis and provide a functional wrapper to the REST services. These classes, and how they are used, can be found in the package `org.flockdata.client.commands`

We recommend using the Docker package `flockdata/fd-client`. The commands in this section show example of running in native docker and with the [fd-demo](http://github.com/monowai/fd-demo) stack.

 * Ping         - Can you see the service
 * Login        - Authenticate with the service
 * Health       - Validate health checks
 * TrackEntity  - Write an entity to the service
 * Registration - Register an authorised login as a data-access user 
 * EntityGet    - Retrieve a tracked entity from the service.

`docker-compose run fd-client fdping`
`curl http://localhost:8080/api/ping`
`docker-compose run fd-client fdhealth`

## fdregister
FlockData allows you to allow users in your authentication domain to access data. This is happens when you connect a login account with FlockData as a "System User" account. System Users have access to data curated in FlockData. Docker-Compose defines where the `fd-engine` api is located where running with `docker` means you have to tell it where to find `fd-engine` 

To register a data access account you need to both login and specify the data access account you want to create. Run the following:

`docker-compose run fd-client fdregister -u=demo:123 -l=demo`

If you are just running Docker, then you will need to tell it how to find fd-engine:

`docker run fd-client fdregister -u=demo:123 -l=demo --org.fd.engine.url=http://fd-engine:8080`

These commands do the same thing - register the login account `demo` as a SystemUser so that it can read and write data

## fdimport
Invoke the fdimporter to ingest data as modeled by `profile.json` using supplied arguments
 
`docker-compose run fdimport --fd.client.import="path/data.txt, path/profile.json"`

## fdcountries
See the world! This customized version of `fdimport` loads [countries](http://opengeocode.org/), capital cities, and common aliases along with geo tagged data, into FlockData. States for USA, Canada, and Australia are also added.

`docker-compose run fd-client fdcountries`

### Configuration
All test based configuration is controlled by files in `src/test/resources/`

 * `application.yml`     - General client configuration settings for client side classes
 * `application_dev.yml` - Unit test client configuration settings
 * `fd-batch.properties` - Spring batch client configuration settings

Configured settings can always be overridden on the command line or set as system environment variables


### Java
FlockData has a comprehensive REST based API. This package provides convenience classes to facilitate client side communication. Classes in this package support injection using the SpringFramework. There are three classes that are primarily required when communicating using Java:
 * FdTemplate  - high level data integration with support for batching  
 * FdClientIo  - low level command interface that runs commands against the service
 * ClientConfiguration - Configuration class and access to environmental variables that are used by the previous classes
 
Include in your pom
 
 ```xml
    <dependencies>
         <dependency>
             <groupId>org.flockdata</groupId>
             <artifactId>fd-client</artifactId>
             <version>${fd.version}</version>
         </dependency>
         <dependency>
             <groupId>org.flockdata</groupId>
             <artifactId>fd-client</artifactId>
             <type>test-jar</type>
             <version>${fd.version}</version>
         </dependency>
         ...
    </dependencies>         

```
Sample code
 
```java
class MyService {
    private org.flockdata.integration.Template template;
    private org.flockdata.integration.ClientConfiguration clientConfiguration;
 
    @Autowired
    void setFdTemplate (Template fdTemplate, ClientConfiguration clientConfiguration){
        this.template = fdTemplate;
        // Not strictly required unless you want access to configuration properties in your code
        this.clientConfiguration = clientConfiguration;
     }
 
    void writeData (EntityInputBean bean){
        // To batch the bean for writing
        fdTemplate.writeEntity(bean);
        // Call fdTemplate.flush(); to commit changes to the service
        
        // or, to write immediately
        fdTemplate.writeEntity(bean, true); 
      }
 }

```
#### Java Unit Tests
 For testing purpose you can either inject the associated mocks or use the AbstractImport via inheritance.
 * FdTemplateMock - inject a mock template that stubs out flushing calls 
 * FdMockIo       - stubs IO calls and buffers data that would be written so that you can assert the results
 * AbstractImport - included in fd-client:test-jar. contains pre-wired mocks 

The easiest way to write a test is to inherit from AbstractImport but you can inject the mocks directly. There are plenty of examples of doing this in the fd-client test package, but conceptually it goes like this:

```java
public class TestCounties extends AbstractImport {

    @Test
    public void validate_Counties() throws Exception {
        // JSON configuration file
        String configFile = "/profiles/fip-counties.json";

        // Extract the content model that tells us how to parse the data
        ContentModel contentModel = ContentModelDeserializer.getContentModel(configFile);
        // Process the file - the configFile contains parsing and modelling sections
        long rows = fileProcessor.processFile(new ExtractProfileHandler(contentModel), "/counties.csv");
        int expectedRows = 1;
        assertEquals(expectedRows, rows);

        // fileProcessor will have created tags, let's assert them
        List<TagInputBean> tagInputBeans = fdTemplate.getTags();
        TestCase.assertEquals(expectedRows, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            assertEquals("01-001", tagInputBean.getCode());
            TestCase.assertEquals("US", tagInputBean.getKeyPrefix());
            assertEquals("Autauga County", tagInputBean.getName());
        }
        // Check that the payload will serialize
        assertNotNull(new ObjectMapper().writeValueAsString(tagInputBeans));
    }
}
```