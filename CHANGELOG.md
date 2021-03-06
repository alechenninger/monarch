# Change Log

## [v0.8.5](https://github.com/alechenninger/monarch/tree/v0.8.5) (2017-04-25)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.8.4...v0.8.5)

**Closed issues:**

- Lineage assignments should be relative to node in dynamic hierarchy [\#92](https://github.com/alechenninger/monarch/issues/92)
- Make apply target optional [\#91](https://github.com/alechenninger/monarch/issues/91)

**Merged pull requests:**

- Fixes \#91: Allow omitting target as if to say, 'apply my change as is' [\#96](https://github.com/alechenninger/monarch/pull/96) ([alechenninger](https://github.com/alechenninger))
- Avoid changing existing whitespace outside of managed yaml blocks [\#95](https://github.com/alechenninger/monarch/pull/95) ([alechenninger](https://github.com/alechenninger))
- Cache some assignments... cache everything! [\#94](https://github.com/alechenninger/monarch/pull/94) ([alechenninger](https://github.com/alechenninger))
- Fixes \#92: Make dynamic hierarchy source lineage and descendants relative [\#93](https://github.com/alechenninger/monarch/pull/93) ([alechenninger](https://github.com/alechenninger))

## [v0.8.4](https://github.com/alechenninger/monarch/tree/v0.8.4) (2017-01-29)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.8.3...v0.8.4)

**Closed issues:**

- Support brace expansion inside a change's target source [\#87](https://github.com/alechenninger/monarch/issues/87)

**Merged pull requests:**

- Closes \#87: Support brace expansion in change source [\#89](https://github.com/alechenninger/monarch/pull/89) ([alechenninger](https://github.com/alechenninger))
- TURBO MODE! Cache some hashCodes and renderings [\#88](https://github.com/alechenninger/monarch/pull/88) ([alechenninger](https://github.com/alechenninger))

## [v0.8.3](https://github.com/alechenninger/monarch/tree/v0.8.3) (2017-01-28)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.8.2...v0.8.3)

**Closed issues:**

- Wrong help message for commands [\#85](https://github.com/alechenninger/monarch/issues/85)
- Support inventory assignment of single value without list syntax [\#84](https://github.com/alechenninger/monarch/issues/84)
- Support ranges in change source syntax [\#80](https://github.com/alechenninger/monarch/issues/80)
- NPE when potentials for variable has nulls [\#66](https://github.com/alechenninger/monarch/issues/66)

**Merged pull requests:**

- Fixes \#85: Remove incorrect description [\#86](https://github.com/alechenninger/monarch/pull/86) ([alechenninger](https://github.com/alechenninger))
- Support brace expansion in inventory definitions [\#83](https://github.com/alechenninger/monarch/pull/83) ([alechenninger](https://github.com/alechenninger))

## [v0.8.2](https://github.com/alechenninger/monarch/tree/v0.8.2) (2017-01-25)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.8.1...v0.8.2)

**Closed issues:**

- Data source output no longer sorted \(regression\) [\#79](https://github.com/alechenninger/monarch/issues/79)

**Merged pull requests:**

- Fixes \#79: Sort output yaml when isolate is 'never' [\#81](https://github.com/alechenninger/monarch/pull/81) ([alechenninger](https://github.com/alechenninger))

## [v0.8.1](https://github.com/alechenninger/monarch/tree/v0.8.1) (2017-01-23)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.8.0...v0.8.1)

**Closed issues:**

- Also look in current directory for config file [\#60](https://github.com/alechenninger/monarch/issues/60)
- Should validate changeset 'source' is in config hierarchy [\#27](https://github.com/alechenninger/monarch/issues/27)

**Merged pull requests:**

- Fixes \#27: Validate changeset; cache individual assignments [\#78](https://github.com/alechenninger/monarch/pull/78) ([alechenninger](https://github.com/alechenninger))
- Handle errors better when looking for config files [\#77](https://github.com/alechenninger/monarch/pull/77) ([alechenninger](https://github.com/alechenninger))
- Use same options to refer to source/target for both apply and set [\#76](https://github.com/alechenninger/monarch/pull/76) ([alechenninger](https://github.com/alechenninger))
- Fixes \#60: Look for .monarch files in working directories and parents [\#75](https://github.com/alechenninger/monarch/pull/75) ([alechenninger](https://github.com/alechenninger))

## [v0.8.0](https://github.com/alechenninger/monarch/tree/v0.8.0) (2017-01-15)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.7.1...v0.8.0)

**Closed issues:**

- Add verbose flag to explain behavior [\#73](https://github.com/alechenninger/monarch/issues/73)
- Allow one variable definition to define others, implicitly [\#67](https://github.com/alechenninger/monarch/issues/67)
- Errors output to stdout instead of stderr [\#61](https://github.com/alechenninger/monarch/issues/61)

**Merged pull requests:**

- Fixes \#73: Add verbose flag [\#74](https://github.com/alechenninger/monarch/pull/74) ([alechenninger](https://github.com/alechenninger))
- Implied variables \(fixes \#67\) [\#72](https://github.com/alechenninger/monarch/pull/72) ([alechenninger](https://github.com/alechenninger))

## [v0.7.1](https://github.com/alechenninger/monarch/tree/v0.7.1) (2016-10-08)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.7.0...v0.7.1)

**Closed issues:**

- monarch set does not rewrite remove values correctly [\#71](https://github.com/alechenninger/monarch/issues/71)

## [v0.7.0](https://github.com/alechenninger/monarch/tree/v0.7.0) (2016-10-08)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.6.0...v0.7.0)

**Closed issues:**

- Allow configuring yaml dump behavior [\#68](https://github.com/alechenninger/monarch/issues/68)
- NPE when no potentials found for a variable [\#65](https://github.com/alechenninger/monarch/issues/65)
- Add ability to restrict modifications to an isolated portion of a yaml file [\#42](https://github.com/alechenninger/monarch/issues/42)

**Merged pull requests:**

- Allow configuring Yaml parser/dumper [\#70](https://github.com/alechenninger/monarch/pull/70) ([alechenninger](https://github.com/alechenninger))
- Isolated updates [\#64](https://github.com/alechenninger/monarch/pull/64) ([alechenninger](https://github.com/alechenninger))

## [v0.6.0](https://github.com/alechenninger/monarch/tree/v0.6.0) (2016-08-07)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.5.6...v0.6.0)

**Closed issues:**

- Support hierarchy defined with variables instead of static path tree [\#53](https://github.com/alechenninger/monarch/issues/53)

**Merged pull requests:**

- Add support for dynamic hierarchy \(fixes \#53\) [\#57](https://github.com/alechenninger/monarch/pull/57) ([alechenninger](https://github.com/alechenninger))

## [v0.5.6](https://github.com/alechenninger/monarch/tree/v0.5.6) (2016-07-14)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.5.5...v0.5.6)

**Closed issues:**

- monarch set --put "" results in NPE [\#62](https://github.com/alechenninger/monarch/issues/62)

## [v0.5.5](https://github.com/alechenninger/monarch/tree/v0.5.5) (2016-07-14)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.5.4...v0.5.5)

**Closed issues:**

- Create change when setting value if change does not yet exist [\#58](https://github.com/alechenninger/monarch/issues/58)
- Regression: doesn't like missing data sources in hierarchy any more [\#56](https://github.com/alechenninger/monarch/issues/56)
- Allow providing multiple targets [\#38](https://github.com/alechenninger/monarch/issues/38)

## [v0.5.4](https://github.com/alechenninger/monarch/tree/v0.5.4) (2016-06-03)
[Full Changelog](https://github.com/alechenninger/monarch/compare/v0.5.3...v0.5.4)

**Merged pull requests:**

- Correctly parse non file paths [\#52](https://github.com/alechenninger/monarch/pull/52) ([alechenninger](https://github.com/alechenninger))

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