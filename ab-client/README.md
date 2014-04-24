Overview
========
This application is a client side batch processing tool that communicates with the ab-engine over restful endpoints. Talking over http is generally slow so this supports batching the input to gain transmission and server side efficiencies. 

ab-client can process Tags and Track information. If your Track data has tags associated with it, then the Tags will be created first. This is to reduce the likelihood of deadlocks during the multi-threaded processing that happens when processing Track events.

parameter  _description_ __example__
- _-b_	batch size to process __-b=100__
- _-s_	server address __-s=http://localhost:8081/ab-engine__
- _"{file},{handler},{skipcount}"_ __the file to import and optional handler
to process file content__

The last parameter is a repeating block will be processed sequentially. Skip count can be applied to each file. Setting skipCount of -1 will force the client to simulate and not call ab-engine.  
Otherwise processing will begin at skipcount + 1 	

JSON Tag Input
If you have a JSON file in the Tag Input syntax, then you can simply pass this file with a command similar to this:
````
java -jar com.auditbucket.client.Importer --b=100 -s=http://localhost:8081/ab-engine "./src/main/resources/myTags.json"
````

Saving the text below to myTags.json and importing it with the previous command will produce a mindmap style graph that shows the conceptual structure of a Microservice. 
````
[
    {"name": "Microservice",  "index":"MS", "targets": {"provides": [{"name": "API", "index":"MS"}]}},
    {"name": "Microservice",  "index":"MS", "targets": {"independently": [{"name": "Deployable", "index":"MS"}]}},
    {"name": "Deployable",  "index":"MS", "targets": {"release": [{"name": "Automated Deployment", "index":"MS", "reverse":true}]}},
    {"name": "Deployable",  "index":"MS", "targets": {"verify": [{"name": "Automated Tests", "index":"MS", "reverse":true}]}},
    {"name": "Microservice",  "index":"MS", "targets": {"aligns": [{"name": "Business Capability", "index":"MS"}]}},
    {"name": "Business Capability",  "index":"MS", "targets": {"describes": [{"name": "Bounded Context", "index":"MS"}]}},
    {"name": "Microservice",  "index":"MS", "targets": {"integrates":[ {"name": "Light Weight Messaging", "index":"MS"}]}},
    {"name": "Microservice",  "index":"MS", "targets": {"honours": [{"name": "Contracts", "index":"MS"}]}},
    {"name": "Microservice",  "index":"MS", "targets": {"consists": [{"name": "Components", "index":"MS"}]}},
    {"name": "Light Weight Messaging",  "index":"MS", "targets": {"provides": [{"name": "RabbitMQ", "index":"MS", "reverse":"true"},{"name": "ZeroMQ", "index":"MS", "reverse":"true"}]}},
	{"name": "Contracts",  "index":"MS", "targets": {"example": [{"name": "Tolerant Reader", "index":"MS", "reverse":true},{"name": "Consumer Driven Contracts", "index":"MS", "reverse":true}]}},
	{"name": "Patterns",  "index":"MS", "targets": {"example": [{"name": "Tolerant Reader", "index":"MS"},{"name": "Consumer Driven Contracts", "index":"MS"}]}},
	{"name": "Components",  "index":"MS", "targets": {"use": [{"name": "Libraries", "index":"MS"}]}},	
	{"name": "Components",  "index":"MS", "targets": {"expose": [{"name": "API", "index":"MS"}]}},	
	{"name": "Libraries",  "index":"MS", "targets": {"persist": [{"name": "Database", "index":"MS"}]}},
	{"name": "Libraries",  "index":"MS", "targets": {"developed": [{"name": "Language", "index":"MS"}]}},		
	{"name": "Spring",  "index":"MS", "targets": {"framework": [{"name": "Libraries", "index":"MS"}]}},		
	{"name": "Rails",  "index":"MS", "targets": {"framework": [{"name": "Libraries", "index":"MS"}]}},		
	{"name": "Components",  "index":"MS", "targets": {"developed": [{"name": "Language", "index":"MS"}]}},		
	{"name": "Java",  "index":"MS", "targets": {"example": [{"name": "Language", "index":"MS"}]}},			
	{"name": "C#",  "index":"MS", "targets": {"example": [{"name": "Language", "index":"MS"}]}},			
	{"name": "Scala",  "index":"MS", "targets": {"example": [{"name": "Language", "index":"MS"}]}},			
	{"name": "Python",  "index":"MS", "targets": {"example": [{"name": "Language", "index":"MS"}]}},			
	{"name": "HTTP",  "index":"MS", "targets": {"transport": [{"name": "API", "index":"MS"}]}},
    {"name": "Monitoring", "index":"MS", "targets": {"checks": [{"name": "HTTP", "index":"MS"}]}}		
]
````
When the above has been loaded, you can query it in the Neo4j Query Browser as 

````
match (tag:MS) return tag;
````

