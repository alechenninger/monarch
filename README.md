# monarch
Rule over hierarchical data!

## install from tarball
1. Download tar or zip @ https://github.com/alechenninger/monarch/releases
2. Extract somewhere you like to put things
3. Have JRE8 installed
4. Have JAVA_HOME defined (for ex: "/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.65-3.b17.fc22.x86_64")
5. Add a symlink 'monarch' to your path which points to ${where_you_extracted_zip}/bin/monarch-bin

## install from source
1. Fork or git clone git@github.com:alechenninger/monarch.git
2. cd monarch
3. Have gradle installed. I recommend using [sdkman](http://sdkman.io/usage.html).
5. Have JRE8 installed
7. Have JAVA_HOME defined (for ex: "/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.65-3.b17.fc22.x86_64")
8. Run gradle installDist
9. Add symlink 'monarch' to your path which points to ${where_you_put_git_things}/monarch/bin/build/install/monarch-bin/bin/monarch-bin
10. Hack or git pull and gradle installDist whenever you want to update your executable

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
inherit defaults from). This tool allows you to start with your end state: all environments get 
the new configuration, and allow to tool to generate the "horizontal" promotions from QA to stage,
as well as the "vertical" promotion from QA and stage to their parent data source.
