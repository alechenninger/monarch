# monarch
Rule over hierarchical data!

## example
```
$ monarch set --changes petstore.yaml --source global.yaml --put 'petstore::version: "2"'
$ monarch apply --changes petstore.yaml --target env=dev
$ cat hieradata/env/dev.yaml 
petstore::version: '2'
$ cat hieradata/global.yaml 
petstore::version: '1'
$ monarch apply --changes petstore.yaml --target global.yaml
$ cat hieradata/env/dev.yaml 
$ cat hieradata/global.yaml 
petstore::version: '2'
```

## screencast

<a href="https://asciinema.org/a/47206?speed=2&autoplay=1" target="_blank"><img src="https://asciinema.org/a/47206.png" width="589"/></a>

## install from tarball
1. Download tar or zip @ https://github.com/alechenninger/monarch/releases/latest
2. Extract somewhere you like to put things
3. Have JRE8 installed
4. Have `java` in your PATH or JAVA_HOME environment variable defined (for ex: "/etc/alternatives/java_sdk")
5. Add a symlink 'monarch' to your PATH which points to ${where_you_extracted_zip}/bin/monarch-bin

## install from source
1. Fork or git clone git@github.com:alechenninger/monarch.git
2. cd monarch
3. Have gradle installed. I recommend using [sdkman](http://sdkman.io/usage.html).
4. Have JRE8 installed
5. Have `java` in your PATH or JAVA_HOME defined (for ex: "/etc/alternatives/java_sdk")
6. Run gradle installDist
7. Add symlink 'monarch' to your path which points to ${where_you_put_git_things}/monarch/bin/build/install/monarch-bin/bin/monarch-bin
8. Hack or git pull and gradle installDist whenever you want to update your executable

## usage
See [bin](https://github.com/alechenninger/monarch/blob/master/bin/) for command line usage.

See [the tests](https://github.com/alechenninger/monarch/blob/master/lib/test/MonarchTest.groovy) for
example library usage and edge cases.

## motivation
The idea is something that can take a desired end state, a hierarchy of data sources, a "target"
data source to change among them, the current state of all data sources in the hierarchy, and
split out a new state of all data sources in the hierarchy with the given changes applied to the
target data source and its children. In other words, it automates promoting values throughout the
hierarchy, only affecting the data sources in the hierarchy you want, but allowing you to specify
changes only as changes to root nodes that you eventually want applied but can't jump to right 
away.

A practical usage example is Hiera, used by Puppet. You organize configuration in a hierarchy of
data sources. During releases, you might promote values from one environment's configuration to
another. It can be tedious to copy these values among horizontal promotions, which do not inherit
each other (say, a QA environment to a staging environment), as well as, finally, vertical
promotions (say from QA and stage environments to a base configuration file which all environments
inherit defaults from). This tool allows you to start with your end state, but be able to generate
intermediate stages as you promote your release from one environment to another. Finally, when you
are ready to generate your final state, redundant keys are removed.
