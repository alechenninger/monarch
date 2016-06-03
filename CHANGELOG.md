# Change Log

## [v0.5.3](https://github.com/alechenninger/monarch/tree/v0.5.3) (2016-06-03)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.5.2...v0.5.3)

**Closed issues:**

- When targeting a source lower in hierarchy where change has hash in this source and higher source for a merge key, only lower value is applied [\#41](https://github.com/alechenninger/monarch/issues/41)
- Include more information when YAML parse exception occurs [\#34](https://github.com/alechenninger/monarch/issues/34)
- Default outputDir to dataDir? [\#21](https://github.com/alechenninger/monarch/issues/21)
- Document example usage as well as cli arguments [\#13](https://github.com/alechenninger/monarch/issues/13)

**Merged pull requests:**

- Include filename when parse error occurs; refactor out common parsing code [\#51](https://github.com/alechenninger/monarch/pull/51) ([alechenninger](https://github.com/alechenninger))

## [v0.5.2](https://github.com/alechenninger/monarch/tree/v0.5.2) (2016-05-30)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.5.1...v0.5.2)

**Merged pull requests:**

- Add '--change' as alias for '--changes' [\#50](https://github.com/alechenninger/monarch/pull/50) ([alechenninger](https://github.com/alechenninger))
- Warn when defaulting to 'apply' command [\#49](https://github.com/alechenninger/monarch/pull/49) ([alechenninger](https://github.com/alechenninger))
- Fix unregonized command help [\#48](https://github.com/alechenninger/monarch/pull/48) ([alechenninger](https://github.com/alechenninger))
- Fix help output when arg error with multiple args [\#47](https://github.com/alechenninger/monarch/pull/47) ([alechenninger](https://github.com/alechenninger))

## [v0.5.1](https://github.com/alechenninger/monarch/tree/v0.5.1) (2016-05-30)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.5.0...v0.5.1)

**Merged pull requests:**

- Improve help and error handling [\#46](https://github.com/alechenninger/monarch/pull/46) ([alechenninger](https://github.com/alechenninger))

## [v0.5.0](https://github.com/alechenninger/monarch/tree/v0.5.0) (2016-05-29)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.4.0...v0.5.0)

**Closed issues:**

- Accept changeset as alias of changes [\#39](https://github.com/alechenninger/monarch/issues/39)
- Add command\(s\) to modify an existing changeset [\#37](https://github.com/alechenninger/monarch/issues/37)
- Merge keys on command line accepts comma delimited list, should be space [\#31](https://github.com/alechenninger/monarch/issues/31)

**Merged pull requests:**

- Add commands [\#45](https://github.com/alechenninger/monarch/pull/45) ([alechenninger](https://github.com/alechenninger))
- Handle empty \(null\) 'set' and 'remove' sections [\#35](https://github.com/alechenninger/monarch/pull/35) ([kahowell](https://github.com/kahowell))

## [v0.4.0](https://github.com/alechenninger/monarch/tree/v0.4.0) (2016-01-24)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.3.4...v0.4.0)

**Closed issues:**

- Config data dir parser reads "data" not "dataDir" [\#30](https://github.com/alechenninger/monarch/issues/30)
- Allow declaration of multiple 'root' hierarchies in config file [\#26](https://github.com/alechenninger/monarch/issues/26)
- Having a changeset that ends with '---' causes a null pointer exception [\#25](https://github.com/alechenninger/monarch/issues/25)
- Missing warning when no changes are provided [\#24](https://github.com/alechenninger/monarch/issues/24)
- Rename "pivot source" -\> "target" [\#23](https://github.com/alechenninger/monarch/issues/23)
- Allow multiple config files [\#22](https://github.com/alechenninger/monarch/issues/22)

**Merged pull requests:**

- Format the file if no changes are provided [\#29](https://github.com/alechenninger/monarch/pull/29) ([kahowell](https://github.com/kahowell))
- Change "pivotSource" to "target" \(fixes \#23\) [\#28](https://github.com/alechenninger/monarch/pull/28) ([kahowell](https://github.com/kahowell))

## [v0.3.4](https://github.com/alechenninger/monarch/tree/v0.3.4) (2015-12-18)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.3.3...v0.3.4)

**Closed issues:**

- Config file expects values to be strings; does not like arbitrary yaml [\#20](https://github.com/alechenninger/monarch/issues/20)
- Use more terse config keys in config yaml [\#19](https://github.com/alechenninger/monarch/issues/19)

## [v0.3.3](https://github.com/alechenninger/monarch/tree/v0.3.3) (2015-12-17)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.3.2...v0.3.3)

**Closed issues:**

- Flattened view of data goes in reverse order [\#17](https://github.com/alechenninger/monarch/issues/17)
- If key/value in a change is already present in data, but only in the pivot source, it is removed [\#16](https://github.com/alechenninger/monarch/issues/16)
- Reverse engineer changeset from exiting hiera configs? [\#14](https://github.com/alechenninger/monarch/issues/14)

## [v0.3.2](https://github.com/alechenninger/monarch/tree/v0.3.2) (2015-12-09)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.2.1...v0.3.2)

**Closed issues:**

- Sort output yaml [\#11](https://github.com/alechenninger/monarch/issues/11)
- Only write out pivot source and below [\#10](https://github.com/alechenninger/monarch/issues/10)

**Merged pull requests:**

- Sort output yaml; only output what's being changed [\#12](https://github.com/alechenninger/monarch/pull/12) ([alechenninger](https://github.com/alechenninger))

## [v0.2.1](https://github.com/alechenninger/monarch/tree/v0.2.1) (2015-12-08)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.2.0...v0.2.1)

**Closed issues:**

- Merged value is not optimized if it has redundant keys that would otherwise be inherited [\#8](https://github.com/alechenninger/monarch/issues/8)

**Merged pull requests:**

- Remove inherited keys in merged value \(fixes \#8\) [\#7](https://github.com/alechenninger/monarch/pull/7) ([alechenninger](https://github.com/alechenninger))

## [v0.2.0](https://github.com/alechenninger/monarch/tree/v0.2.0) (2015-12-08)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.1.0...v0.2.0)

**Closed issues:**

- Allow merging hashes [\#4](https://github.com/alechenninger/monarch/issues/4)

**Merged pull requests:**

- Add support for keys whose values are merged with ancestry \(fixes \#4\) [\#5](https://github.com/alechenninger/monarch/pull/5) ([alechenninger](https://github.com/alechenninger))

## [v0.1.0](https://github.com/alechenninger/monarch/tree/v0.1.0) (2015-11-29)
**Closed issues:**

- CLI help output is not very readable, examples are incorrectly formatted [\#1](https://github.com/alechenninger/monarch/issues/1)



\* *This Change Log was automatically generated by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*