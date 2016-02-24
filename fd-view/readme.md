fd-view is the visualization, search and comparison layer to the FlockData API.

## Dependencies :
* NodeJS

Once node is installed, you should install the following

* Grunt
* Grunt-CLI
* Bower

On Ubuntu, this is all achieved in the following manner
```
sudo apt-get install python-software-properties
sudo apt-add-repository ppa:chris-lea/node.js -y
sudo apt-get update
sudo apt-get install nodejs -q -y

sudo npm install -g grunt
sudo npm install -g grunt-cli
sudo npm install -g bower
```
The -g flag indicates that the package should be installed globally (for all users)

## Building :
These commands install node dependencies which are defined in package.json and should be run in the fd-view sub-folder
```
npm install 
bower install 
```

## Running

Testing :
```
grunt test
```

To build a production version
```
grunt
```

the bundle will be available in dist folder - **!!this is not currently working!!**

Run the server with
```
grunt serve
```
By default you will find fd-view listening on 9000. 

```
http://localhost:9000
```
Click on settings, and change the fd-engine URL to be 
```
http://localhost:8080/api
```
