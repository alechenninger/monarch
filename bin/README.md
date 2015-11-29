```
usage: monarch --hierarchy hierarchy.yaml --changes changes.yaml --pivot
               teams/myteam.yaml --data-dir ~/hieradata --output-dir ./
 -?,--help                Show this text.
 -c,--changes <path>      Path to a yaml file describing the desired end-state
                          changes. For example:
                          ---
                            source: teams/myteam.yaml
                            set:
                              myapp::version: 2
                              myapp::favorite_website: http://www.redhat.com
                          ---
                            source: teams/myteam/stage.yaml
                            set:
                              myapp::favorite_website: http://stage.redhat.com
    --config <path>       Path to file which configures default values for
                          command line options.
 -d,--data-dir <path>     Path to where existing data sources life. The data for
                          sources describe in the hierarchy is looked using the
                          paths in the hierarchy relative to this folder.
 -h,--hierarchy <path>    Path to a yaml file describing the source hierarchy,
                          relative to the data directory (see data-dir option).
                          For example:
                          global.yaml:
                            teams/myteam.yaml:
                              teams/myteam/dev.yaml
                              teams/myteam/stage.yaml
                              teams/myteam/prod.yaml
                            teams/otherteam.yaml
 -o,--output-dir <path>   Path to directory where result data sources will be
                          written. Data sources will be written using relative
                          paths from hierarchy.
 -p,--pivot <source>      A pivot source is the source in the source tree from
                          where you want to change, including itself and any
                          sources beneath it in the hierarchy. Redundant keys
                          will be removed in sources beneath the pivot (that is,
                          sources which inherit its values). Ex:
                          'teams/myteam.yaml'
https://github.com/alechenninger/monarch
```
