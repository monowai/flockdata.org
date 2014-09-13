Overview
========
This application is a client side batch processing tool that communicates with the ab-engine over restful endpoints. Talking over http is generally slow so this supports batching the input to gain transmission and server side efficiencies. 

ab-client can process Tags and Track information. If your Track data has tags associated with it, then the Tags will be created first. This is to reduce the likelihood of deadlocks during the multi-threaded processing that happens when processing Track events.

_parameter_  description __example__
- _-b_	batch size to process __-b=100__
- _-s_	server address __-s=http://localhost:8081/ab-engine__
- _"{file},{handler},{skipcount}"_ the file to import and optional handler
to process file content __"./cow.txt,com.auditbucket.health.Countries,0"__

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
    {"name": "Microservice",  "label":"MS", "targets": {"provides": [{"name": "API", "label":"MS"}]}},
    {"name": "Microservice",  "label":"MS", "targets": {"independently": [{"name": "Deployable", "label":"MS"}]}},
    {"name": "Deployable",  "label":"MS", "targets": {"release": [{"name": "Automated Deployment", "label":"MS", "reverse":true}]}},
    {"name": "Deployable",  "label":"MS", "targets": {"verify": [{"name": "Automated Tests", "label":"MS", "reverse":true}]}},
    {"name": "Microservice",  "label":"MS", "targets": {"aligns": [{"name": "Business Capability", "label":"MS"}]}},
    {"name": "Business Capability",  "label":"MS", "targets": {"describes": [{"name": "Bounded Context", "label":"MS"}]}},
    {"name": "Microservice",  "label":"MS", "targets": {"integrates":[ {"name": "Light Weight Messaging", "label":"MS"}]}},
    {"name": "Microservice",  "label":"MS", "targets": {"honours": [{"name": "Contracts", "label":"MS"}]}},
    {"name": "Microservice",  "label":"MS", "targets": {"consists": [{"name": "Components", "label":"MS"}]}},
    {"name": "Light Weight Messaging",  "label":"MS", "targets": {"provides": [{"name": "RabbitMQ", "label":"MS", "reverse":"true"},{"name": "ZeroMQ", "label":"MS", "reverse":"true"}]}},
	{"name": "Contracts",  "label":"MS", "targets": {"example": [{"name": "Tolerant Reader", "label":"MS", "reverse":true},{"name": "Consumer Driven Contracts", "label":"MS", "reverse":true}]}},
	{"name": "Patterns",  "label":"MS", "targets": {"example": [{"name": "Tolerant Reader", "label":"MS"},{"name": "Consumer Driven Contracts", "label":"MS"}]}},
	{"name": "Components",  "label":"MS", "targets": {"use": [{"name": "Libraries", "label":"MS"}]}},	
	{"name": "Components",  "label":"MS", "targets": {"expose": [{"name": "API", "label":"MS"}]}},	
	{"name": "Libraries",  "label":"MS", "targets": {"persist": [{"name": "Database", "label":"MS"}]}},
	{"name": "Libraries",  "label":"MS", "targets": {"developed": [{"name": "Language", "label":"MS"}]}},		
	{"name": "Spring",  "label":"MS", "targets": {"framework": [{"name": "Libraries", "label":"MS"}]}},		
	{"name": "Rails",  "label":"MS", "targets": {"framework": [{"name": "Libraries", "label":"MS"}]}},		
	{"name": "Components",  "label":"MS", "targets": {"developed": [{"name": "Language", "label":"MS"}]}},		
	{"name": "Java",  "label":"MS", "targets": {"example": [{"name": "Language", "label":"MS"}]}},			
	{"name": "C#",  "label":"MS", "targets": {"example": [{"name": "Language", "label":"MS"}]}},			
	{"name": "Scala",  "label":"MS", "targets": {"example": [{"name": "Language", "label":"MS"}]}},			
	{"name": "Python",  "label":"MS", "targets": {"example": [{"name": "Language", "label":"MS"}]}},			
	{"name": "HTTP",  "label":"MS", "targets": {"transport": [{"name": "API", "label":"MS"}]}},
    {"name": "Monitoring", "label":"MS", "targets": {"checks": [{"name": "HTTP", "label":"MS"}]}}		
]
````
When the above has been loaded, you can query it in the Neo4j Query Browser as 

````
match (tag:MS) return tag;
````
Which will produce this visualization
![Alt text](./ms-neo.png?raw=true "Microservice mind-map")
