Overview
========
fd-client is an Apache licensed client side library used to talk to FlockData. It contains classes that can be injected into your Java code and commands that can be executed from the FD shell
    



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