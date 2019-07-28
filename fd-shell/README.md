### Shell

`docker run -it flockdata/fd-shell`
- or -
`java -jar fd-shell-[version].jar`

 * help         - Confuses you
 * ping         - pong
 * cd           - report the working director
 * login        - Authenticate with the service
 * health       - Validate health checks
 * set          - Reconfigure, the FD environment 
 * env          - Display the current FD environment
 * import       - Push data into the FD service
 * register     - allows the `--login {name}` to write data to the service
  
## login
Connect the shell with a different set of credentials
`login --user demo --pass 123`

## register
FlockData allows you to allow users in your authentication domain to access data. This is happens when you connect a login account with FlockData as a "System User" account. System Users have access to data curated in FlockData. Docker-Compose defines where the `fd-engine` api is located where running with `docker` means you have to tell it where to find `fd-engine` 

To register a data access account you need to both login and specify the data access account you want to create. Run the following:

`register --login demo`

## env
Dump the currently configured environment settings

Configured settings can always be overridden on the command line or set as system environment variables

## health
Report on connectivity of the service chain

## import
See the world! This customized version of `fdimport` loads [countries](http://opengeocode.org/), capital cities, and common aliases along with geo tagged data, into FlockData. States for USA, Canada, and Australia are also added.

From the shell you can ingest delimited data by supplying its content model profile `profile.json` 
 
`ingest --data "data/fd-cow.txt, model/countries.json;data/states.csv, model/states.json"`

