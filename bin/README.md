
```
usage: monarch [-?] [-v] [--verbose | --quiet] {apply,set} ...

A tool to manage hierarchical data.

optional arguments:
  -?, --help             Show this message and exit.
  -v, --version          Show the running version of monarch and exit.

commands:
  {apply,set}            Pass --help to a command for more information.
    apply                Applies changes to a target data source and its descendants.
    set                  Add or remove key value pairs to set within a change.

logging flags:
  Control log levels. Without either  flag,  some  logs  are  written.  Errors and warnings are
  output to stderr.

  --verbose              Log everything. Useful to understand what your changeset is doing.
  --quiet                Log nothing.

https://github.com/alechenninger/monarch
```

```
usage: monarch apply [-?] --changes CHANGES --target TARGET [TARGET ...]
               [--configs CONFIG [CONFIG ...]] [--hierarchy HIERARCHY] [--data-dir DATA_DIR]
               [--output-dir OUTPUT_DIR] [--merge-keys MERGE_KEY [MERGE_KEY ...]]
               [--yaml-isolate {always,never}]

Applies changes to a target data source and its descendants.

optional arguments:
  -?, --help             Show this message and exit.
  --changes CHANGES, --changeset CHANGES, --change CHANGES, -c CHANGES
                         Path to a yaml file  describing  the  desired  end-state changes for a
                         particular data source. The yaml file  may contain many documents, and
                         each document is a  change  with  'source',  'set', and 'remove' keys.
                         The 'set' key is a map of  key  value  pairs  to set in a data source.
                         The 'remove' key  is  a  list  of  keys  to  remove.  The 'source' key
                         defines the data source which  you  wish  to receive the modifications
                         described by 'set' and 'remove'.
                         
                         Data sources  can  be  defined  in  one  of  two  ways:  by  paths  or
                         variables. To define by a path  is  simple:  'source' is a string to a
                         data source path relative to  the  data  directory. To define a source
                         by variables, use a map  value  for  'source'  instead, where the keys
                         are variable names and  values  are  their  values. Variables are only
                         supported  by  dynamic   hierarchies.   For   more  information  about
                         hierarchies, see --hierarchy.
                         
                         Example:
                         ---
                         source: teams/myteam.yaml
                         set:
                           myapp::version: 2
                           myapp::favorite_website: http://www.redhat.com
                         ---
                         source:
                           team: myteam
                           environment: stage
                         set:
                           myapp::favorite_website: http://stage.redhat.com
  --target TARGET [TARGET ...], -t TARGET [TARGET ...], --source TARGET [TARGET ...], -s TARGET [TARGET ...]
                         A target is the source  in  the  source  tree  from  where you want to
                         change, including itself and any sources  beneath it in the hierarchy.
                         Redundant keys will be  removed  in  sources  beneath the target (that
                         is, sources which inherit its values).  A  target  may be defined as a
                         single data  source  path,  or  as  a  set  of  key=value  pairs which
                         evaluate to a single source in a dynamic hierarchy. For example:
                         teams/myteam.yaml
                         environment=qa team=ops
  --configs CONFIG [CONFIG ...], --config CONFIG [CONFIG ...]
                         Space delimited paths to  files  which  configures  default values for
                         command line options. By  default,  monarch  will  look for '.monarch'
                         files in the working  directory  and  all  of  its parent directories.
                         Additionally, '~/.monarch/config.yaml' is always checked.
                         
                         Config  values  read  are  'dataDir',  'outputDir',  'hierarchy',  and
                         'dataFormats'.  'dataFormats'  has  sub   values  for  supported  data
                         formats, like 'yaml'. Each  data  format  has  its own options. 'yaml'
                         has 'indent' and 'isolate'.
  --hierarchy HIERARCHY, -h HIERARCHY
                         Path to a yaml file describing  the source hierarchy in paths relative
                         to the data directory  (see  data-dir  option).  If not provided, will
                         look for a value  in  config  files  with key 'hierarchy'. Hierarchies
                         come in two flavors: static  and  dynamic.  Static hierarchies have an
                         explicit, pre-defined tree  structure.  A  source  deeper  in the tree
                         inherits from its lineage, nearest first. In YAML, these look like: 
                         global.yaml:
                           teams/myteam.yaml:
                             - teams/myteam/dev.yaml
                             - teams/myteam/stage.yaml
                             - teams/myteam/prod.yaml
                           teams/otherteam.yaml
                         
                         Dynamic hierarchies are defined with an  ordered list of source nodes,
                         from top  most  to  bottom-most  (ancestors  to  children).  Nodes are
                         relative paths to data sources  which  inherit from their lineage like
                         in static hierarchies,  but  may  use  variables  and  an inventory of
                         possible assignable values for  those  variables.  In YAML, these look
                         like: 
                         sources:
                           - common.yaml
                           - team/%{team}.yaml
                           - environment/%{environment}.yaml
                           - team/%{team}/%{environment}.yaml
                         inventory:
                           team:
                             - teamA
                             - teamB
                           environment:
                             - qa
                             - prod
                         
                         Inventory definitions can  use  brace  expansion  syntax  like that in
                         Bash:
                         inventory:
                           team:
                             - team{A,B}
                         
                         Assignments in an inventory can imply other assignments, like so:
                         inventory:
                           team:
                             - team{A,B}
                           app:
                             store:
                               team: teamA
                         
                         When a variable is  assigned  a  value,  its  implicit assignments are
                         applied simultaneously. Assignments may  rule  out possible values for
                         a  variable  if  those  values   would   imply  conflicts  with  known
                         assignments.
  --data-dir DATA_DIR, -d DATA_DIR
                         Path to  where  existing  data  sources  life.  The  data  for sources
                         describe in the hierarchy is looked  using  the paths in the hierarchy
                         relative to this folder. If  not  provided,  will  look for a value in
                         config files with key 'dataDir'.
  --output-dir OUTPUT_DIR, -o OUTPUT_DIR
                         Path to directory where  result  data  sources  will  be written. Data
                         sources will be written using  relative  paths  from hierarchy. If not
                         provided, will look for a value in config files with key 'outputDir'.
  --merge-keys MERGE_KEY [MERGE_KEY ...], -m MERGE_KEY [MERGE_KEY ...]
                         Space-delimited list of  keys  which  should  be  inherited with merge
                         semantics. That is, normally the value  that  is inherited for a given
                         key is only the nearest ancestor's  value.  Keys that are in the merge
                         key list however  inherit  values  from  all  of  their ancestor's and
                         merge  them  together,  provided  they   are   like  types  of  either
                         collections or maps. If not provided, will  look for an array value in
                         config files with key 'outputDir'.
  --yaml-isolate {always,never}
                         Controls when you want monarch to  possibly avoid destructive edits to
                         existing YAML data sources  with  regards  to format, ordering, and/or
                         comments outside of keys  previously  created  by monarch. Always will
                         cause monarch to abort updating  a  source that would require changing
                         keys not previously managed by  monarch.  Never  will cause monarch to
                         always manage the entire source.
                         
                         Defaults to config  files  (see  --config),  and  if  neither  are set
                         defaults to 'always'.
```

```
usage: monarch set [-?] --changes CHANGES --source SOURCE [SOURCE ...] [--put PUT [PUT ...]]
               [--remove REMOVE [REMOVE ...]] [--hierarchy HIERARCHY]
               [--configs CONFIG [CONFIG ...]]

Add or remove key value pairs to set within a change.

optional arguments:
  -?, --help             Show this message and exit.
  --changes CHANGES, --changeset CHANGES, --change CHANGES, -c CHANGES
                         Path to a file with changes to modify or create.
  --source SOURCE [SOURCE ...], -s SOURCE [SOURCE ...], --target SOURCE [SOURCE ...], -t SOURCE [SOURCE ...]
                         Identifies the change  to  operate  on  by  its  data  source.  May be
                         defined as a single data source path,  or  as a set of key=value pairs
                         which evaluate  to  a  single  source  in  a  dynamic  hierarchy.  For
                         example:
                         teams/myteam.yaml
                         environment=qa team=ops
  --put PUT [PUT ...], -p PUT [PUT ...]
                         Key value pairs to add or  replace  in  the  set block of the source's
                         change.
                         The  list  may  contain   paths   to   yaml   files   or  inline  yaml
                         heterogeneously.
  --remove REMOVE [REMOVE ...], -r REMOVE [REMOVE ...]
                         List of keys to remove from the set block of a change.
                         Applied after 'put' so this may remove keys set by the 'put' argument.
  --hierarchy HIERARCHY, -h HIERARCHY
                         Optional path to  hierarchy.  Only  used  for  sorting  entries in the
                         output changes. For more information  about hierarchies, see: apply --
                         help
  --configs CONFIG [CONFIG ...], --config CONFIG [CONFIG ...]
                         Space delimited paths to  files  which  configures  default values for
                         command line options. By  default,  monarch  will  look for '.monarch'
                         files in the working  directory  and  all  of  its parent directories.
                         Additionally, '~/.monarch/config.yaml' is always checked.
                         
                         The only config value read is  'hierarchy',  which is used to sort the
                         changes from top-most to bottom-most
```

