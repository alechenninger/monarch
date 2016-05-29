```
usage: monarch [-?] [--version] {apply,set} ...

A tool to manage hierarchical data.

optional arguments:
  -?, --help             Show this message and exit.
  --version              Show the running version of monarch and exit.

commands:
  If none chosen, defaults to 'apply'

  {apply,set}            Pass --help to a command for more information.
    apply                Applies a changeset to  a  target  data source and
                         its descendants.
    set                  Add or remove  key  value  pairs  to  set within a
                         change.

https://github.com/alechenninger/monarch
```

```
usage: monarch apply [-?] [--hierarchy HIERARCHY] [--changeset CHANGES]
               [--target TARGET] [--data-dir DATA_DIR]
               [--configs CONFIGS [CONFIGS ...]] [--output-dir OUTPUT_DIR]
               [--merge-keys MERGE_KEYS]

Applies a changeset to a target data source and its descendants.

optional arguments:
  -?, --help             Show this message and exit.
  --hierarchy HIERARCHY, -h HIERARCHY
                         Path  to  a  yaml   file   describing  the  source
                         hierarchy, relative  to  the  data  directory (see
                         data-dir option). For example: 
                         global.yaml:
                           teams/myteam.yaml:
                             teams/myteam/dev.yaml
                             teams/myteam/stage.yaml
                             teams/myteam/prod.yaml
                           teams/otherteam.yaml
  --changeset CHANGES, --changes CHANGES, -c CHANGES
                         Path to a yaml  file  describing  the desired end-
                         state changes. For example: 
                         ---
                           source: teams/myteam.yaml
                           set:
                             myapp::version: 2
                         myapp::favorite_website:        http://www.redhat.
                         com
                         ---
                           source: teams/myteam/stage.yaml
                           set:
                         myapp::favorite_website:      http://stage.redhat.
                         com
  --target TARGET, -t TARGET
                         A target is the  source  in  the  source tree from
                         where you want  to  change,  including  itself and
                         any  sources   beneath   it   in   the  hierarchy.
                         Redundant keys will be  removed in sources beneath
                         the target (that  is,  sources  which  inherit its
                         values). Ex: 'teams/myteam.yaml'
  --data-dir DATA_DIR, -d DATA_DIR
                         Path to  where  existing  data  sources  life. The
                         data for  sources  describe  in  the  hierarchy is
                         looked using the paths  in  the hierarchy relative
                         to this folder.
  --configs CONFIGS [CONFIGS ...], --config CONFIGS [CONFIGS ...]
                         Space delimited paths  to  files  which configures
                         default  values  for  command  line  options.  The
                         default config path  of  ~/.monarch/config.yaml is
                         always checked.
  --output-dir OUTPUT_DIR, -o OUTPUT_DIR
                         Path to directory where  result  data sources will
                         be written. Data  sources  will  be  written using
                         relative paths from hierarchy.
  --merge-keys MERGE_KEYS, -m MERGE_KEYS
                         Space-delimited  list  of  keys  which  should  be
                         inherited with merge semantics.  That is, normally
                         the value that is  inherited  for  a  given key is
                         only the nearest ancestor's  value.  Keys that are
                         in the merge key list  however inherit values from
                         all of their ancestor's  and  merge them together,
                         provided   they   are   like   types   of   either
                         collections or maps.
```

```
usage: monarch set [-?] [--changes CHANGES] [--source SOURCE]
               [--put PUT [PUT ...]] [--remove REMOVE [REMOVE ...]]
               [--hierarchy HIERARCHY] [--configs CONFIGS [CONFIGS ...]]

Add or remove key value pairs to set within a change.

optional arguments:
  -?, --help             Show this message and exit.
  --changes CHANGES, --changeset CHANGES, -c CHANGES
                         Path to a changeset to modify or create.
  --source SOURCE, -s SOURCE
                         Identifies the change to  operate  on  by its data
                         source.
  --put PUT [PUT ...], -p PUT [PUT ...]
                         Key value pairs  to  add  or  replace  in  the set
                         block of the source's change.
                         The list  may  contain  paths  to  yaml  files  or
                         inline yaml heterogeneously.
  --remove REMOVE [REMOVE ...], -r REMOVE [REMOVE ...]
                         List of keys to  remove  from  the  set block of a
                         change.
                         Applied after 'put' so  this  may  remove keys set
                         by the 'put' argument.
  --hierarchy HIERARCHY, -h HIERARCHY
                         Optional path to hierarchy.  Only used for sorting
                         entries in the output changes.
  --configs CONFIGS [CONFIGS ...], --config CONFIGS [CONFIGS ...]
                         Paths to config files  to  use  for the hierarchy.
                         First one with a hierarchy wins.
```

