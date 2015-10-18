# monarch
Rule over hierarchical data!

The idea is something that can take a desired end state, a hierarchy of data sources, a specific
data source to change among them, and the current state of all data sources in the hierarchy, and
split out a new state of all data sources in the hierarchy. In other words, it automates promotion
of values throughout the hierarchy: vertically and horizontally.

A practical usage example is Hiera, used by Puppet. You organize configuration in a hierarchy of
data sources. During releases, you might promote values from one environment's configuration to
another. It can be tedious to copy these values among horizontal promotions, which do not inherit
each other (say, a QA environment to a staging environment), as well as, finally, vertical
promotions (say from QA and stage environments to a base configuration file which all environments
inherit defaults from).

See [the tests](https://github.com/alechenninger/monarch/blob/master/test/MonarchTest.groovy) for
example usage.
