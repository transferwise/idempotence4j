# Changelog

All notable changes to this project will be documented in this file, in reverse chronological order by release.

## 1.7.1 - 2022-05-03
bump cron-utils to fix avd.aquasec.com/nvd/cve-2021-41269
bump jackson-datatype to fix https://cve.mitre.org/cgi-bin/cvename.cgi?name=cve-2018-1000873

## 1.7.0 - 2022-04-19
- `deleteOlderThan` method on `ActionRepository` now forces use of a primitive for batch size.
- Begin returning the number of rows deleted from deletion methods in `ActionRepository`
- Exposed a `deleteByTypeAndClient` method on `ActionRepository`
- Deletion of old action IDs is now more performant on MariaDB

## 1.6.0 - 2022-04-08

expose ActionRepository deleteByIds method

## 1.4.0 - 2020-09-25

MariaDb integration

## 1.3.0 - 2020-09-15

Action retention policy

## 1.0.0 - 2020-05-19

Initial release.
